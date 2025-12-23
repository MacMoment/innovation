#!/bin/bash

# StaffSystem Setup Script
# This script helps configure and set up all components of the StaffSystem

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Print banner
print_banner() {
    echo -e "${CYAN}"
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║                   StaffSystem Setup Script                     ║"
    echo "║         Complete Staff Management Solution for Minecraft       ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Print colored messages
info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Check prerequisites
check_prerequisites() {
    echo ""
    info "Checking prerequisites..."
    
    local missing_deps=0
    
    # Check for Java
    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 17 ] 2>/dev/null; then
            success "Java 17+ found"
        else
            warning "Java found but version may be below 17. Required: Java 17+"
        fi
    else
        error "Java not found. Required: Java 17+"
        missing_deps=1
    fi
    
    # Check for Maven (optional but recommended for building plugin)
    if command_exists mvn; then
        success "Maven found"
        MAVEN_AVAILABLE=true
    else
        warning "Maven not found. You won't be able to build the Minecraft plugin from source."
        MAVEN_AVAILABLE=false
    fi
    
    # Check for Node.js
    if command_exists node; then
        NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
        if [ "$NODE_VERSION" -ge 18 ] 2>/dev/null; then
            success "Node.js 18+ found (v$(node -v | cut -d'v' -f2))"
        else
            warning "Node.js found but version may be below 18. Required: Node.js 18+"
        fi
    else
        error "Node.js not found. Required: Node.js 18+"
        missing_deps=1
    fi
    
    # Check for npm
    if command_exists npm; then
        success "npm found"
    else
        error "npm not found. Required for web dashboard and Discord bot."
        missing_deps=1
    fi
    
    if [ $missing_deps -eq 1 ]; then
        echo ""
        error "Missing required dependencies. Please install them before continuing."
        echo ""
        echo "Installation guides:"
        echo "  - Java: https://adoptium.net/"
        echo "  - Node.js: https://nodejs.org/"
        echo "  - Maven: https://maven.apache.org/install.html"
        exit 1
    fi
    
    echo ""
    success "All required prerequisites are installed!"
}

# Generate a random string for secrets
generate_secret() {
    local length=${1:-32}
    if command_exists openssl; then
        # openssl rand -hex produces 2 hex chars per byte, so divide by 2
        openssl rand -hex $((length / 2))
    else
        cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w "$length" | head -n 1
    fi
}

# Prompt for input with default value
prompt_input() {
    local prompt_text="$1"
    local default_value="$2"
    local var_name="$3"
    local is_secret="$4"
    
    if [ -n "$default_value" ]; then
        prompt_text="$prompt_text [$default_value]"
    fi
    
    if [ "$is_secret" = "true" ]; then
        read -s -p "$prompt_text: " input
        echo ""
    else
        read -p "$prompt_text: " input
    fi
    
    if [ -z "$input" ]; then
        eval "$var_name=\"$default_value\""
    else
        eval "$var_name=\"$input\""
    fi
}

# Prompt for yes/no
prompt_yes_no() {
    local prompt_text="$1"
    local default="$2"
    
    if [ "$default" = "y" ]; then
        prompt_text="$prompt_text [Y/n]"
    else
        prompt_text="$prompt_text [y/N]"
    fi
    
    read -p "$prompt_text: " response
    
    if [ -z "$response" ]; then
        response="$default"
    fi
    
    case "$response" in
        [yY][eE][sS]|[yY]) return 0 ;;
        *) return 1 ;;
    esac
}

# Configure web dashboard
configure_web_dashboard() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}                 Web Dashboard Configuration                   ${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    
    # Port
    prompt_input "Web dashboard port" "3000" "WEB_PORT"
    
    # Protocol
    echo ""
    info "Choose the protocol for your web dashboard:"
    echo "  1) HTTP (for local/development use)"
    echo "  2) HTTPS (for production with SSL/reverse proxy)"
    read -p "Select [1]: " protocol_choice
    protocol_choice=${protocol_choice:-1}
    
    if [ "$protocol_choice" = "2" ]; then
        WEB_PROTOCOL="https"
    else
        WEB_PROTOCOL="http"
    fi
    
    # Host/Domain
    prompt_input "Web dashboard host/domain (e.g., localhost, yourdomain.com)" "localhost" "WEB_HOST"
    
    # Construct URL
    if [ "$WEB_PORT" = "80" ] || [ "$WEB_PORT" = "443" ]; then
        WEBSITE_URL="${WEB_PROTOCOL}://${WEB_HOST}"
    else
        WEBSITE_URL="${WEB_PROTOCOL}://${WEB_HOST}:${WEB_PORT}"
    fi
    
    WEB_API_URL="${WEBSITE_URL}/api"
    
    info "Web Dashboard URL will be: $WEBSITE_URL"
    info "API URL will be: $WEB_API_URL"
    
    # Generate secrets
    echo ""
    info "Generating secure secrets..."
    SESSION_SECRET=$(generate_secret 64)
    API_KEY=$(generate_secret 32)
    
    success "Secrets generated!"
    echo ""
    warning "IMPORTANT: Save these keys securely!"
    echo "  API Key: $API_KEY"
    echo "  (You'll need this for the Minecraft plugin configuration)"
    
    # Discord webhook (optional)
    echo ""
    if prompt_yes_no "Do you want to configure Discord webhook notifications?" "n"; then
        prompt_input "Discord Webhook URL" "" "DISCORD_WEBHOOK_URL"
    else
        DISCORD_WEBHOOK_URL=""
    fi
    
    # Environment
    echo ""
    prompt_input "Environment (development/production)" "development" "NODE_ENV"
    
    # Create .env file for web dashboard
    info "Creating web dashboard .env file..."
    
    # Determine if HTTPS should be forced
    if [ "$WEB_PROTOCOL" = "https" ]; then
        FORCE_HTTPS="true"
    else
        FORCE_HTTPS="false"
    fi
    
    cat > "$SCRIPT_DIR/web-dashboard/.env" << EOF
# StaffSystem Web Dashboard Environment Configuration
# Generated by setup.sh on $(date)

# Server Configuration
PORT=$WEB_PORT
NODE_ENV=$NODE_ENV

# Force HTTPS - set to true to require HTTPS for session cookies
# Only enable if you have HTTPS configured (via reverse proxy or SSL certificates)
FORCE_HTTPS=$FORCE_HTTPS

# Session Secret (keep this secure!)
SESSION_SECRET=$SESSION_SECRET

# API Key for Minecraft Plugin communication (must match plugin config)
API_KEY=$API_KEY

# Discord Integration (optional)
DISCORD_WEBHOOK_URL=$DISCORD_WEBHOOK_URL

# Website URL (for links in Discord messages)
# Supports both HTTP and HTTPS - configure based on your setup
WEBSITE_URL=$WEBSITE_URL

# Database file path (SQLite - no setup required!)
# The database file will be created automatically
DB_PATH=./data/staffsystem.db
EOF

    success "Web dashboard .env file created!"
}

# Configure Discord bot
configure_discord_bot() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}                  Discord Bot Configuration                    ${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    
    info "Get your Discord bot token from: https://discord.com/developers/applications"
    echo ""
    
    prompt_input "Discord Bot Token" "" "DISCORD_TOKEN" "true"
    prompt_input "Discord Server (Guild) ID" "" "GUILD_ID"
    
    echo ""
    info "Channel IDs for logging (right-click channel > Copy ID, enable Developer Mode in Discord settings)"
    prompt_input "Log Channel ID" "" "LOG_CHANNEL_ID"
    prompt_input "Staff Channel ID" "" "STAFF_CHANNEL_ID"
    
    echo ""
    info "Role IDs for staff tiers (comma-separated, tier1 through tier5)"
    prompt_input "Tier Role IDs (tier1,tier2,tier3,tier4,tier5)" "" "TIER_ROLES"
    
    # Create .env file for Discord bot
    info "Creating Discord bot .env file..."
    
    cat > "$SCRIPT_DIR/discord-bot/.env" << EOF
# Discord Bot Configuration
# Generated by setup.sh on $(date)

# Discord Bot Token (get from Discord Developer Portal)
DISCORD_TOKEN=$DISCORD_TOKEN

# Discord Server ID (Guild ID)
GUILD_ID=$GUILD_ID

# Database file path (SQLite - no setup required!)
# Should point to the same database as the web dashboard
DB_PATH=../web-dashboard/data/staffsystem.db

# Staff channel IDs
LOG_CHANNEL_ID=$LOG_CHANNEL_ID
STAFF_CHANNEL_ID=$STAFF_CHANNEL_ID

# Role IDs for each tier (comma-separated: tier1,tier2,tier3,tier4,tier5)
TIER_ROLES=$TIER_ROLES

# API Configuration (for web dashboard communication)
# Supports both HTTP and HTTPS - must match your web dashboard configuration
WEB_API_URL=$WEB_API_URL
WEB_API_KEY=$API_KEY
EOF

    success "Discord bot .env file created!"
}

# Configure Minecraft plugin
configure_minecraft_plugin() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}               Minecraft Plugin Configuration                  ${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    
    info "The Minecraft plugin is configured via config.yml after first run."
    info "You'll need to set these values in plugins/StaffSystem/config.yml:"
    echo ""
    echo "  web-integration:"
    echo "    enabled: true"
    echo "    api-url: $WEB_API_URL"
    echo "    api-key: $API_KEY"
    echo ""
    
    if [ -n "$DISCORD_WEBHOOK_URL" ]; then
        echo "  discord:"
        echo "    enabled: true"
        echo "    webhook-url: $DISCORD_WEBHOOK_URL"
        echo ""
    fi
    
    # Create a helper config snippet
    mkdir -p "$SCRIPT_DIR/minecraft-plugin/generated"
    cat > "$SCRIPT_DIR/minecraft-plugin/generated/config-snippet.yml" << EOF
# StaffSystem Plugin Configuration Snippet
# Generated by setup.sh on $(date)
# Copy these values to your plugins/StaffSystem/config.yml

web-integration:
  enabled: true
  # API endpoint of your web dashboard
  # Supports both HTTP and HTTPS - must match your web dashboard
  api-url: $WEB_API_URL
  # Secret key for authentication (must match web dashboard API_KEY)
  api-key: $API_KEY

discord:
  enabled: true
  # Discord bot webhook for notifications
  webhook-url: ${DISCORD_WEBHOOK_URL:-https://discord.com/api/webhooks/your-webhook-url}
EOF

    success "Config snippet saved to minecraft-plugin/generated/config-snippet.yml"
}

# Build Minecraft plugin
build_minecraft_plugin() {
    if [ "$MAVEN_AVAILABLE" = true ]; then
        echo ""
        if prompt_yes_no "Would you like to build the Minecraft plugin now?" "y"; then
            info "Building Minecraft plugin..."
            cd "$SCRIPT_DIR/minecraft-plugin"
            
            if mvn clean package; then
                if [ -f "target/StaffSystem-1.0.0.jar" ]; then
                    success "Plugin built successfully!"
                    echo ""
                    info "The plugin JAR is located at:"
                    echo "  $SCRIPT_DIR/minecraft-plugin/target/StaffSystem-1.0.0.jar"
                    echo ""
                    info "Copy this file to your Minecraft server's plugins folder."
                else
                    warning "Build completed but JAR file not found. Check Maven output above."
                fi
            else
                error "Maven build failed. Check the error messages above."
            fi
            
            cd "$SCRIPT_DIR"
        fi
    else
        warning "Maven is not installed. Skipping plugin build."
        info "To build manually: cd minecraft-plugin && mvn clean package"
    fi
}

# Install npm dependencies
install_npm_dependencies() {
    echo ""
    if prompt_yes_no "Would you like to install npm dependencies for web dashboard and Discord bot?" "y"; then
        info "Installing web dashboard dependencies..."
        cd "$SCRIPT_DIR/web-dashboard"
        if npm install; then
            success "Web dashboard dependencies installed!"
            WEB_DEPS_INSTALLED=true
        else
            error "Failed to install web dashboard dependencies. Check npm output above."
            WEB_DEPS_INSTALLED=false
        fi
        
        info "Installing Discord bot dependencies..."
        cd "$SCRIPT_DIR/discord-bot"
        if npm install; then
            success "Discord bot dependencies installed!"
            DISCORD_DEPS_INSTALLED=true
        else
            error "Failed to install Discord bot dependencies. Check npm output above."
            DISCORD_DEPS_INSTALLED=false
        fi
        
        cd "$SCRIPT_DIR"
    else
        WEB_DEPS_INSTALLED=false
        DISCORD_DEPS_INSTALLED=false
    fi
}

# Start services
start_services() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}                       Start Services                          ${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    
    if prompt_yes_no "Would you like to start the services now?" "y"; then
        # Check if .env files exist
        if [ ! -f "$SCRIPT_DIR/web-dashboard/.env" ]; then
            error "Web dashboard .env file not found. Please configure the web dashboard first."
            return 1
        fi
        
        # Check if dependencies were installed
        if [ "$WEB_DEPS_INSTALLED" = false ] && [ ! -d "$SCRIPT_DIR/web-dashboard/node_modules" ]; then
            warning "Web dashboard dependencies may not be installed."
            if ! prompt_yes_no "Continue anyway?" "n"; then
                info "Run 'npm install' in the web-dashboard directory first."
                return 1
            fi
        fi
        
        # Select mode
        echo ""
        info "Select startup mode:"
        echo "  1) Development mode (runs in foreground with hot reload)"
        echo "  2) Production mode (runs in background)"
        read -p "Select [1]: " startup_mode
        startup_mode=${startup_mode:-1}
        
        if [ "$startup_mode" = "2" ]; then
            start_services_production
        else
            start_services_development
        fi
    fi
}

# Start services in development mode
start_services_development() {
    echo ""
    info "Starting services in development mode..."
    warning "Services will run in the foreground. Use Ctrl+C to stop."
    echo ""
    
    # Check if tmux or screen is available for running multiple services
    if command_exists tmux; then
        info "Using tmux to manage multiple services..."
        
        # Create a new tmux session
        tmux kill-session -t staffsystem 2>/dev/null || true
        
        # Start web dashboard in first window
        tmux new-session -d -s staffsystem -n web-dashboard -c "$SCRIPT_DIR/web-dashboard"
        tmux send-keys -t staffsystem:web-dashboard "npm run dev 2>&1 || npm start" Enter
        
        # Start Discord bot in second window (if configured)
        if [ -f "$SCRIPT_DIR/discord-bot/.env" ]; then
            tmux new-window -t staffsystem -n discord-bot -c "$SCRIPT_DIR/discord-bot"
            tmux send-keys -t staffsystem:discord-bot "npm run dev 2>&1 || npm start" Enter
        fi
        
        echo ""
        success "Services started in tmux session 'staffsystem'!"
        echo ""
        echo "  To attach to the session: tmux attach -t staffsystem"
        echo "  To switch windows: Ctrl+B then 0/1/2"
        echo "  To detach: Ctrl+B then D"
        echo "  To stop all: tmux kill-session -t staffsystem"
        echo ""
        
        info "Web Dashboard URL: $WEBSITE_URL"
        info "Default login: admin / admin123"
        echo ""
        
        if prompt_yes_no "Attach to the tmux session now?" "y"; then
            tmux attach -t staffsystem
        fi
    else
        # Fallback: start web dashboard only (user can start discord bot separately)
        warning "tmux not found. Starting web dashboard only (foreground)."
        warning "To run both services, install tmux: sudo apt install tmux"
        echo ""
        
        info "Starting web dashboard..."
        info "Press Ctrl+C to stop."
        echo ""
        
        cd "$SCRIPT_DIR/web-dashboard"
        npm run dev 2>&1 || npm start
    fi
}

# Start services in production mode (background)
start_services_production() {
    echo ""
    info "Starting services in production mode..."
    
    # Create logs directory
    mkdir -p "$SCRIPT_DIR/logs"
    
    # Start web dashboard
    info "Starting web dashboard..."
    cd "$SCRIPT_DIR/web-dashboard"
    nohup npm start > "$SCRIPT_DIR/logs/web-dashboard.log" 2>&1 &
    WEB_PID=$!
    echo $WEB_PID > "$SCRIPT_DIR/logs/web-dashboard.pid"
    success "Web dashboard started (PID: $WEB_PID)"
    
    # Start Discord bot (if configured)
    if [ -f "$SCRIPT_DIR/discord-bot/.env" ]; then
        info "Starting Discord bot..."
        cd "$SCRIPT_DIR/discord-bot"
        nohup npm start > "$SCRIPT_DIR/logs/discord-bot.log" 2>&1 &
        DISCORD_PID=$!
        echo $DISCORD_PID > "$SCRIPT_DIR/logs/discord-bot.pid"
        success "Discord bot started (PID: $DISCORD_PID)"
    fi
    
    cd "$SCRIPT_DIR"
    
    # Wait a moment for services to start
    sleep 3
    
    # Check if services are running
    echo ""
    if kill -0 $WEB_PID 2>/dev/null; then
        success "Web dashboard is running!"
        echo "   URL: $WEBSITE_URL"
        echo "   Log: $SCRIPT_DIR/logs/web-dashboard.log"
        echo "   PID: $WEB_PID"
    else
        error "Web dashboard failed to start. Check logs at: $SCRIPT_DIR/logs/web-dashboard.log"
    fi
    
    if [ -n "$DISCORD_PID" ]; then
        if kill -0 $DISCORD_PID 2>/dev/null; then
            success "Discord bot is running!"
            echo "   Log: $SCRIPT_DIR/logs/discord-bot.log"
            echo "   PID: $DISCORD_PID"
        else
            error "Discord bot failed to start. Check logs at: $SCRIPT_DIR/logs/discord-bot.log"
        fi
    fi
    
    echo ""
    echo -e "${YELLOW}To stop services:${NC}"
    echo "   kill \$(cat $SCRIPT_DIR/logs/web-dashboard.pid)"
    if [ -f "$SCRIPT_DIR/discord-bot/.env" ]; then
        echo "   kill \$(cat $SCRIPT_DIR/logs/discord-bot.pid)"
    fi
    echo ""
    echo -e "${YELLOW}To view logs:${NC}"
    echo "   tail -f $SCRIPT_DIR/logs/web-dashboard.log"
    if [ -f "$SCRIPT_DIR/discord-bot/.env" ]; then
        echo "   tail -f $SCRIPT_DIR/logs/discord-bot.log"
    fi
}

# Print final instructions
print_final_instructions() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}                      Setup Complete!                          ${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    
    success "StaffSystem has been configured!"
    echo ""
    
    echo -e "${YELLOW}Quick Reference:${NC}"
    echo ""
    
    echo "Web Dashboard:"
    echo "   URL: $WEBSITE_URL"
    echo "   Default login: admin / admin123"
    echo -e "   ${RED}IMPORTANT: Change the default password immediately!${NC}"
    echo ""
    
    if [ -f "$SCRIPT_DIR/discord-bot/.env" ]; then
        echo "Discord Bot:"
        echo "   Configured and ready to start"
        echo ""
    fi
    
    echo "Minecraft Plugin:"
    if [ "$MAVEN_AVAILABLE" = true ] && [ -f "$SCRIPT_DIR/minecraft-plugin/target/StaffSystem-1.0.0.jar" ]; then
        echo "   JAR: minecraft-plugin/target/StaffSystem-1.0.0.jar"
    else
        echo "   Build: cd minecraft-plugin && mvn clean package"
    fi
    echo "   Config snippet: minecraft-plugin/generated/config-snippet.yml"
    echo ""
    
    echo -e "${YELLOW}Important Configuration Values:${NC}"
    echo "   API Key: $API_KEY"
    echo "   API URL: $WEB_API_URL"
    echo "   Website URL: $WEBSITE_URL"
    echo ""
    
    echo -e "${YELLOW}Manual Start Commands:${NC}"
    echo "   Web Dashboard: cd web-dashboard && npm start"
    echo "   Discord Bot:   cd discord-bot && npm start"
    echo "   Run setup.sh again and select 'start services' to auto-start"
    echo ""
    
    echo -e "${GREEN}For more information, see README.md${NC}"
}

# Main setup flow
main() {
    print_banner
    
    # Check prerequisites
    check_prerequisites
    
    echo ""
    info "This script will help you set up all StaffSystem components."
    info "Press Ctrl+C at any time to cancel."
    echo ""
    
    # Configuration
    if prompt_yes_no "Configure Web Dashboard?" "y"; then
        configure_web_dashboard
    else
        # Set defaults for other components
        WEB_API_URL="http://localhost:3000/api"
        API_KEY=$(generate_secret 32)
        WEBSITE_URL="http://localhost:3000"
    fi
    
    if prompt_yes_no "Configure Discord Bot?" "y"; then
        configure_discord_bot
    fi
    
    if prompt_yes_no "Generate Minecraft Plugin configuration snippet?" "y"; then
        configure_minecraft_plugin
    fi
    
    # Build and install
    build_minecraft_plugin
    install_npm_dependencies
    
    # Print summary
    print_final_instructions
    
    # Start services
    start_services
}

# Run main
main "$@"
