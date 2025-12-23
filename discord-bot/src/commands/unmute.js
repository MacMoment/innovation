const { SlashCommandBuilder, PermissionFlagsBits } = require('discord.js');
const db = require('../utils/database');
const { createSuccessEmbed, createErrorEmbed } = require('../utils/embeds');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('unmute')
        .setDescription('Remove an active mute from a player')
        .addStringOption(option =>
            option.setName('player')
                .setDescription('Minecraft player name')
                .setRequired(true)
        )
        .addStringOption(option =>
            option.setName('reason')
                .setDescription('Reason for unmuting')
                .setRequired(false)
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.ModerateMembers),

    async execute(interaction) {
        const playerName = interaction.options.getString('player');
        const reason = interaction.options.getString('reason') || 'Unmuted via Discord';

        await interaction.deferReply();

        try {
            // Find active mute
            const activeMute = await db.queryOne(
                "SELECT * FROM punishments WHERE player_name = ? AND type IN ('MUTE', 'TEMP_MUTE') AND active = TRUE ORDER BY timestamp DESC LIMIT 1",
                [playerName]
            );

            if (!activeMute) {
                const embed = createErrorEmbed('Not Muted', `${playerName} does not have an active mute.`);
                return await interaction.editReply({ embeds: [embed] });
            }

            // Revoke the mute
            await db.query(
                'UPDATE punishments SET active = FALSE WHERE id = ?',
                [activeMute.id]
            );

            // Log the unmute
            await db.query(`
                INSERT INTO activity_log (action, details, ip_address)
                VALUES (?, ?, ?)
            `, ['DISCORD_UNMUTE', `${interaction.user.tag} unmuted ${playerName}: ${reason}`, 'Discord']);

            const embed = createSuccessEmbed(
                'Player Unmuted',
                `**${playerName}** has been unmuted.\n\n**Reason:** ${reason}\n**Unmuted by:** ${interaction.user.tag}`
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
            console.error('Unmute command error:', error);
            const embed = createErrorEmbed('Error', 'Failed to unmute player.');
            await interaction.editReply({ embeds: [embed] });
        }
    }
};
