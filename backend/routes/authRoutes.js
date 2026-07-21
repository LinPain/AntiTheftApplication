const express = require('express');
const router = express.Router();
const User = require('../models/User');
const OTPLog = require('../models/OTPLog');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const nodemailer = require('nodemailer');

const JWT_SECRET = process.env.JWT_SECRET || 'your_super_secret_key';
const MOCK_OTP = process.env.MOCK_OTP === 'true';

// Email Transporter Configuration
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS
    }
});

/**
 * Situation-Aware OTP Mailer
 */
const sendOTPEmail = async (email, code, type, username = 'User') => {
    let subject = 'Anti-Theft System - Mã xác thực';
    let message = `Mã xác thực của bạn là: ${code}`;

    switch (type) {
        case 'REGISTRATION':
            subject = 'Anti-Theft System - Xác minh đăng ký';
            message = `Chào mừng ${username}! Mã xác minh ĐĂNG KÝ tài khoản của bạn là: ${code}. Vui lòng nhập mã này để kích hoạt tài khoản.`;
            break;
        case 'LOGIN':
            subject = 'Anti-Theft System - Xác thực đăng nhập';
            message = `Bạn vừa đăng nhập vào hệ thống. Mã xác thực ĐĂNG NHẬP của bạn là: ${code}. Mã này có hiệu lực trong 10 phút.`;
            break;
        case 'RESET':
            subject = 'Anti-Theft System - Đặt lại mật khẩu';
            message = `Yêu cầu đặt lại mật khẩu đã được tạo. Mã ĐẶT LẠI MẬT KHẨU của bạn là: ${code}. Nếu không phải bạn yêu cầu, hãy bỏ qua email này.`;
            break;
        case 'CHANGE':
            subject = 'Anti-Theft System - Xác nhận đổi mật khẩu';
            message = `Bạn đang thực hiện thay đổi mật khẩu. Mã XÁC NHẬN ĐỔI MẬT KHẨU của bạn là: ${code}.`;
            break;
    }

    const mailOptions = {
        from: process.env.EMAIL_USER,
        to: email,
        subject: subject,
        text: message
    };

    return new Promise((resolve, reject) => {
        transporter.sendMail(mailOptions, (error, info) => {
            if (error) {
                console.error(`[MAIL-ERROR] Type: ${type}, To: ${email}, Error:`, error.message);
                reject(new Error("Gửi email thất bại. Vui lòng kiểm tra cấu hình server."));
            } else {
                console.log(`[MAIL-SENT] Type: ${type} sent to ${email}`);
                resolve(info);
            }
        });
    });
};

/**
 * Helper to generate and save OTP
 */
const createOTP = async (canonicalUsername, type, userRecord) => {
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = new Date(Date.now() + 600000); // 10 mins
    const identifier = canonicalUsername.toLowerCase().trim();

    await OTPLog.deleteMany({ identifier, type });
    await new OTPLog({ identifier, code, type, expiresAt }).save();

    await sendOTPEmail(userRecord.email, code, type, canonicalUsername);
    return code;
};

// Register
router.post('/register', async (req, res) => {
    try {
        let { username, password, email, name, phone } = req.body;
        username = username.toLowerCase().trim();
        email = email.toLowerCase().trim();

        const existingUser = await User.findOne({ $or: [{ username }, { email }] });
        if (existingUser) return res.status(400).json({ error: 'Username hoặc Email đã tồn tại' });

        const hashedPassword = await bcrypt.hash(password, 10);
        const user = new User({ username, password: hashedPassword, email, name, phone, isVerified: false });
        await user.save();

        const code = await createOTP(username, 'REGISTRATION', user);

        res.status(201).json({
            message: 'User registered. Please verify email.',
            verificationRequired: true,
            username,
            mockCode: MOCK_OTP ? code : undefined
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Verify Registration
router.post('/verify-registration', async (req, res) => {
    try {
        let { username, otp } = req.body;
        username = username.toLowerCase().trim();

        const user = await User.findOne({ $or: [{ username: username }, { email: username }] });
        if (!user) return res.status(404).json({ error: 'Không tìm thấy tài khoản' });

        const log = await OTPLog.findOne({
            identifier: user.username.toLowerCase(),
            code: otp,
            type: 'REGISTRATION'
        });

        if (!log || log.expiresAt < new Date()) {
            return res.status(401).json({ error: 'Mã xác minh không chính xác hoặc đã hết hạn' });
        }

        user.isVerified = true;
        await user.save();
        await OTPLog.deleteOne({ _id: log._id });

        res.json({ message: 'Tài khoản đã được xác minh thành công!' });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Login
router.post('/login', async (req, res) => {
    try {
        let { username, password, riskScore } = req.body;
        username = username.toLowerCase().trim();

        const user = await User.findOne({ $or: [{ username: username }, { email: username }] });
        if (!user || !(await bcrypt.compare(password, user.password))) {
            return res.status(401).json({ error: 'Sai tài khoản hoặc mật khẩu' });
        }

        if (!user.isVerified) {
            return res.status(403).json({ error: 'Email chưa được xác minh.', verificationRequired: true, username: user.username });
        }

        if (riskScore > 70) {
            return res.json({ message: 'High risk. Lockdown activated.', lockdownRequired: true });
        }

        const code = await createOTP(user.username, 'LOGIN', user);

        res.json({
            message: 'OTP sent',
            mfaRequired: true,
            username: user.username,
            mockCode: MOCK_OTP ? code : undefined
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Verify Login OTP
router.post('/verify-otp', async (req, res) => {
    try {
        let { username, otp } = req.body;
        username = username.toLowerCase().trim();

        const user = await User.findOne({ $or: [{ username: username }, { email: username }] });
        if (!user) return res.status(404).json({ error: 'Không tìm thấy tài khoản' });

        const log = await OTPLog.findOne({
            identifier: user.username.toLowerCase(),
            code: otp,
            type: 'LOGIN'
        });

        if (!log || log.expiresAt < new Date()) {
            return res.status(401).json({ error: 'Mã OTP không chính xác hoặc đã hết hạn' });
        }

        const token = jwt.sign({ id: user._id, username: user.username }, JWT_SECRET, { expiresIn: '1h' });

        await OTPLog.deleteOne({ _id: log._id });
        res.json({ message: 'Login successful', token, username: user.username });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Forgot Password
router.post('/forgot-password', async (req, res) => {
    try {
        let { identifier } = req.body;
        identifier = identifier.toLowerCase().trim();

        const user = await User.findOne({ $or: [{ username: identifier }, { email: identifier }] });
        if (!user) return res.status(404).json({ error: 'Không tìm thấy người dùng' });

        const code = await createOTP(user.username, 'RESET', user);

        res.json({
            message: 'OTP sent',
            username: user.username,
            mockCode: MOCK_OTP ? code : undefined
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Verify Reset
router.post('/verify-reset', async (req, res) => {
    try {
        let { username, otp } = req.body;
        username = username.toLowerCase().trim();

        const user = await User.findOne({ $or: [{ username: username }, { email: username }] });
        if (!user) return res.status(404).json({ error: 'Không tìm thấy tài khoản' });

        const log = await OTPLog.findOne({
            identifier: user.username.toLowerCase(),
            code: otp,
            type: 'RESET'
        });

        if (!log || log.expiresAt < new Date()) {
            return res.status(401).json({ error: 'Mã xác thực không hợp lệ' });
        }

        const resetToken = jwt.sign({ id: user._id, type: 'password_reset' }, JWT_SECRET, { expiresIn: '15m' });

        await OTPLog.deleteOne({ _id: log._id });
        res.json({ message: 'Xác thực thành công', resetToken });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Reset Password
router.post('/reset-password', async (req, res) => {
    try {
        const { resetToken, newPassword } = req.body;
        const decoded = jwt.verify(resetToken, JWT_SECRET);
        if (decoded.type !== 'password_reset') throw new Error('Invalid token');

        const user = await User.findById(decoded.id);
        user.password = await bcrypt.hash(newPassword, 10);
        await user.save();

        res.json({ message: 'Mật khẩu đã được thay đổi thành công!' });
    } catch (error) {
        res.status(401).json({ error: 'Phiên làm việc hết hạn hoặc không hợp lệ' });
    }
});

// Resend OTP
router.post('/resend-otp', async (req, res) => {
    try {
        let { username, type } = req.body;
        username = username.toLowerCase().trim();

        const user = await User.findOne({ $or: [{ username: username }, { email: username }] });
        if (!user) return res.status(404).json({ error: 'User not found' });

        const code = await createOTP(user.username, type, user);
        res.json({
            message: 'OTP resent',
            mockCode: MOCK_OTP ? code : undefined
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// Alerts
router.post('/alert-risk', async (req, res) => {
    try {
        let { username, riskScore } = req.body;
        username = username.toLowerCase().trim();
        const user = await User.findOne({ username });
        if (!user) return res.status(404).json({ error: 'User not found' });

        transporter.sendMail({
            from: process.env.EMAIL_USER,
            to: user.email,
            subject: 'Anti-Theft System - CẢNH BÁO RỦI RO CAO',
            text: `Phát hiện rủi ro bảo mật nghiêm trọng. Điểm: ${riskScore}/100.`
        });
        res.json({ message: 'Alert sent' });
    } catch (error) { res.status(500).json({ error: error.message }); }
});

router.post('/alert-sim', async (req, res) => {
    try {
        let { username, operatorName } = req.body;
        username = username.toLowerCase().trim();
        const user = await User.findOne({ username });
        if (!user) return res.status(404).json({ error: 'User not found' });

        transporter.sendMail({
            from: process.env.EMAIL_USER,
            to: user.email,
            subject: 'Anti-Theft System - CẢNH BÁO THAY ĐỔI SIM',
            text: `CẢNH BÁO: Phát hiện thay đổi SIM. Nhà mạng: ${operatorName || 'Unknown'}.`
        });
        res.json({ message: 'Alert sent' });
    } catch (error) { res.status(500).json({ error: error.message }); }
});

module.exports = router;
