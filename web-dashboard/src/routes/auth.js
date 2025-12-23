const express = require('express');
const router = express.Router();
const passport = require('passport');
const bcrypt = require('bcryptjs');
const { body, validationResult } = require('express-validator');
const db = require('../utils/database');
const { ensureGuest, ensureAuthenticated, ensureTier } = require('../middleware/auth');

// Login page
router.get('/login', ensureGuest, (req, res) => {
    res.render('pages/login', { title: 'Login' });
});

// Login handler
router.post('/login', ensureGuest, (req, res, next) => {
    passport.authenticate('local', {
        successRedirect: '/dashboard',
        failureRedirect: '/auth/login',
        failureFlash: true
    })(req, res, next);
});

// Register page (admin only)
router.get('/register', ensureAuthenticated, ensureTier(4), (req, res) => {
    res.render('pages/register', { title: 'Register New Staff' });
});

// Register handler
router.post('/register',
    ensureAuthenticated,
    ensureTier(4),
    [
        body('username').trim().isLength({ min: 3, max: 50 }).escape(),
        body('email').isEmail().normalizeEmail(),
        body('password').isLength({ min: 6 }),
        body('tier').isInt({ min: 1, max: 5 })
    ],
    async (req, res) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            req.flash('error_msg', 'Invalid input. Please check all fields.');
            return res.redirect('/auth/register');
        }

        try {
            const { username, email, password, tier, minecraft_uuid, discord_id } = req.body;

            // Check if user exists
            const existing = await db.queryOne(
                'SELECT id FROM staff_users WHERE username = ? OR email = ?',
                [username, email]
            );

            if (existing) {
                req.flash('error_msg', 'Username or email already exists');
                return res.redirect('/auth/register');
            }

            // Can't create users with higher tier than yourself
            if (tier > req.user.tier) {
                req.flash('error_msg', 'Cannot create user with higher tier than yourself');
                return res.redirect('/auth/register');
            }

            // Hash password
            const hashedPassword = await bcrypt.hash(password, 10);

            // Create user
            await db.query(`
                INSERT INTO staff_users (username, email, password, tier, minecraft_uuid, discord_id)
                VALUES (?, ?, ?, ?, ?, ?)
            `, [username, email, hashedPassword, tier, minecraft_uuid || null, discord_id || null]);

            // Log activity
            await db.query(`
                INSERT INTO activity_log (staff_user_id, action, details, ip_address)
                VALUES (?, ?, ?, ?)
            `, [req.user.id, 'CREATE_STAFF', `Created staff account: ${username}`, req.ip]);

            req.flash('success_msg', `Staff account "${username}" created successfully`);
            res.redirect('/staff');
        } catch (err) {
            console.error('Registration error:', err);
            req.flash('error_msg', 'Error creating account');
            res.redirect('/auth/register');
        }
    }
);

// Logout
router.get('/logout', ensureAuthenticated, (req, res) => {
    req.logout((err) => {
        if (err) {
            console.error('Logout error:', err);
        }
        req.flash('success_msg', 'You have been logged out');
        res.redirect('/auth/login');
    });
});

// Change password page
router.get('/change-password', ensureAuthenticated, (req, res) => {
    res.render('pages/change-password', { title: 'Change Password' });
});

// Change password handler
router.post('/change-password',
    ensureAuthenticated,
    [
        body('currentPassword').notEmpty(),
        body('newPassword').isLength({ min: 6 }),
        body('confirmPassword').custom((value, { req }) => {
            if (value !== req.body.newPassword) {
                throw new Error('Passwords do not match');
            }
            return true;
        })
    ],
    async (req, res) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            req.flash('error_msg', errors.array()[0].msg);
            return res.redirect('/auth/change-password');
        }

        try {
            const { currentPassword, newPassword } = req.body;

            // Verify current password
            const user = await db.queryOne('SELECT password FROM staff_users WHERE id = ?', [req.user.id]);
            const isMatch = await bcrypt.compare(currentPassword, user.password);
            
            if (!isMatch) {
                req.flash('error_msg', 'Current password is incorrect');
                return res.redirect('/auth/change-password');
            }

            // Update password
            const hashedPassword = await bcrypt.hash(newPassword, 10);
            await db.query('UPDATE staff_users SET password = ? WHERE id = ?', [hashedPassword, req.user.id]);

            // Log activity
            await db.query(`
                INSERT INTO activity_log (staff_user_id, action, details, ip_address)
                VALUES (?, ?, ?, ?)
            `, [req.user.id, 'CHANGE_PASSWORD', 'Changed password', req.ip]);

            req.flash('success_msg', 'Password changed successfully');
            res.redirect('/dashboard');
        } catch (err) {
            console.error('Password change error:', err);
            req.flash('error_msg', 'Error changing password');
            res.redirect('/auth/change-password');
        }
    }
);

module.exports = router;
