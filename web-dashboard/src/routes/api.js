const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
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
router.get('/punishments', apiAuth, async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 50;
        const offset = (page - 1) * limit;

        const punishments = await db.query(
            'SELECT * FROM punishments ORDER BY timestamp DESC LIMIT ? OFFSET ?',
            [limit, offset]
        );

        const total = await db.queryOne('SELECT COUNT(*) as count FROM punishments');

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
router.get('/punishments/:id', apiAuth, async (req, res) => {
    try {
        const punishment = await db.queryOne(
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
router.get('/players/:uuid/punishments', apiAuth, async (req, res) => {
    try {
        const punishments = await db.query(
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
router.get('/players/:uuid/banned', apiAuth, async (req, res) => {
    try {
        const ban = await db.queryOne(`
            SELECT * FROM punishments 
            WHERE player_uuid = ? AND type IN ('BAN', 'TEMP_BAN') AND active = TRUE
            ORDER BY timestamp DESC LIMIT 1
        `, [req.params.uuid]);

        if (ban) {
            // Check if temp ban has expired
            if (ban.expiration > 0 && ban.expiration < Date.now()) {
                await db.query('UPDATE punishments SET active = FALSE WHERE id = ?', [ban.id]);
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
router.get('/players/:uuid/muted', apiAuth, async (req, res) => {
    try {
        const mute = await db.queryOne(`
            SELECT * FROM punishments 
            WHERE player_uuid = ? AND type IN ('MUTE', 'TEMP_MUTE') AND active = TRUE
            ORDER BY timestamp DESC LIMIT 1
        `, [req.params.uuid]);

        if (mute) {
            // Check if temp mute has expired
            if (mute.expiration > 0 && mute.expiration < Date.now()) {
                await db.query('UPDATE punishments SET active = FALSE WHERE id = ?', [mute.id]);
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
router.post('/punishments/sync', apiAuth, async (req, res) => {
    try {
        const {
            playerUuid, playerName, staffUuid, staffName,
            type, reason, timestamp, duration, expiration, active, server
        } = req.body;

        // Check if punishment already exists
        const existing = await db.queryOne(
            'SELECT id FROM punishments WHERE player_uuid = ? AND timestamp = ? AND type = ?',
            [playerUuid, timestamp, type]
        );

        if (existing) {
            // Update existing
            await db.query(`
                UPDATE punishments SET active = ?, expiration = ? WHERE id = ?
            `, [active, expiration, existing.id]);
            
            return res.json({ success: true, action: 'updated', id: existing.id });
        }

        // Create new
        const result = await db.query(`
            INSERT INTO punishments 
            (player_uuid, player_name, staff_uuid, staff_name, type, reason, timestamp, duration, expiration, active, server)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `, [playerUuid, playerName, staffUuid, staffName, type, reason, timestamp, duration, expiration, active, server || 'main']);

        res.json({ success: true, action: 'created', id: result.insertId });
    } catch (err) {
        console.error('API sync error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Revoke punishment via API
router.post('/punishments/:id/revoke', apiAuth, async (req, res) => {
    try {
        const result = await db.query(
            'UPDATE punishments SET active = FALSE WHERE id = ?',
            [req.params.id]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({ success: false, error: 'Punishment not found' });
        }

        res.json({ success: true, message: 'Punishment revoked' });
    } catch (err) {
        console.error('API revoke error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Get staff members
router.get('/staff', apiAuth, async (req, res) => {
    try {
        const staff = await db.query(`
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
router.get('/stats', apiAuth, async (req, res) => {
    try {
        const totalPunishments = await db.queryOne('SELECT COUNT(*) as count FROM punishments');
        const activeBans = await db.queryOne(
            "SELECT COUNT(*) as count FROM punishments WHERE type IN ('BAN', 'TEMP_BAN') AND active = TRUE"
        );
        const activeMutes = await db.queryOne(
            "SELECT COUNT(*) as count FROM punishments WHERE type IN ('MUTE', 'TEMP_MUTE') AND active = TRUE"
        );
        const totalWarnings = await db.queryOne(
            "SELECT COUNT(*) as count FROM punishments WHERE type = 'WARN'"
        );
        const totalStaff = await db.queryOne(
            'SELECT COUNT(*) as count FROM staff_users WHERE is_active = TRUE'
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
