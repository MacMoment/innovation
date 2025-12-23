const express = require('express');
const router = express.Router();
const db = require('../utils/database');
const { apiAuth } = require('../middleware/auth');

// Webhook endpoint for punishment notifications from Minecraft plugin
router.post('/punishment', apiAuth, async (req, res) => {
    try {
        const {
            type,
            punishmentType,
            playerUuid,
            playerName,
            staffUuid,
            staffName,
            reason,
            duration,
            timestamp,
            server
        } = req.body;

        // Save to database
        await db.query(`
            INSERT INTO punishments 
            (player_uuid, player_name, staff_uuid, staff_name, type, reason, timestamp, duration, expiration, active, server)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        `, [
            playerUuid, playerName, staffUuid, staffName, punishmentType, reason,
            timestamp, duration, duration === -1 ? -1 : timestamp + duration, true, server || 'main'
        ]);

        // Send to Discord webhook if configured
        if (process.env.DISCORD_WEBHOOK_URL) {
            await sendDiscordNotification({
                type: 'punishment',
                punishmentType,
                playerName,
                staffName,
                reason,
                duration
            });
        }

        // Log activity
        await db.query(`
            INSERT INTO activity_log (action, details, ip_address)
            VALUES (?, ?, ?)
        `, ['WEBHOOK_PUNISHMENT', `${punishmentType} issued to ${playerName} by ${staffName}`, req.ip]);

        res.json({ success: true, message: 'Punishment logged' });
    } catch (err) {
        console.error('Webhook punishment error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Webhook endpoint for freeze notifications
router.post('/freeze', apiAuth, async (req, res) => {
    try {
        const { playerName, staffName, frozen, timestamp } = req.body;

        // Send to Discord webhook if configured
        if (process.env.DISCORD_WEBHOOK_URL) {
            await sendDiscordNotification({
                type: 'freeze',
                playerName,
                staffName,
                frozen
            });
        }

        // Log activity
        await db.query(`
            INSERT INTO activity_log (action, details, ip_address)
            VALUES (?, ?, ?)
        `, ['WEBHOOK_FREEZE', `${playerName} ${frozen ? 'frozen' : 'unfrozen'} by ${staffName}`, req.ip]);

        res.json({ success: true, message: 'Freeze notification logged' });
    } catch (err) {
        console.error('Webhook freeze error:', err);
        res.status(500).json({ success: false, error: 'Internal server error' });
    }
});

// Helper function to send Discord notifications
async function sendDiscordNotification(data) {
    const webhookUrl = process.env.DISCORD_WEBHOOK_URL;
    if (!webhookUrl) return;

    let embed;
    
    if (data.type === 'punishment') {
        const color = getPunishmentColor(data.punishmentType);
        embed = {
            title: `${data.punishmentType} - ${data.playerName}`,
            color: color,
            fields: [
                { name: 'Player', value: data.playerName, inline: true },
                { name: 'Staff', value: data.staffName, inline: true },
                { name: 'Reason', value: data.reason || 'No reason specified', inline: false },
                { name: 'Duration', value: formatDuration(data.duration), inline: true }
            ],
            timestamp: new Date().toISOString(),
            footer: { text: 'StaffSystem' }
        };
    } else if (data.type === 'freeze') {
        embed = {
            title: `Player ${data.frozen ? 'Frozen' : 'Unfrozen'}`,
            color: data.frozen ? 0x00BFFF : 0x90EE90,
            fields: [
                { name: 'Player', value: data.playerName, inline: true },
                { name: 'Staff', value: data.staffName, inline: true }
            ],
            timestamp: new Date().toISOString(),
            footer: { text: 'StaffSystem' }
        };
    }

    try {
        const response = await fetch(webhookUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ embeds: [embed] })
        });
        
        if (!response.ok) {
            console.error('Discord webhook failed:', response.status);
        }
    } catch (err) {
        console.error('Discord webhook error:', err);
    }
}

function getPunishmentColor(type) {
    switch (type) {
        case 'BAN':
        case 'TEMP_BAN':
            return 0xFF0000; // Red
        case 'MUTE':
        case 'TEMP_MUTE':
            return 0xFFA500; // Orange
        case 'KICK':
            return 0xFFFF00; // Yellow
        case 'WARN':
            return 0x90EE90; // Light green
        default:
            return 0x808080; // Gray
    }
}

function formatDuration(duration) {
    if (duration === -1 || duration === 0) {
        return 'Permanent';
    }
    
    const seconds = Math.floor(duration / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 0) return `${days} day(s)`;
    if (hours > 0) return `${hours} hour(s)`;
    if (minutes > 0) return `${minutes} minute(s)`;
    return `${seconds} second(s)`;
}

module.exports = router;
