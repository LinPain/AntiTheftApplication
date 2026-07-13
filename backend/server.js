const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');
require('dotenv').config();

const locationRoutes = require('./routes/locationRoutes');
const authRoutes = require('./routes/authRoutes');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'your_super_secret_key';

app.use(cors());
app.use(express.json());
app.use(express.static('public'));

// Share socket.io instance with routes
app.set('io', io);

// JWT Middleware to protect routes
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) return res.status(401).json({ error: 'Access denied. No token provided.' });

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) return res.status(403).json({ error: 'Invalid or expired token.' });

        const requestedUsername = req.params.username || req.username;

        // Detailed logging for debugging 403
        if (requestedUsername && requestedUsername !== user.username) {
            console.error(`[AUTH-DENIED] Mismatch: URL=${requestedUsername}, Token=${user.username}`);
            return res.status(403).json({
                error: 'Access denied. You can only access your own data.',
                debug: { requested: requestedUsername, actual: user.username }
            });
        }

        req.user = user;
        next();
    });
};

// Routes - REGISTER AUTH FIRST to avoid wildcard conflicts with :username
app.use('/api/auth', authRoutes);

// Protected Namespaced Routes
app.use('/api/:username/location', (req, res, next) => {
    req.username = req.params.username;
    authenticateToken(req, res, next);
}, locationRoutes);

// Root route
app.get('/', (req, res) => {
    res.send('Anti-Theft System GPS Tracking Backend is running');
});

// User-specific states (In a real app, these would be in MongoDB)
let userStates = {};

function getUserState(username) {
    if (!userStates[username]) {
        userStates[username] = {
            alarm: { active: false, timestamp: 0 },
            lockdown: { active: false, timestamp: 0 },
            trackRequest: { active: false, timestamp: 0 }
        };
    }
    return userStates[username];
}

// Protected Status Routes
app.get('/api/:username/status', authenticateToken, (req, res) => {
    const { username } = req.params;
    res.json(getUserState(username));
});

app.post('/api/:username/lockdown', authenticateToken, (req, res) => {
    const { username } = req.params;
    const { active } = req.body;
    const state = getUserState(username);
    state.lockdown = { active: !!active, timestamp: Date.now() };
    io.to(username).emit('statusUpdate', { lockdown: state.lockdown });
    res.json({ message: `Lockdown ${active ? 'activated' : 'deactivated'} for ${username}`, state: state.lockdown });
});

app.post('/api/:username/track', authenticateToken, (req, res) => {
    const { username } = req.params;
    const state = getUserState(username);
    state.trackRequest = { active: true, timestamp: Date.now() };
    io.to(username).emit('trackRequested', { timestamp: state.trackRequest.timestamp });
    res.json({ message: `Track request sent to ${username}`, state: state.trackRequest });
});

app.get('/api/:username/alarm', authenticateToken, (req, res) => {
    const { username } = req.params;
    res.json(getUserState(username).alarm);
});

app.post('/api/:username/alarm', authenticateToken, (req, res) => {
    const { username } = req.params;
    const { active } = req.body;
    const state = getUserState(username);
    state.alarm = { active: !!active, timestamp: Date.now() };
    io.to(username).emit('statusUpdate', { alarm: state.alarm });
    res.json({ message: `Alarm ${active ? 'activated' : 'deactivated'} for ${username}`, state: state.alarm });
});

// Socket.io Connection Logic with Auth
io.use((socket, next) => {
    const token = socket.handshake.auth.token;
    if (!token) return next(new Error("Authentication error"));

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) return next(new Error("Authentication error"));
        socket.user = user;
        next();
    });
});

io.on('connection', (socket) => {
    console.log('New client connected:', socket.id, 'User:', socket.user.username);

    socket.on('join', (username) => {
        if (username === socket.user.username) {
            socket.join(username);
            console.log(`Socket ${socket.id} joined room: ${username}`);
        } else {
            console.warn(`Socket ${socket.id} tried to join unauthorized room: ${username}`);
        }
    });

    socket.on('disconnect', () => {
        console.log('Client disconnected:', socket.id);
    });
});

// Connect to MongoDB
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/zerotrustauth';

mongoose.connect(MONGODB_URI)
    .then(() => {
        console.log('Connected to MongoDB');
        server.listen(PORT, () => {
            console.log(`Server is running on port ${PORT}`);
        });
    })
    .catch(err => {
        console.error('MongoDB connection error:', err);
    });
