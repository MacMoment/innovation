const express = require('express');
const router = express.Router();
const db = require('../utils/database');
const { ensureAuthenticated, ensurePermission } = require('../middleware/auth');

// Punishments list
router.get('/', ensureAuthenticated, async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = 20;
        const offset = (page - 1) * limit;
        
        const type = req.query.type || '';
        const status = req.query.status || '';
        const search = req.query.search || '';

        let whereClause = '1=1';
        const params = [];

        if (type) {
            whereClause += ' AND type = ?';
            params.push(type);
        }

        if (status === 'active') {
            whereClause += ' AND active = TRUE';
        } else if (status === 'inactive') {
            whereClause += ' AND active = FALSE';
        }

        if (search) {
            whereClause += ' AND (player_name LIKE ? OR staff_name LIKE ? OR reason LIKE ?)';
            const searchPattern = `%${search}%`;
            params.push(searchPattern, searchPattern, searchPattern);
        }

        // Get total count
        const countResult = await db.queryOne(
            `SELECT COUNT(*) as total FROM punishments WHERE ${whereClause}`,
            params
        );
        const totalCount = countResult?.total || 0;
        const totalPages = Math.ceil(totalCount / limit);

        // Get punishments
        const punishments = await db.query(
            `SELECT * FROM punishments WHERE ${whereClause} ORDER BY timestamp DESC LIMIT ? OFFSET ?`,
            [...params, limit, offset]
        );

        res.render('pages/punishments', {
            title: 'Punishments',
            punishments,
            currentPage: page,
            totalPages,
            totalCount,
            filters: { type, status, search }
        });
    } catch (err) {
        console.error('Punishments list error:', err);
        req.flash('error_msg', 'Error loading punishments');
        res.redirect('/dashboard');
    }
});

// View single punishment
router.get('/view/:id', ensureAuthenticated, async (req, res) => {
    try {
        const punishment = await db.queryOne('SELECT * FROM punishments WHERE id = ?', [req.params.id]);
        
        if (!punishment) {
            req.flash('error_msg', 'Punishment not found');
            return res.redirect('/punishments');
        }

        // Get player history
        const playerHistory = await db.query(
            'SELECT * FROM punishments WHERE player_uuid = ? ORDER BY timestamp DESC',
            [punishment.player_uuid]
        );

        res.render('pages/punishment-view', {
            title: 'View Punishment',
            punishment,
            playerHistory
        });
    } catch (err) {
        console.error('View punishment error:', err);
        req.flash('error_msg', 'Error loading punishment');
        res.redirect('/punishments');
    }
});

// Revoke punishment
router.post('/revoke/:id', ensureAuthenticated, ensurePermission('ban'), async (req, res) => {
    try {
        const punishment = await db.queryOne('SELECT * FROM punishments WHERE id = ?', [req.params.id]);
        
        if (!punishment) {
            req.flash('error_msg', 'Punishment not found');
            return res.redirect('/punishments');
        }

        await db.query('UPDATE punishments SET active = FALSE WHERE id = ?', [req.params.id]);

        // Log activity
        await db.query(`
            INSERT INTO activity_log (staff_user_id, action, details, ip_address)
            VALUES (?, ?, ?, ?)
        `, [req.user.id, 'REVOKE_PUNISHMENT', 
            `Revoked ${punishment.type} for ${punishment.player_name}`, req.ip]);

        req.flash('success_msg', 'Punishment revoked successfully');
        res.redirect(`/punishments/view/${req.params.id}`);
    } catch (err) {
        console.error('Revoke punishment error:', err);
        req.flash('error_msg', 'Error revoking punishment');
        res.redirect('/punishments');
    }
});

// Search player
router.get('/player/:name', ensureAuthenticated, async (req, res) => {
    try {
        const playerName = req.params.name;
        
        const punishments = await db.query(
            'SELECT * FROM punishments WHERE player_name = ? ORDER BY timestamp DESC',
            [playerName]
        );

        // Get player stats
        const stats = {
            totalPunishments: punishments.length,
            activeBans: punishments.filter(p => 
                (p.type === 'BAN' || p.type === 'TEMP_BAN') && p.active
            ).length,
            activeMutes: punishments.filter(p => 
                (p.type === 'MUTE' || p.type === 'TEMP_MUTE') && p.active
            ).length,
            totalWarnings: punishments.filter(p => p.type === 'WARN').length
        };

        res.render('pages/player-history', {
            title: `Player History: ${playerName}`,
            playerName,
            punishments,
            stats
        });
    } catch (err) {
        console.error('Player history error:', err);
        req.flash('error_msg', 'Error loading player history');
        res.redirect('/punishments');
    }
});

module.exports = router;
