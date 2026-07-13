const express = require('express');
const router = express.Router();
const User = require('../models/User');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const nodemailer = require('nodemailer');

const JWT_SECRET = process.env.JWT_SECRET || 'your_super_secret_key';

// Email Transporter Configuration
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS
    }
});

// Register with Email Verification
router.post('/register', async (req, res) => {
    try {
        const { username, password, email } = req.body;

        // Check if user already exists
        const existingUser = await User.findOne({ $or: [{ username }, { email }] });
        if (existingUser) {
            return res.status(400).json({ error: 'Username or Email already exists' });
        }

        const hashedPassword = await bcrypt.hash(password, 10);

        // Generate OTP for registration
        const otpCode = Math.floor(100000 + Math.random() * 900000).toString();

        const user = new User({
            username,
            password: hashedPassword,
            email,
            isVerified: false,
            otp: {
                code: otpCode,
                expiresAt: new Date(Date.now() + 10 * 60 * 1000) // 10 mins for registration
            }
        });

        await user.save();

        // Send Verification Email
        const mailOptions = {
            from: process.env.EMAIL_USER,
            to: email,
            subject: 'Anti-Theft System - Xác minh tài khoản',
            text: `Chào mừng ${username}! Mã xác minh đăng ký của bạn là: ${otpCode}. Vui lòng nhập mã này để kích hoạt tài khoản.`
        };

        transporter.sendMail(mailOptions, (error, info) => {
            if (error) console.error('Email send error:', error);
        });

        res.status(201).json({
            message: 'User registered. Please verify your email.',
            verificationRequired: true,
            username: username
        });
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

// Verify Registration OTP
router.post('/verify-registration', async (req, res) => {
    try {
        const { username, otp } = req.body;
        const user = await User.findOne({ username });

        if (!user || !user.otp || user.otp.code !== otp || user.otp.expiresAt < new Date()) {
            return res.status(401).json({ error: 'Mã xác minh không chính xác hoặc đã hết hạn' });
        }

        // Activate user
        user.isVerified = true;
        user.otp = undefined; // Clear OTP
        await user.save();

        res.json({ message: 'Tài khoản đã được xác minh thành công! Bạn có thể đăng nhập ngay bây giờ.' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Login (Always requires OTP after password)
router.post('/login', async (req, res) => {
    try {
        const { username, password, riskScore } = req.body;

        // Search by username OR email
        const user = await User.findOne({
            $or: [
                { username: username },
                { email: username }
            ]
        });

        if (!user || !(await bcrypt.compare(password, user.password))) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        // Check if email is verified
        if (!user.isVerified) {
            return res.status(403).json({
                error: 'Email chưa được xác minh. Vui lòng kiểm tra email của bạn.',
                verificationRequired: true,
                username: user.username
            });
        }

        // High Risk -> Lockdown
        if (riskScore > 70) {
            console.log(`[RISK] HIGH RISK DETECTED (${riskScore}) for ${user.username}. Forcing lockdown.`);
            return res.json({
                message: 'High risk detected. Device lockdown activated.',
                lockdownRequired: true
            });
        }

        // Mandatory OTP for login
        console.log(`[AUTH] Password correct for ${user.username}. Triggering login OTP.`);
        const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
        user.otp = {
            code: otpCode,
            expiresAt: new Date(Date.now() + 5 * 60 * 1000) // 5 mins
        };
        await user.save();

        const mailOptions = {
            from: process.env.EMAIL_USER,
            to: user.email,
            subject: 'Anti-Theft System - Mã xác thực đăng nhập',
            text: `Bạn vừa đăng nhập. Mã xác thực của bạn là: ${otpCode}`
        };

        transporter.sendMail(mailOptions, (error, info) => {
            if (error) console.error('Email send error:', error);
        });

        return res.json({
            message: 'OTP sent successfully',
            mfaRequired: true,
            username: user.username
        });

    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Verify Login OTP
router.post('/verify-otp', async (req, res) => {
    try {
        const { username, otp } = req.body;
        const user = await User.findOne({ username });

        if (!user || !user.otp || user.otp.code !== otp || user.otp.expiresAt < new Date()) {
            return res.status(401).json({ error: 'Mã OTP không chính xác hoặc đã hết hạn' });
        }

        user.otp = undefined;
        await user.save();

        const token = jwt.sign({ id: user._id, username: user.username }, JWT_SECRET, { expiresIn: '1h' });
        res.json({ message: 'Login successful', token, username: user.username });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Alert Risk
router.post('/alert-risk', async (req, res) => {
    try {
        const { username, riskScore } = req.body;
        const user = await User.findOne({ username });

        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        const mailOptions = {
            from: process.env.EMAIL_USER,
            to: user.email,
            subject: 'Anti-Theft System - CẢNH BÁO RỦI RO CAO',
            text: `Phát hiện rủi ro bảo mật nghiêm trọng trên thiết bị của bạn.
                   \nĐiểm rủi ro hiện tại: ${riskScore}/100.
                   \nVui lòng truy cập dashboard ngay lập tức để kiểm tra trạng thái thiết bị.`
        };

        transporter.sendMail(mailOptions, (error, info) => {
            if (error) {
                console.error('[ALERT-ERROR] Email failed:', error);
            } else {
                console.log(`[ALERT] Risk warning email sent to ${user.email} (Score: ${riskScore})`);
            }
        });

        res.json({ message: 'Risk alert processed' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
