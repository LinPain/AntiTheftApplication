const mongoose = require('mongoose');

const otpLogSchema = new mongoose.Schema({
    identifier: { type: String, required: true, lowercase: true, trim: true }, // username or email
    code: { type: String, required: true },
    type: { type: String, enum: ['REGISTRATION', 'LOGIN', 'RESET', 'CHANGE'], required: true },
    expiresAt: { type: Date, required: true }
});

// Automatically delete after expiration
otpLogSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

module.exports = mongoose.model('OTPLog', otpLogSchema);
