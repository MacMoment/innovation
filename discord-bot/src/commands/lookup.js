const { SlashCommandBuilder, PermissionFlagsBits } = require('discord.js');
const db = require('../utils/database');
const { createPunishmentEmbed, createErrorEmbed } = require('../utils/embeds');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('lookup')
        .setDescription('Look up a specific punishment by ID')
        .addIntegerOption(option =>
            option.setName('id')
                .setDescription('Punishment ID')
                .setRequired(true)
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.ModerateMembers),

    async execute(interaction) {
        const punishmentId = interaction.options.getInteger('id');

        await interaction.deferReply();

        try {
            const punishment = await db.queryOne(
                'SELECT * FROM punishments WHERE id = ?',
                [punishmentId]
            );

            if (!punishment) {
                const embed = createErrorEmbed('Not Found', `Punishment #${punishmentId} not found.`);
                return await interaction.editReply({ embeds: [embed] });
            }

            const embed = createPunishmentEmbed(punishment);
            await interaction.editReply({ embeds: [embed] });
        } catch (error) {
            console.error('Lookup command error:', error);
            const embed = createErrorEmbed('Error', 'Failed to look up punishment.');
            await interaction.editReply({ embeds: [embed] });
        }
    }
};
