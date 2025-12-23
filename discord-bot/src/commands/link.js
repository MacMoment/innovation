const { SlashCommandBuilder, PermissionFlagsBits } = require('discord.js');
const db = require('../utils/database');
const { createSuccessEmbed, createErrorEmbed } = require('../utils/embeds');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('link')
        .setDescription('Link your Discord account to a staff account')
        .addStringOption(option =>
            option.setName('username')
                .setDescription('Your staff account username')
                .setRequired(true)
        )
        .setDefaultMemberPermissions(PermissionFlagsBits.ModerateMembers),

    async execute(interaction) {
        const username = interaction.options.getString('username');
        const discordId = interaction.user.id;

        await interaction.deferReply({ ephemeral: true });

        try {
            // Check if staff account exists
            const staffAccount = await db.queryOne(
                'SELECT * FROM staff_users WHERE username = ? AND is_active = TRUE',
                [username]
            );

            if (!staffAccount) {
                const embed = createErrorEmbed('Not Found', `Staff account "${username}" not found or is inactive.`);
                return await interaction.editReply({ embeds: [embed] });
            }

            // Check if already linked to another account
            if (staffAccount.discord_id && staffAccount.discord_id !== discordId) {
                const embed = createErrorEmbed('Already Linked', 'This staff account is already linked to another Discord account.');
                return await interaction.editReply({ embeds: [embed] });
            }

            // Check if this Discord account is already linked elsewhere
            const existingLink = await db.queryOne(
                'SELECT * FROM staff_users WHERE discord_id = ? AND username != ?',
                [discordId, username]
            );

            if (existingLink) {
                const embed = createErrorEmbed('Already Linked', `Your Discord account is already linked to "${existingLink.username}".`);
                return await interaction.editReply({ embeds: [embed] });
            }

            // Link the account
            await db.query(
                'UPDATE staff_users SET discord_id = ? WHERE username = ?',
                [discordId, username]
            );

            // Try to assign tier role if configured
            const tierRoles = process.env.TIER_ROLES?.split(',') || [];
            const tierRoleId = tierRoles[staffAccount.tier - 1];
            
            if (tierRoleId) {
                try {
                    const member = await interaction.guild.members.fetch(discordId);
                    await member.roles.add(tierRoleId);
                } catch (roleError) {
                    console.error('Failed to assign role:', roleError);
                }
            }

            const embed = createSuccessEmbed(
                'Account Linked',
                `Your Discord account has been linked to staff account **${username}**.\n\nTier: ${staffAccount.tier}`
            );
            await interaction.editReply({ embeds: [embed] });

            // Log the action
            await db.query(`
                INSERT INTO activity_log (staff_user_id, action, details, ip_address)
                VALUES (?, ?, ?, ?)
            `, [staffAccount.id, 'DISCORD_LINK', `Discord account linked: ${interaction.user.tag}`, 'Discord']);

        } catch (error) {
            console.error('Link command error:', error);
            const embed = createErrorEmbed('Error', 'Failed to link accounts.');
            await interaction.editReply({ embeds: [embed] });
        }
    }
};
