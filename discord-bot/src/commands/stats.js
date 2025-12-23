const { SlashCommandBuilder, PermissionFlagsBits } = require('discord.js');
const db = require('../utils/database');
const { createStatsEmbed, createErrorEmbed } = require('../utils/embeds');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('stats')
        .setDescription('View StaffSystem statistics')
        .setDefaultMemberPermissions(PermissionFlagsBits.ModerateMembers),

    async execute(interaction) {
        await interaction.deferReply();

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

            const stats = {
                totalPunishments: totalPunishments?.count || 0,
                activeBans: activeBans?.count || 0,
                activeMutes: activeMutes?.count || 0,
                totalWarnings: totalWarnings?.count || 0,
                totalStaff: totalStaff?.count || 0
            };

            const embed = createStatsEmbed(stats);
            await interaction.editReply({ embeds: [embed] });
        } catch (error) {
            console.error('Stats command error:', error);
            const embed = createErrorEmbed('Error', 'Failed to fetch statistics.');
            await interaction.editReply({ embeds: [embed] });
        }
    }
};
