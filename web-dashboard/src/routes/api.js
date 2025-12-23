const express = require('express');
const router = express.Router();
const db = require('../utils/database');
const { apiAuth } = require('../middleware/auth');

// Get API status
router.get('/status', (req, res) => {
    res.json({
        status: 'online',
        version: '1.0.0',
        timestamp: Date.now()
    });
});

// Get all punishments (authenticated)
router.get('/punishments', apiAuth, (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 50;
        const offset = (page - 1) * limit;

        const punishments = db.query(
            'SELECT * FROM punishments ORDER BY timestamp DESC LIMIT ? OFFSET ?',
            [limit, offset]
        );

        const total = db.queryOne('SELECT COUNT(*) as count FROM punishments');

        res.json({
            success: true,
            data: punishments,
            pagination: {
                page,
                limit,
                total: total?.count || 0,
                pages: Math.ceil((total?.count || 0) / limit)
            }
        });
    } catch (err) {
        console.error('API error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Get punishment by ID
router.get('/punishments/:id', apiAuth, (req, res) => {
    try {
        const punishment = db.queryOne(
            'SELECT * FROM punishments WHERE id = ?',
            [req.params.id]
        );

        if (!punishment) {
            return res.status(404).json({ success: false, error: 'Punishment not found' });
        }

        res.json({ success: true, data: punishment });
    } catch (err) {
        console.error('API error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Get player punishments
router.get('/players/:uuid/punishments', apiAuth, (req, res) => {
    try {
        const punishments = db.query(
            'SELECT * FROM punishments WHERE player_uuid = ? ORDER BY timestamp DESC',
            [req.params.uuid]
        );

        res.json({ success: true, data: punishments });
    } catch (err) {
        console.error('API error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Check if player is banned
router.get('/players/:uuid/banned', apiAuth, (req, res) => {
    try {
        const ban = db.queryOne(`
            SELECT * FROM punishments 
            WHERE player_uuid = ? AND type IN ('BAN', 'TEMP_BAN') AND active = 1
            ORDER BY timestamp DESC LIMIT 1
        `, [req.params.uuid]);

        if (ban) {
            // Check if temp ban has expired
            if (ban.expiration > 0 && ban.expiration < Date.now()) {
                db.query('UPDATE punishments SET active = 0 WHERE id = ?', [ban.id]);
                return res.json({ success: true, banned: false });
            }
            return res.json({ success: true, banned: true, data: ban });
        }

        res.json({ success: true, banned: false });
    } catch (err) {
        console.error('API error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Check if player is muted
router.get('/players/:uuid/muted', apiAuth, (req, res) => {
    try {
        const mute = db.queryOne(`
            SELECT * FROM punishments 
            WHERE player_uuid = ? AND type IN ('MUTE', 'TEMP_MUTE') AND active = 1
            ORDER BY timestamp DESC LIMIT 1
        `, [req.params.uuid]);

        if (mute) {
            // Check if temp mute has expired
            if (mute.expiration > 0 && mute.expiration < Date.now()) {
                db.query('UPDATE punishments SET active = 0 WHERE id = ?', [mute.id]);
                return res.json({ success: true, muted: false });
            }
            return res.json({ success: true, muted: true, data: mute });
        }

        res.json({ success: true, muted: false });
    } catch (err) {
        console.error('API error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Sync punishment from Minecraft plugin
router.post('/punishments/sync', apiAuth, (req, res) => {
    try {
        const {
            playerUuid, playerName, staffUuid, staffName,
            type, reason, timestamp, duration, expiration, active, server
        } = req.body;

        // Check if punishment already exists
        const existing = db.queryOne(
            'SELECT id FROM punishments WHERE player_uuid = ? AND timestamp = ? AND type = ?',
            [playerUuid, timestamp, type]
        );

        if (existing) {
            // Update existing
            db.query(`
                UPDATE punishments SET active = ?, expiration = ? WHERE id = ?
            `, [active ? 1 : 0, expiration, existing.id]);
            
            return res.json({ success: true, action: 'updated', id: existing.id });
        }

        // Create new
        const result = db.query(`
            INSERT INTO punishments 
            (player_uuid, player_name, staff_uuid, staff_name, type, reason, timestamp, duration, expiration, active, server)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `, [playerUuid, playerName, staffUuid, staffName, type, reason, timestamp, duration, expiration, active ? 1 : 0, server || 'main']);

        res.json({ success: true, action: 'created', id: result.lastInsertRowid });
    } catch (err) {
        console.error('API sync error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Revoke punishment via API
router.post('/punishments/:id/revoke', apiAuth, (req, res) => {
    try {
        const result = db.query(
            'UPDATE punishments SET active = 0 WHERE id = ?',
            [req.params.id]
        );

        if (result.changes === 0) {
            return res.status(404).json({ success: false, error: 'Punishment not found' });
        }

        res.json({ success: true, message: 'Punishment revoked' });
    } catch (err) {
        console.error('API revoke error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Get staff members
router.get('/staff', apiAuth, (req, res) => {
    try {
        const staff = db.query(`
            SELECT id, username, minecraft_uuid, minecraft_name, discord_id, tier, role, is_active, last_login
            FROM staff_users
            ORDER BY tier DESC, username ASC
        `);

        res.json({ success: true, data: staff });
    } catch (err) {
        console.error('API staff error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Get stats
router.get('/stats', apiAuth, (req, res) => {
    try {
        const totalPunishments = db.queryOne('SELECT COUNT(*) as count FROM punishments');
        const activeBans = db.queryOne(
            "SELECT COUNT(*) as count FROM punishments WHERE type IN ('BAN', 'TEMP_BAN') AND active = 1"
        );
        const activeMutes = db.queryOne(
            "SELECT COUNT(*) as count FROM punishments WHERE type IN ('MUTE', 'TEMP_MUTE') AND active = 1"
        );
        const totalWarnings = db.queryOne(
            "SELECT COUNT(*) as count FROM punishments WHERE type = 'WARN'"
        );
        const totalStaff = db.queryOne(
            'SELECT COUNT(*) as count FROM staff_users WHERE is_active = 1'
        );

        res.json({
            success: true,
            data: {
                totalPunishments: totalPunishments?.count || 0,
                activeBans: activeBans?.count || 0,
                activeMutes: activeMutes?.count || 0,
                totalWarnings: totalWarnings?.count || 0,
                totalStaff: totalStaff?.count || 0
            }
        });
    } catch (err) {
        console.error('API stats error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

module.exports = router;
