const { SlashCommandBuilder, PermissionFlagsBits } = require('discord.js');
const db = require('../utils/database');
const { createHistoryEmbed, createErrorEmbed } = require('../utils/embeds');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('history')
        .setDescription('View punishment history for a player')
        .addStringOption(option =>
            option.setName('player')
                .setDescription('Minecraft player name')
                .setRequired(true)
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.ModerateMembers),

    async execute(interaction) {
        const playerName = interaction.options.getString('player');

        await interaction.deferReply();

        try {
            const punishments = await db.query(
                'SELECT * FROM punishments WHERE player_name = ? ORDER BY timestamp DESC',
                [playerName]
            );

            const embed = createHistoryEmbed(playerName, punishments);
            await interaction.editReply({ embeds: [embed] });
        } catch (error) {
            console.error('History command error:', error);
            const embed = createErrorEmbed('Error', 'Failed to fetch player history.');
            await interaction.editReply({ embeds: [embed] });
        }
    }
};
