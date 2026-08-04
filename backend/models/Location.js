const mongoose = require('mongoose');

const locationSchema = new mongoose.Schema({
    username: { type: String, required: true },
    deviceId: { type: String, required: true },
    deviceName: { type: String },
    latitude: { type: Number, required: true },
    longitude: { type: Number, required: true },
    accuracy: { type: Number, default: 0 },
    speed: { type: Number, default: 0 },

    // Detailed Device Info (Latest Snapshot)
    batteryLevel: { type: Number },
    isCharging: { type: Boolean },
    networkType: { type: String },
    carrier: { type: String },
    ipAddress: { type: String },

    // Hardware & OS
    manufacturer: { type: String },
    model: { type: String },
    androidVersion: { type: String },
    apiLevel: { type: Number },

    // Security Status
    isRooted: { type: Boolean },
    isEncryptionEnabled: { type: Boolean },
    isDeveloperMode: { type: Boolean },
    isUsbDebuggingEnabled: { type: Boolean },
    riskScore: { type: Number, default: 0 },

    timestamp: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Location', locationSchema);
