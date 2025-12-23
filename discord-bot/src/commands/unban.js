const { SlashCommandBuilder, PermissionFlagsBits } = require('discord.js');
const db = require('../utils/database');
const { createSuccessEmbed, createErrorEmbed } = require('../utils/embeds');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('unban')
        .setDescription('Remove an active ban from a player')
        .addStringOption(option =>
            option.setName('player')
                .setDescription('Minecraft player name')
                .setRequired(true)
        )
        .addStringOption(option =>
            option.setName('reason')
                .setDescription('Reason for unbanning')
                .setRequired(false)
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.BanMembers),

    async execute(interaction) {
        const playerName = interaction.options.getString('player');
        const reason = interaction.options.getString('reason') || 'Unbanned via Discord';

        await interaction.deferReply();

        try {
            // Find active ban
            const activeBan = await db.queryOne(
                "SELECT * FROM punishments WHERE player_name = ? AND type IN ('BAN', 'TEMP_BAN') AND active = TRUE ORDER BY timestamp DESC LIMIT 1",
                [playerName]
            );

            if (!activeBan) {
                const embed = createErrorEmbed('Not Banned', `${playerName} does not have an active ban.`);
                return await interaction.editReply({ embeds: [embed] });
            }

            // Revoke the ban
            await db.query(
                'UPDATE punishments SET active = FALSE WHERE id = ?',
                [activeBan.id]
            );

            // Log the unban
            await db.query(`
                INSERT INTO activity_log (action, details, ip_address)
                VALUES (?, ?, ?)
            `, ['DISCORD_UNBAN', `${interaction.user.tag} unbanned ${playerName}: ${reason}`, 'Discord']);

            const embed = createSuccessEmbed(
                'Player Unbanned',
                `**${playerName}** has been unbanned.\n\n**Reason:** ${reason}\n**Unbanned by:** ${interaction.user.tag}`
            );
            await interaction.editReply({ embeds: [embed] });

            // Log to staff channel if configured
            const logChannelId = process.env.LOG_CHANNEL_ID;
            if (logChannelId) {
                const logChannel = interaction.guild.channels.cache.get(logChannelId);
                if (logChannel) {
                    await logChannel.send({ embeds: [embed] });
                }
            }
        } catch (error) {
            console.error('Unban command error:', error);
            const embed = createErrorEmbed('Error', 'Failed to unban player.');
            await interaction.editReply({ embeds: [embed] });
        }
    }
};
