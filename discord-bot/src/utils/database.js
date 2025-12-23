const mysql = require('mysql2/promise');

let pool;

const initialize = async () => {
    try {
        pool = mysql.createPool({
            host: process.env.DB_HOST || 'localhost',
            port: process.env.DB_PORT || 3306,
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || '',
            database: process.env.DB_NAME || 'staffsystem',
            waitForConnections: true,
            connectionLimit: 5,
            queueLimit: 0
        });

        // Test connection
        await pool.getConnection();
        console.log('Database connected successfully');
    } catch (error) {
        console.error('Database connection failed:', error);
        throw error;
    }
};

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
    query,
    queryOne
};
