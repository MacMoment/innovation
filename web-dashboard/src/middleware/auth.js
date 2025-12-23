const ensureAuthenticated = (req, res, next) => {
    if (req.isAuthenticated()) {
        return next();
    }
    req.flash('error_msg', 'Please log in to access this page');
    res.redirect('/auth/login');
};

const ensureGuest = (req, res, next) => {
    if (!req.isAuthenticated()) {
        return next();
    }
    res.redirect('/dashboard');
};

const ensureTier = (minTier) => {
    return (req, res, next) => {
        if (!req.isAuthenticated()) {
            req.flash('error_msg', 'Please log in to access this page');
            return res.redirect('/auth/login');
        }
        
        if (req.user.tier >= minTier) {
            return next();
        }
        
        req.flash('error_msg', 'You do not have permission to access this page');
        res.redirect('/dashboard');
    };
};

const ensurePermission = (permission) => {
    return (req, res, next) => {
        if (!req.isAuthenticated()) {
            req.flash('error_msg', 'Please log in to access this page');
            return res.redirect('/auth/login');
        }

        // Parse permissions
        let userPerms = {};
        let tierPerms = {};
        
        try {
            userPerms = typeof req.user.permissions === 'string' 
                ? JSON.parse(req.user.permissions) 
                : req.user.permissions || {};
            tierPerms = typeof req.user.tier_permissions === 'string'
                ? JSON.parse(req.user.tier_permissions)
                : req.user.tier_permissions || {};
        } catch (e) {
            // Invalid JSON, use empty object
        }

        // Check for 'all' permission or specific permission
        if (userPerms.all || userPerms[permission] || tierPerms[permission]) {
            return next();
        }

        req.flash('error_msg', 'You do not have permission to perform this action');
        res.redirect('/dashboard');
    };
};

const apiAuth = (req, res, next) => {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'No token provided' });
    }
    
    const token = authHeader.split(' ')[1];
    
    if (token !== process.env.API_KEY) {
        return res.status(401).json({ error: 'Invalid API key' });
    }
    
    next();
};

module.exports = {
    ensureAuthenticated,
    ensureGuest,
    ensureTier,
    ensurePermission,
    apiAuth
};
