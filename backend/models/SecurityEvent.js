const mongoose = require('mongoose');

const securityEventSchema = new mongoose.Schema({
    username: { type: String, required: true },
    deviceId: { type: String },
    eventType: { type: String, required: true }, // LOST_MODE_ENABLED, ALARM_STARTED, etc.
    details: { type: String },
    timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.model('SecurityEvent', securityEventSchema);
