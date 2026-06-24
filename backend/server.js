const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();

const locationRoutes = require('./routes/locationRoutes');
const authRoutes = require('./routes/authRoutes');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.use(express.static('public'));

// Routes
app.use('/api/location', locationRoutes);
app.use('/api/auth', authRoutes);

// Root route
app.get('/', (req, res) => {
    res.send('ZeroTrustAuth GPS Tracking Backend is running');
});

let alarmState = { active: false, timestamp: 0 };
let lockdownState = { active: false, timestamp: 0 };

// GET /api/status - Check both alarm and lockdown status (for polling)
app.get('/api/status', (req, res) => {
    res.json({ alarm: alarmState, lockdown: lockdownState });
});

// POST /api/lockdown - Trigger/Stop remote lockdown
app.post('/api/lockdown', (req, res) => {
    const { active } = req.body;
    lockdownState = { active: !!active, timestamp: Date.now() };
    res.json({ message: `Lockdown ${active ? 'activated' : 'deactivated'}`, state: lockdownState });
});

// Existing /api/alarm routes for backward compatibility
app.get('/api/alarm', (req, res) => {
    res.json(alarmState);
});

// POST /api/alarm - Trigger/Stop alarm
app.post('/api/alarm', (req, res) => {
    const { active } = req.body;
    alarmState = { active: !!active, timestamp: Date.now() };
    res.json({ message: `Alarm ${active ? 'activated' : 'deactivated'}`, state: alarmState });
});

// Connect to MongoDB
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/zerotrustauth';

// Check for required environment variables
const requiredEnv = ['EMAIL_USER', 'EMAIL_PASS'];
const missingEnv = requiredEnv.filter(env => !process.env[env] || process.env[env].includes('your-'));

if (missingEnv.length > 0) {
    console.warn('\x1b[33m%s\x1b[0m', `WARNING: Missing or default values for: ${missingEnv.join(', ')}`);
    console.warn('\x1b[33m%s\x1b[0m', 'Real email OTP will not work until these are set in backend/.env');
}

mongoose.connect(MONGODB_URI)
    .then(() => {
        console.log('Connected to MongoDB');
        app.listen(PORT, () => {
            console.log(`Server is running on port ${PORT}`);
        });
    })
    .catch(err => {
        console.error('MongoDB connection error:', err);
    });
