const express = require('express');
const router = express.Router();
const Location = require('../models/Location');

// POST /api/:username/location - Save new location data
router.post('/', async (req, res) => {
    try {
        const username = req.username; // From middleware in server.js
        const { deviceId, deviceName, latitude, longitude } = req.body;
        console.log(`[GPS] Location received from ${username} (${deviceName || deviceId}): ${latitude}, ${longitude}`);

        const newLocation = new Location({ username, deviceId, deviceName, latitude, longitude });
        await newLocation.save();

        // Prevent data overflow: Keep only last 100 locations per device
        try {
            const MAX_HISTORY = 100;
            const count = await Location.countDocuments({ username, deviceId });
            if (count > MAX_HISTORY) {
                const oldestToKeep = await Location.find({ username, deviceId })
                    .sort({ timestamp: -1 })
                    .skip(MAX_HISTORY - 1)
                    .limit(1);

                if (oldestToKeep.length > 0) {
                    await Location.deleteMany({
                        username,
                        deviceId,
                        timestamp: { $lt: oldestToKeep[0].timestamp }
                    });
                }
            }
        } catch (pruneError) {
            console.error('[ERROR] Failed to prune location history:', pruneError.message);
        }

        // Emit real-time update via Socket.io
        const io = req.app.get('io');
        if (io) {
            const room = username.toLowerCase().trim();
            console.log(`[SOCKET] Broadcasting update to room: ${room}`);
            io.to(room).emit('locationUpdate', {
                deviceId,
                deviceName,
                latitude,
                longitude,
                timestamp: newLocation.timestamp
            });
        }

        res.status(201).json({ message: 'Location saved successfully' });
    } catch (error) {
        console.error(`[ERROR] Failed to save location for ${req.params.username}:`, error.message);
        res.status(500).json({ error: error.message });
    }
});

// GET /api/:username/location/devices/list - List unique devices for a user
router.get('/devices/list', async (req, res) => {
    try {
        const username = req.username;
        const devices = await Location.distinct('deviceId', { username });
        res.status(200).json(devices);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// GET /api/:username/location/devices/status - List devices with last known info
router.get('/devices/status', async (req, res) => {
    try {
        const username = req.username;
        const devices = await Location.aggregate([
            { $match: { username } },
            { $sort: { timestamp: -1 } },
            { $group: {
                _id: "$deviceId",
                deviceName: { $first: "$deviceName" },
                lastLatitude: { $first: "$latitude" },
                lastLongitude: { $first: "$longitude" },
                lastTimestamp: { $first: "$timestamp" }
            }}
        ]);
        res.status(200).json(devices);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// GET /api/:username/location/:deviceId - Retrieve location history for a device
router.get('/:deviceId', async (req, res) => {
    try {
        const username = req.username; // From middleware in server.js
        const { deviceId } = req.params;
        const history = await Location.find({ username, deviceId }).sort({ timestamp: -1 }).limit(50);
        res.status(200).json(history);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// DELETE /api/:username/location/:deviceId - Remove all records for a specific device
router.delete('/:deviceId', async (req, res) => {
    try {
        const username = req.username;
        const { deviceId } = req.params;
        await Location.deleteMany({ username, deviceId });
        res.status(200).json({ message: `Đã gỡ thiết bị ${deviceId} thành công.` });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
