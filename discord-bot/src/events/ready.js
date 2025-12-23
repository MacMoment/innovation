const { Events } = require('discord.js');

module.exports = {
    name: Events.ClientReady,
    once: true,
    execute(client) {
        console.log(`Ready! Logged in as ${client.user.tag}`);
        console.log(`Bot is in ${client.guilds.cache.size} guild(s)`);
        
        // Set presence
        client.user.setPresence({
            activities: [{ name: 'for rule breakers', type: 3 }], // Type 3 = Watching
            status: 'online',
        });
    },
};
