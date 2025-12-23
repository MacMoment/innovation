const Database = require('better-sqlite3');
const path = require('path');

let db;

const initialize = async () => {
    try {
        const dbPath = process.env.DB_PATH || '../web-dashboard/data/staffsystem.db';
        
        db = new Database(dbPath, { readonly: false });
        db.pragma('journal_mode = WAL');

        console.log('SQLite database connected successfully at:', dbPath);
    } catch (error) {
        console.error('Database connection failed:', error);
        throw error;
    }
};

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
    query,
    queryOne
};
