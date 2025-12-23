const { EmbedBuilder } = require('discord.js');

// Color constants
const COLORS = {
    BAN: 0xFF0000,
    TEMP_BAN: 0xFF4444,
    MUTE: 0xFFA500,
    TEMP_MUTE: 0xFFCC00,
    KICK: 0xFFFF00,
    WARN: 0x90EE90,
    SUCCESS: 0x00FF00,
    ERROR: 0xFF0000,
    INFO: 0x00BFFF
};

/**
 * Create a punishment embed
 */
function createPunishmentEmbed(punishment) {
    const color = COLORS[punishment.type] || COLORS.INFO;
    
    const embed = new EmbedBuilder()
        .setTitle(`${punishment.type.replace('_', ' ')} - ${punishment.player_name}`)
        .setColor(color)
        .addFields(
            { name: 'Player', value: punishment.player_name, inline: true },
            { name: 'Staff', value: punishment.staff_name, inline: true },
            { name: 'Server', value: punishment.server || 'main', inline: true },
            { name: 'Reason', value: punishment.reason || 'No reason specified', inline: false },
            { name: 'Duration', value: formatDuration(punishment.duration), inline: true },
            { name: 'Status', value: punishment.active ? '🟢 Active' : '🔴 Inactive', inline: true }
        )
        .setTimestamp(new Date(punishment.timestamp))
        .setFooter({ text: 'StaffSystem' });

    if (punishment.player_uuid) {
        embed.setThumbnail(`https://crafatar.com/avatars/${punishment.player_uuid}?size=64&overlay`);
    }

    return embed;
}

/**
 * Create a player history embed
 */
function createHistoryEmbed(playerName, punishments) {
    const embed = new EmbedBuilder()
        .setTitle(`Punishment History: ${playerName}`)
        .setColor(COLORS.INFO)
        .setTimestamp()
        .setFooter({ text: 'StaffSystem' });

    if (punishments.length === 0) {
        embed.setDescription('This player has no punishment history.');
        return embed;
    }

    // Count by type
    const counts = {
        bans: punishments.filter(p => p.type.includes('BAN')).length,
        mutes: punishments.filter(p => p.type.includes('MUTE')).length,
        kicks: punishments.filter(p => p.type === 'KICK').length,
        warns: punishments.filter(p => p.type === 'WARN').length
    };

    embed.addFields(
        { name: 'Total Punishments', value: punishments.length.toString(), inline: true },
        { name: 'Bans', value: counts.bans.toString(), inline: true },
        { name: 'Mutes', value: counts.mutes.toString(), inline: true },
        { name: 'Kicks', value: counts.kicks.toString(), inline: true },
        { name: 'Warnings', value: counts.warns.toString(), inline: true }
    );

    // Show recent punishments
    const recent = punishments.slice(0, 5);
    const recentText = recent.map(p => {
        const date = new Date(p.timestamp).toLocaleDateString();
        return `• **${p.type}** - ${date}\n  ${p.reason || 'No reason'}`;
    }).join('\n\n');

    embed.addFields({ name: 'Recent Punishments', value: recentText || 'None' });

    return embed;
}

/**
 * Create a stats embed
 */
function createStatsEmbed(stats) {
    return new EmbedBuilder()
        .setTitle('📊 StaffSystem Statistics')
        .setColor(COLORS.INFO)
        .addFields(
            { name: 'Total Punishments', value: stats.totalPunishments?.toString() || '0', inline: true },
            { name: 'Active Bans', value: stats.activeBans?.toString() || '0', inline: true },
            { name: 'Active Mutes', value: stats.activeMutes?.toString() || '0', inline: true },
            { name: 'Total Warnings', value: stats.totalWarnings?.toString() || '0', inline: true },
            { name: 'Total Staff', value: stats.totalStaff?.toString() || '0', inline: true }
        )
        .setTimestamp()
        .setFooter({ text: 'StaffSystem' });
}

/**
 * Create a success embed
 */
function createSuccessEmbed(title, description) {
    return new EmbedBuilder()
        .setTitle(`✅ ${title}`)
        .setDescription(description)
        .setColor(COLORS.SUCCESS)
        .setTimestamp()
        .setFooter({ text: 'StaffSystem' });
}

/**
 * Create an error embed
 */
function createErrorEmbed(title, description) {
    return new EmbedBuilder()
        .setTitle(`❌ ${title}`)
        .setDescription(description)
        .setColor(COLORS.ERROR)
        .setTimestamp()
        .setFooter({ text: 'StaffSystem' });
}

/**
 * Format duration from milliseconds to readable string
 */
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

module.exports = {
    COLORS,
    createPunishmentEmbed,
    createHistoryEmbed,
    createStatsEmbed,
    createSuccessEmbed,
    createErrorEmbed,
    formatDuration
};
