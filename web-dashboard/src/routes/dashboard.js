const express = require('express');
const router = express.Router();
const db = require('../utils/database');
const { ensureAuthenticated } = require('../middleware/auth');

// Dashboard home
router.get('/', ensureAuthenticated, (req, res) => {
    try {
        // Get statistics
        const stats = getStats();
        
        // Get recent punishments
        const recentPunishments = db.query(`
            SELECT * FROM punishments 
            ORDER BY timestamp DESC 
            LIMIT 10
        `);

        // Get recent activity
        const recentActivity = db.query(`
            SELECT al.*, su.username 
            FROM activity_log al
            LEFT JOIN staff_users su ON al.staff_user_id = su.id
            ORDER BY al.timestamp DESC 
            LIMIT 10
        `);

        res.render('pages/dashboard', {
            title: 'Dashboard',
            stats,
            recentPunishments,
            recentActivity
        });
    } catch (err) {
        console.error('Dashboard error:', err);
        req.flash('error_msg', 'Error loading dashboard');
        res.render('pages/dashboard', {
            title: 'Dashboard',
            stats: {},
            recentPunishments: [],
            recentActivity: []
        });
    }
});

// Get statistics
function getStats() {
    const totalPunishments = db.queryOne('SELECT COUNT(*) as count FROM punishments');
    const activeBans = db.queryOne(`
        SELECT COUNT(*) as count FROM punishments 
        WHERE type IN ('BAN', 'TEMP_BAN') AND active = 1
    `);
    const activeMutes = db.queryOne(`
        SELECT COUNT(*) as count FROM punishments 
        WHERE type IN ('MUTE', 'TEMP_MUTE') AND active = 1
    `);
    const totalWarnings = db.queryOne(`
        SELECT COUNT(*) as count FROM punishments 
        WHERE type = 'WARN'
    `);
    const totalStaff = db.queryOne('SELECT COUNT(*) as count FROM staff_users WHERE is_active = 1');

    // Today's punishments
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayTimestamp = today.getTime();
    
    const todayPunishments = db.queryOne(`
        SELECT COUNT(*) as count FROM punishments 
        WHERE timestamp >= ?
    `, [todayTimestamp]);

    return {
        totalPunishments: totalPunishments?.count || 0,
        activeBans: activeBans?.count || 0,
        activeMutes: activeMutes?.count || 0,
        totalWarnings: totalWarnings?.count || 0,
        totalStaff: totalStaff?.count || 0,
        todayPunishments: todayPunishments?.count || 0
    };
}

module.exports = router;
