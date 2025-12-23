const { SlashCommandBuilder, PermissionFlagsBits, EmbedBuilder } = require('discord.js');
const db = require('../utils/database');
const { createErrorEmbed } = require('../utils/embeds');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('staff')
        .setDescription('View staff team members')
        .setDefaultMemberPermissions(PermissionFlagsBits.ModerateMembers),

    async execute(interaction) {
        await interaction.deferReply();

        try {
            const staff = await db.query(`
                SELECT su.*, st.name as tier_name, st.color as tier_color
                FROM staff_users su
                LEFT JOIN staff_tiers st ON su.tier = st.tier_level
                WHERE su.is_active = TRUE
                ORDER BY su.tier DESC, su.username ASC
            `);

            if (staff.length === 0) {
                const embed = createErrorEmbed('No Staff', 'No active staff members found.');
                return await interaction.editReply({ embeds: [embed] });
            }

            // Group by tier
            const groupedStaff = {};
            for (const member of staff) {
                const tierName = member.tier_name || `Tier ${member.tier}`;
                if (!groupedStaff[tierName]) {
                    groupedStaff[tierName] = [];
                }
                groupedStaff[tierName].push(member);
            }

            const embed = new EmbedBuilder()
                .setTitle('👥 Staff Team')
                .setColor(0x6366f1)
                .setTimestamp()
                .setFooter({ text: 'StaffSystem' });

            for (const [tierName, members] of Object.entries(groupedStaff)) {
                const memberList = members.map(m => {
                    let text = `• **${m.username}**`;
                    if (m.discord_id) {
                        text += ` (<@${m.discord_id}>)`;
                    }
                    return text;
                }).join('\n');

                embed.addFields({ name: tierName, value: memberList || 'None', inline: false });
            }

            await interaction.editReply({ embeds: [embed] });
        } catch (error) {
            console.error('Staff command error:', error);
            const embed = createErrorEmbed('Error', 'Failed to fetch staff list.');
            await interaction.editReply({ embeds: [embed] });
        }
    }
};
