const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
    username: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    otp: { code: String, expiresAt: Date }
});

module.exports = mongoose.model('User', userSchema);
