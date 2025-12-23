const LocalStrategy = require('passport-local').Strategy;
const bcrypt = require('bcryptjs');
const db = require('./database');

module.exports = function(passport) {
    passport.use(new LocalStrategy(
        { usernameField: 'username' },
        (username, password, done) => {
            try {
                const user = db.queryOne(
                    'SELECT * FROM staff_users WHERE username = ? AND is_active = 1',
                    [username]
                );

                if (!user) {
                    return done(null, false, { message: 'Invalid username or password' });
                }

                const isMatch = bcrypt.compareSync(password, user.password);
                if (!isMatch) {
                    return done(null, false, { message: 'Invalid username or password' });
                }

                // Update last login
                db.query(
                    'UPDATE staff_users SET last_login = datetime("now") WHERE id = ?',
                    [user.id]
                );

                return done(null, user);
            } catch (err) {
                return done(err);
            }
        }
    ));

    passport.serializeUser((user, done) => {
        done(null, user.id);
    });

    passport.deserializeUser((id, done) => {
        try {
            const user = db.queryOne(
                `SELECT su.*, st.name as tier_name, st.color as tier_color, st.permissions as tier_permissions
                 FROM staff_users su
                 LEFT JOIN staff_tiers st ON su.tier = st.tier_level
                 WHERE su.id = ?`,
                [id]
            );
            done(null, user);
        } catch (err) {
            done(err, null);
        }
    });
};
