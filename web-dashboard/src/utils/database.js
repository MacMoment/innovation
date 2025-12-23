const mysql = require('mysql2/promise');
const bcrypt = require('bcryptjs');

let pool;

const initialize = async () => {
    pool = mysql.createPool({
        host: process.env.DB_HOST || 'localhost',
        port: process.env.DB_PORT || 3306,
        user: process.env.DB_USER || 'root',
        password: process.env.DB_PASSWORD || '',
        database: process.env.DB_NAME || 'staffsystem',
        waitForConnections: true,
        connectionLimit: 10,
        queueLimit: 0
    });

    // Create tables if they don't exist
    await createTables();
    
    // Create default admin if not exists
    await createDefaultAdmin();
    
    console.log('Database initialized successfully');
};

const createTables = async () => {
    const connection = await pool.getConnection();
    
    try {
        // Staff users table
        await connection.execute(`
            CREATE TABLE IF NOT EXISTS staff_users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                minecraft_uuid VARCHAR(36),
                minecraft_name VARCHAR(16),
                discord_id VARCHAR(20),
                tier INT DEFAULT 1,
                role VARCHAR(50) DEFAULT 'staff',
                permissions JSON,
                is_active BOOLEAN DEFAULT TRUE,
                last_login TIMESTAMP NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_username (username),
                INDEX idx_email (email),
                INDEX idx_tier (tier)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        `);

        // Punishments table (synced with Minecraft plugin)
        await connection.execute(`
            CREATE TABLE IF NOT EXISTS punishments (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                staff_uuid VARCHAR(36) NOT NULL,
                staff_name VARCHAR(16) NOT NULL,
                type VARCHAR(20) NOT NULL,
                reason TEXT,
                timestamp BIGINT NOT NULL,
                duration BIGINT NOT NULL,
                expiration BIGINT NOT NULL,
                active BOOLEAN DEFAULT TRUE,
                server VARCHAR(50) DEFAULT 'main',
                INDEX idx_player_uuid (player_uuid),
                INDEX idx_active (active),
                INDEX idx_type (type)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        `);

        // Activity log table
        await connection.execute(`
            CREATE TABLE IF NOT EXISTS activity_log (
                id INT AUTO_INCREMENT PRIMARY KEY,
                staff_user_id INT,
                action VARCHAR(100) NOT NULL,
                details TEXT,
                ip_address VARCHAR(45),
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (staff_user_id) REFERENCES staff_users(id) ON DELETE SET NULL,
                INDEX idx_timestamp (timestamp),
                INDEX idx_staff_user (staff_user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        `);

        // Staff tiers configuration
        await connection.execute(`
            CREATE TABLE IF NOT EXISTS staff_tiers (
                id INT AUTO_INCREMENT PRIMARY KEY,
                tier_level INT UNIQUE NOT NULL,
                name VARCHAR(50) NOT NULL,
                color VARCHAR(7) DEFAULT '#FFFFFF',
                permissions JSON,
                discord_role_id VARCHAR(20),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        `);

        // Insert default tiers if not exist
        await connection.execute(`
            INSERT IGNORE INTO staff_tiers (tier_level, name, color, permissions, discord_role_id) VALUES
            (1, 'Trial Staff', '#90EE90', '{"ban": false, "mute": true, "kick": true, "warn": true, "freeze": true}', NULL),
            (2, 'Moderator', '#00BFFF', '{"ban": true, "mute": true, "kick": true, "warn": true, "freeze": true}', NULL),
            (3, 'Senior Moderator', '#9370DB', '{"ban": true, "mute": true, "kick": true, "warn": true, "freeze": true, "manage_staff": false}', NULL),
            (4, 'Admin', '#FF6347', '{"ban": true, "mute": true, "kick": true, "warn": true, "freeze": true, "manage_staff": true}', NULL),
            (5, 'Owner', '#FFD700', '{"ban": true, "mute": true, "kick": true, "warn": true, "freeze": true, "manage_staff": true, "admin": true}', NULL)
        `);

        console.log('Database tables created successfully');
    } finally {
        connection.release();
    }
};

const createDefaultAdmin = async () => {
    const connection = await pool.getConnection();
    
    try {
        const [rows] = await connection.execute(
            'SELECT id FROM staff_users WHERE username = ?',
            ['admin']
        );

        if (rows.length === 0) {
            const hashedPassword = await bcrypt.hash('admin123', 10);
            await connection.execute(`
                INSERT INTO staff_users (username, email, password, tier, role, is_active, permissions)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            `, ['admin', 'admin@example.com', hashedPassword, 5, 'owner', true, 
                JSON.stringify({ all: true })]);
            
            console.log('Default admin account created (username: admin, password: admin123)');
            console.log('IMPORTANT: Change the default password immediately!');
        }
    } finally {
        connection.release();
    }
};

const getPool = () => pool;

const query = async (sql, params = []) => {
    const [rows] = await pool.execute(sql, params);
    return rows;
};

const queryOne = async (sql, params = []) => {
    const rows = await query(sql, params);
    return rows[0] || null;
};

module.exports = {
    initialize,
    getPool,
    query,
    queryOne
};
