const { Events } = require('discord.js');
const db = require('../utils/database');

module.exports = {
    name: Events.GuildMemberAdd,
    async execute(member) {
        try {
            // Check if this member has a linked staff account
            const staffAccount = await db.queryOne(
                'SELECT * FROM staff_users WHERE discord_id = ? AND is_active = TRUE',
                [member.id]
            );

            if (staffAccount) {
                // Get tier roles
                const tierRoles = process.env.TIER_ROLES?.split(',') || [];
                const tierRoleId = tierRoles[staffAccount.tier - 1];

                if (tierRoleId) {
                    try {
                        await member.roles.add(tierRoleId);
                        console.log(`Assigned tier ${staffAccount.tier} role to ${member.user.tag}`);
                    } catch (error) {
                        console.error('Failed to assign tier role on join:', error);
                    }
                }

                // Notify staff channel
                const staffChannelId = process.env.STAFF_CHANNEL_ID;
                if (staffChannelId) {
                    const staffChannel = member.guild.channels.cache.get(staffChannelId);
                    if (staffChannel) {
                        await staffChannel.send(
                            `👋 Staff member **${member.user.tag}** (${staffAccount.username}) has joined the server.`
                        );
                    }
                }
            }
        } catch (error) {
            console.error('Error in guildMemberAdd event:', error);
        }
    },
};
