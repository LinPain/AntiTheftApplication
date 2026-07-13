const mongoose = require('mongoose');

const intruderSchema = new mongoose.Schema({
    username: { type: String, required: true },
    imageBase64: { type: String, required: true },
    latitude: { type: Number, required: true },
    longitude: { type: Number, required: true },
    timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Intruder', intruderSchema);
