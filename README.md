# StaffSystem - Complete Staff Management Solution

A comprehensive staff management system for Minecraft servers, featuring a Minecraft plugin, web dashboard, and Discord bot integration. **Zero database setup required** - uses SQLite for automatic, hassle-free data storage.

## 🌟 Features

### Minecraft Plugin
- **Punishment System**: Ban, tempban, mute, tempmute, kick, warn
- **Freeze System**: Freeze players with auto-ban on logout
- **Staff Mode**: Toggle staff mode with special items
- **GUI Interface**: Beautiful inventory-based menus for all actions
- **Staff Chat**: Private communication channel for staff members
- **History Tracking**: Complete punishment history for all players
- **SQLite Database**: No setup required - works out of the box
- **Web Integration**: Real-time sync with web dashboard
- **Discord Notifications**: Send punishment notifications to Discord

### Web Dashboard
- **Modern UI**: Clean, dark-themed responsive dashboard
- **Staff Management**: Create and manage staff accounts with tier levels
- **Punishment Management**: View, search, and revoke punishments
- **Player History**: Detailed punishment history for each player
- **Activity Logging**: Track all staff actions
- **API Endpoints**: RESTful API for plugin communication
- **Role-Based Access**: Different permissions for different tiers

### Discord Bot
- **Slash Commands**: Modern Discord slash command integration
- **Punishment Lookup**: View punishments and player history
- **Staff Commands**: Unban, unmute, view staff team
- **Account Linking**: Link Discord accounts to staff accounts
- **Auto Role Assignment**: Automatically assign roles based on staff tier
- **Notifications**: Receive punishment notifications in channels

## 📁 Project Structure

```
├── minecraft-plugin/      # Spigot/Paper Minecraft plugin
│   ├── src/main/java/    # Java source code
│   ├── src/main/resources/ # Plugin configs
│   └── pom.xml           # Maven build file
├── web-dashboard/         # Node.js/Express web application
│   ├── src/              # Server source code
│   ├── views/            # EJS templates
│   └── package.json      # NPM dependencies
├── discord-bot/           # Discord.js bot
│   ├── src/              # Bot source code
│   └── package.json      # NPM dependencies
├── setup.sh              # Interactive setup script
└── README.md             # This file
```

## 🚀 Installation

### Quick Setup (Recommended)

Run the interactive setup script to configure everything automatically:

```bash
./setup.sh
```

The setup script will:
- Check for required prerequisites (Java 17+, Node.js 18+, Maven)
- Guide you through configuration for all components
- Generate secure API keys and secrets
- Create `.env` files for web dashboard and Discord bot
- Build the Minecraft plugin (if Maven is installed)
- Install npm dependencies
- Generate a configuration snippet for the Minecraft plugin

### Prerequisites
- Java 17+ (for Minecraft plugin)
- Node.js 18+ (for web dashboard and Discord bot)
- A Minecraft server running Spigot/Paper 1.20+

**No database setup required!** SQLite is used automatically.

### Manual Installation

If you prefer to set up each component manually, follow the steps below.

### 1. Minecraft Plugin Installation

1. Build the plugin:
```bash
cd minecraft-plugin
mvn clean package
```

2. Copy `target/StaffSystem-1.0.0.jar` to your server's `plugins` folder

3. Start your server to generate config files

4. Edit `plugins/StaffSystem/config.yml` to customize settings (optional):
```yaml
# The database is automatically created - no configuration needed!
database:
  file: database.db

web-integration:
  enabled: true
  # Supports both HTTP and HTTPS - use HTTP for local, HTTPS for production
  api-url: http://localhost:3000/api
  api-key: your-secret-api-key-here
```

5. Restart the server

### 2. Web Dashboard Installation

1. Install dependencies:
```bash
cd web-dashboard
npm install
```

2. Copy and edit environment file:
```bash
cp .env.example .env
```

3. Edit `.env` with your configuration:
```env
PORT=3000
SESSION_SECRET=your-super-secret-session-key
API_KEY=your-secret-api-key-here
DB_PATH=./data/staffsystem.db
```

4. Start the server:
```bash
npm start
```

5. Access the dashboard at `http://localhost:3000`
   - Default login: `admin` / `admin123`
   - **Change the default password immediately!**

### 3. Discord Bot Installation

1. Create a Discord application at [Discord Developer Portal](https://discord.com/developers/applications)

2. Enable the following intents:
   - Server Members Intent
   - Message Content Intent

3. Install dependencies:
```bash
cd discord-bot
npm install
```

4. Copy and edit environment file:
```bash
cp .env.example .env
```

5. Edit `.env` with your configuration:
```env
DISCORD_TOKEN=your-bot-token
GUILD_ID=your-guild-id
DB_PATH=../web-dashboard/data/staffsystem.db
LOG_CHANNEL_ID=your-log-channel-id
STAFF_CHANNEL_ID=your-staff-channel-id
```

6. Start the bot:
```bash
npm start
```

7. Invite the bot to your server with appropriate permissions

## 📝 Commands

### Minecraft Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/staff` | Open staff GUI | `staffsystem.staff` |
| `/ban <player> [duration] [reason]` | Ban a player | `staffsystem.ban` |
| `/tempban <player> <duration> [reason]` | Temporarily ban | `staffsystem.tempban` |
| `/unban <player>` | Unban a player | `staffsystem.unban` |
| `/mute <player> [duration] [reason]` | Mute a player | `staffsystem.mute` |
| `/tempmute <player> <duration> [reason]` | Temporarily mute | `staffsystem.tempmute` |
| `/unmute <player>` | Unmute a player | `staffsystem.unmute` |
| `/kick <player> [reason]` | Kick a player | `staffsystem.kick` |
| `/warn <player> [reason]` | Warn a player | `staffsystem.warn` |
| `/freeze <player>` | Freeze/unfreeze a player | `staffsystem.freeze` |
| `/history <player>` | View punishment history | `staffsystem.history` |
| `/staffchat [message]` | Toggle/send staff chat | `staffsystem.staffchat` |
| `/ss reload` | Reload configuration | `staffsystem.admin` |

**Duration Format**: `1y` (years), `1mo` (months), `1w` (weeks), `1d` (days), `1h` (hours), `1m` (minutes), `1s` (seconds)
- Examples: `7d`, `1d12h`, `2w`, `1mo`

### Discord Commands

| Command | Description |
|---------|-------------|
| `/history <player>` | View player punishment history |
| `/lookup <id>` | Look up a specific punishment |
| `/stats` | View server statistics |
| `/staff` | View staff team members |
| `/unban <player>` | Unban a player |
| `/unmute <player>` | Unmute a player |
| `/link <username>` | Link Discord to staff account |

## 🔒 Staff Tiers

| Tier | Name | Permissions |
|------|------|-------------|
| 1 | Trial Staff | Mute, Kick, Warn, Freeze |
| 2 | Moderator | + Ban |
| 3 | Senior Moderator | + View Staff List |
| 4 | Admin | + Manage Staff |
| 5 | Owner | Full Access |

## 🔧 Configuration

### config.yml (Minecraft Plugin)
The main configuration file for the plugin. See the default `config.yml` for all options.

### messages.yml (Minecraft Plugin)
Customize all plugin messages. Supports color codes and hex colors.

### .env (Web Dashboard & Discord Bot)
Environment variables for database, session, and API configuration.

## 📡 API Endpoints

The web dashboard provides a REST API for plugin communication:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/status` | GET | API status check |
| `/api/punishments` | GET | List all punishments |
| `/api/punishments/:id` | GET | Get specific punishment |
| `/api/punishments/sync` | POST | Sync punishment from plugin |
| `/api/punishments/:id/revoke` | POST | Revoke a punishment |
| `/api/players/:uuid/punishments` | GET | Get player punishments |
| `/api/players/:uuid/banned` | GET | Check if player is banned |
| `/api/players/:uuid/muted` | GET | Check if player is muted |
| `/api/staff` | GET | Get staff list |
| `/api/stats` | GET | Get statistics |

All API endpoints require Bearer token authentication with the API key.

## 🎨 Screenshots

The web dashboard features a modern, dark-themed UI with:
- Dashboard with statistics cards
- Punishment management with filtering
- Staff management with tier badges
- Player history views
- Mobile-responsive design

## 🛡️ Security

- All passwords are hashed with bcrypt
- API endpoints require authentication
- Session-based authentication for web dashboard
- Rate limiting on API endpoints
- CSRF protection enabled
- Helmet.js for security headers

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For support, please open an issue on GitHub or contact the development team.

---

**Note**: Make sure to change all default passwords and API keys before deploying to production!
