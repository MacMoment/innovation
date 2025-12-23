const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const { body, validationResult } = require('express-validator');
const db = require('../utils/database');
const { ensureAuthenticated, ensureTier, ensurePermission } = require('../middleware/auth');

// Staff list
router.get('/', ensureAuthenticated, ensureTier(3), async (req, res) => {
    try {
        const staffMembers = await db.query(`
            SELECT su.*, st.name as tier_name, st.color as tier_color
            FROM staff_users su
            LEFT JOIN staff_tiers st ON su.tier = st.tier_level
            ORDER BY su.tier DESC, su.username ASC
        `);

        const tiers = await db.query('SELECT * FROM staff_tiers ORDER BY tier_level ASC');

        res.render('pages/staff', {
            title: 'Staff Management',
            staffMembers,
            tiers
        });
    } catch (err) {
        console.error('Staff list error:', err);
        req.flash('error_msg', 'Error loading staff list');
        res.redirect('/dashboard');
    }
});

// Edit staff member
router.get('/edit/:id', ensureAuthenticated, ensureTier(4), async (req, res) => {
    try {
        const staffMember = await db.queryOne(`
            SELECT su.*, st.name as tier_name
            FROM staff_users su
            LEFT JOIN staff_tiers st ON su.tier = st.tier_level
            WHERE su.id = ?
        `, [req.params.id]);

        if (!staffMember) {
            req.flash('error_msg', 'Staff member not found');
            return res.redirect('/staff');
        }

        // Can't edit users with higher or equal tier unless you're owner
        if (staffMember.tier >= req.user.tier && req.user.tier < 5) {
            req.flash('error_msg', 'Cannot edit this staff member');
            return res.redirect('/staff');
        }

        const tiers = await db.query('SELECT * FROM staff_tiers WHERE tier_level <= ? ORDER BY tier_level ASC', 
            [req.user.tier]);

        res.render('pages/staff-edit', {
            title: 'Edit Staff Member',
            staffMember,
            tiers
        });
    } catch (err) {
        console.error('Edit staff error:', err);
        req.flash('error_msg', 'Error loading staff member');
        res.redirect('/staff');
    }
});

// Update staff member
router.post('/edit/:id',
    ensureAuthenticated,
    ensureTier(4),
    [
        body('email').isEmail().normalizeEmail(),
        body('tier').isInt({ min: 1, max: 5 })
    ],
    async (req, res) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            req.flash('error_msg', 'Invalid input');
            return res.redirect(`/staff/edit/${req.params.id}`);
        }

        try {
            const staffMember = await db.queryOne('SELECT * FROM staff_users WHERE id = ?', [req.params.id]);
            
            if (!staffMember) {
                req.flash('error_msg', 'Staff member not found');
                return res.redirect('/staff');
            }

            // Can't edit users with higher or equal tier unless you're owner
            if (staffMember.tier >= req.user.tier && req.user.tier < 5) {
                req.flash('error_msg', 'Cannot edit this staff member');
                return res.redirect('/staff');
            }

            const { email, tier, minecraft_uuid, discord_id, is_active, newPassword } = req.body;

            // Can't set tier higher than your own
            if (parseInt(tier) > req.user.tier) {
                req.flash('error_msg', 'Cannot set tier higher than your own');
                return res.redirect(`/staff/edit/${req.params.id}`);
            }

            let updateQuery = `
                UPDATE staff_users SET 
                    email = ?, tier = ?, minecraft_uuid = ?, discord_id = ?, is_active = ?
            `;
            let params = [email, tier, minecraft_uuid || null, discord_id || null, is_active === 'on'];

            // Update password if provided
            if (newPassword && newPassword.length >= 6) {
                const hashedPassword = await bcrypt.hash(newPassword, 10);
                updateQuery += ', password = ?';
                params.push(hashedPassword);
            }

            updateQuery += ' WHERE id = ?';
            params.push(req.params.id);

            await db.query(updateQuery, params);

            // Log activity
            await db.query(`
                INSERT INTO activity_log (staff_user_id, action, details, ip_address)
                VALUES (?, ?, ?, ?)
            `, [req.user.id, 'UPDATE_STAFF', `Updated staff: ${staffMember.username}`, req.ip]);

            req.flash('success_msg', 'Staff member updated successfully');
            res.redirect('/staff');
        } catch (err) {
            console.error('Update staff error:', err);
            req.flash('error_msg', 'Error updating staff member');
            res.redirect(`/staff/edit/${req.params.id}`);
        }
    }
);

// Delete staff member
router.post('/delete/:id', ensureAuthenticated, ensureTier(4), async (req, res) => {
    try {
        const staffMember = await db.queryOne('SELECT * FROM staff_users WHERE id = ?', [req.params.id]);
        
        if (!staffMember) {
            req.flash('error_msg', 'Staff member not found');
            return res.redirect('/staff');
        }

        // Can't delete users with higher or equal tier
        if (staffMember.tier >= req.user.tier) {
            req.flash('error_msg', 'Cannot delete this staff member');
            return res.redirect('/staff');
        }

        // Can't delete yourself
        if (staffMember.id === req.user.id) {
            req.flash('error_msg', 'Cannot delete your own account');
            return res.redirect('/staff');
        }

        await db.query('DELETE FROM staff_users WHERE id = ?', [req.params.id]);

        // Log activity
        await db.query(`
            INSERT INTO activity_log (staff_user_id, action, details, ip_address)
            VALUES (?, ?, ?, ?)
        `, [req.user.id, 'DELETE_STAFF', `Deleted staff: ${staffMember.username}`, req.ip]);

        req.flash('success_msg', 'Staff member deleted successfully');
        res.redirect('/staff');
    } catch (err) {
        console.error('Delete staff error:', err);
        req.flash('error_msg', 'Error deleting staff member');
        res.redirect('/staff');
    }
});

// Tier management
router.get('/tiers', ensureAuthenticated, ensureTier(5), async (req, res) => {
    try {
        const tiers = await db.query('SELECT * FROM staff_tiers ORDER BY tier_level ASC');
        
        res.render('pages/tiers', {
            title: 'Tier Management',
            tiers
        });
    } catch (err) {
        console.error('Tiers error:', err);
        req.flash('error_msg', 'Error loading tiers');
        res.redirect('/staff');
    }
});

// Update tier
router.post('/tiers/:id', ensureAuthenticated, ensureTier(5), async (req, res) => {
    try {
        const { name, color, discord_role_id, permissions } = req.body;
        
        await db.query(`
            UPDATE staff_tiers SET name = ?, color = ?, discord_role_id = ?, permissions = ?
            WHERE id = ?
        `, [name, color, discord_role_id || null, permissions, req.params.id]);

        req.flash('success_msg', 'Tier updated successfully');
        res.redirect('/staff/tiers');
    } catch (err) {
        console.error('Update tier error:', err);
        req.flash('error_msg', 'Error updating tier');
        res.redirect('/staff/tiers');
    }
});

module.exports = router;
