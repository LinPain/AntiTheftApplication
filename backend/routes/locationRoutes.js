const express = require('express');
const router = express.Router();
const Location = require('../models/Location');

// POST /api/location - Save new location data
router.post('/', async (req, res) => {
    try {
        const { deviceId, latitude, longitude } = req.body;
        const newLocation = new Location({ deviceId, latitude, longitude });
        await newLocation.save();
        res.status(201).json({ message: 'Location saved successfully' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// GET /api/location/:deviceId - Retrieve location history for a device
router.get('/:deviceId', async (req, res) => {
    try {
        const { deviceId } = req.params;
        const history = await Location.find({ deviceId }).sort({ timestamp: -1 }).limit(50);
        res.status(200).json(history);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
