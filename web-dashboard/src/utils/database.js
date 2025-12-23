const Database = require('better-sqlite3');
const bcrypt = require('bcryptjs');
const path = require('path');
const fs = require('fs');

let db;

const initialize = async () => {
    // Ensure data directory exists
    const dbPath = process.env.DB_PATH || './data/staffsystem.db';
    const dbDir = path.dirname(dbPath);
    
    if (!fs.existsSync(dbDir)) {
        fs.mkdirSync(dbDir, { recursive: true });
    }

    // Create database connection
    db = new Database(dbPath);
    db.pragma('journal_mode = WAL'); // Better performance

    // Create tables
    createTables();
    
    // Create default admin if not exists
    createDefaultAdmin();
    
    console.log('SQLite database initialized successfully at:', dbPath);
};

const createTables = () => {
    // Staff users table
    db.exec(`
        CREATE TABLE IF NOT EXISTS staff_users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE NOT NULL,
            email TEXT UNIQUE NOT NULL,
            password TEXT NOT NULL,
            minecraft_uuid TEXT,
            minecraft_name TEXT,
            discord_id TEXT,
            tier INTEGER DEFAULT 1,
            role TEXT DEFAULT 'staff',
            permissions TEXT,
            is_active INTEGER DEFAULT 1,
            last_login TEXT,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
    `);

    // Punishments table (synced with Minecraft plugin)
    db.exec(`
        CREATE TABLE IF NOT EXISTS punishments (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid TEXT NOT NULL,
            player_name TEXT NOT NULL,
            staff_uuid TEXT NOT NULL,
            staff_name TEXT NOT NULL,
            type TEXT NOT NULL,
            reason TEXT,
            timestamp INTEGER NOT NULL,
            duration INTEGER NOT NULL,
            expiration INTEGER NOT NULL,
            active INTEGER DEFAULT 1,
            server TEXT DEFAULT 'main'
        )
    `);

    // Create indexes
    db.exec(`CREATE INDEX IF NOT EXISTS idx_player_uuid ON punishments(player_uuid)`);
    db.exec(`CREATE INDEX IF NOT EXISTS idx_active ON punishments(active)`);
    db.exec(`CREATE INDEX IF NOT EXISTS idx_type ON punishments(type)`);

    // Activity log table
    db.exec(`
        CREATE TABLE IF NOT EXISTS activity_log (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            staff_user_id INTEGER,
            action TEXT NOT NULL,
            details TEXT,
            ip_address TEXT,
            timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (staff_user_id) REFERENCES staff_users(id) ON DELETE SET NULL
        )
    `);

    // Staff tiers configuration
    db.exec(`
        CREATE TABLE IF NOT EXISTS staff_tiers (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            tier_level INTEGER UNIQUE NOT NULL,
            name TEXT NOT NULL,
            color TEXT DEFAULT '#FFFFFF',
            permissions TEXT,
            discord_role_id TEXT,
            created_at TEXT DEFAULT CURRENT_TIMESTAMP
        )
    `);

    // Insert default tiers if not exist
    const insertTier = db.prepare(`
        INSERT OR IGNORE INTO staff_tiers (tier_level, name, color, permissions, discord_role_id) 
        VALUES (?, ?, ?, ?, ?)
    `);
    
    insertTier.run(1, 'Trial Staff', '#90EE90', '{"ban": false, "mute": true, "kick": true, "warn": true, "freeze": true}', null);
    insertTier.run(2, 'Moderator', '#00BFFF', '{"ban": true, "mute": true, "kick": true, "warn": true, "freeze": true}', null);
    insertTier.run(3, 'Senior Moderator', '#9370DB', '{"ban": true, "mute": true, "kick": true, "warn": true, "freeze": true, "manage_staff": false}', null);
    insertTier.run(4, 'Admin', '#FF6347', '{"ban": true, "mute": true, "kick": true, "warn": true, "freeze": true, "manage_staff": true}', null);
    insertTier.run(5, 'Owner', '#FFD700', '{"ban": true, "mute": true, "kick": true, "warn": true, "freeze": true, "manage_staff": true, "admin": true}', null);

    console.log('Database tables created successfully');
};

const createDefaultAdmin = () => {
    const existing = db.prepare('SELECT id FROM staff_users WHERE username = ?').get('admin');

    if (!existing) {
        const hashedPassword = bcrypt.hashSync('admin123', 10);
        db.prepare(`
            INSERT INTO staff_users (username, email, password, tier, role, is_active, permissions)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        `).run('admin', 'admin@example.com', hashedPassword, 5, 'owner', 1, JSON.stringify({ all: true }));
        
        console.log('Default admin account created (username: admin, password: admin123)');
        console.log('IMPORTANT: Change the default password immediately!');
    }
};

const getDb = () => db;

const query = (sql, params = []) => {
    const stmt = db.prepare(sql);
    if (sql.trim().toUpperCase().startsWith('SELECT')) {
        return stmt.all(...params);
    } else {
        return stmt.run(...params);
    }
};

const queryOne = (sql, params = []) => {
    const stmt = db.prepare(sql);
    return stmt.get(...params) || null;
};

module.exports = {
    initialize,
    getDb,
    query,
    queryOne
};
