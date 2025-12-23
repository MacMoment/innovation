require('dotenv').config();

const express = require('express');
const session = require('express-session');
const passport = require('passport');
const flash = require('connect-flash');
const path = require('path');
const helmet = require('helmet');
const cookieParser = require('cookie-parser');
const rateLimit = require('express-rate-limit');
const crypto = require('crypto');

const db = require('./utils/database');
const passportConfig = require('./utils/passport');

// Import routes
const authRoutes = require('./routes/auth');
const dashboardRoutes = require('./routes/dashboard');
const staffRoutes = require('./routes/staff');
const punishmentsRoutes = require('./routes/punishments');
const apiRoutes = require('./routes/api');
const webhookRoutes = require('./routes/webhook');

const app = express();

// Disable trust proxy to prevent Express from using X-Forwarded-* headers
// This ensures HTTP connections are not incorrectly detected as HTTPS
// Set to false to explicitly not trust any proxy headers when running HTTP-only
app.set('trust proxy', process.env.FORCE_HTTPS === 'true' ? 1 : false);

// Security middleware
// Configure based on FORCE_HTTPS setting
const isHttpsEnabled = process.env.FORCE_HTTPS === 'true';
const cspDirectives = {
    defaultSrc: ["'self'"],
    baseUri: ["'self'"],
    fontSrc: ["'self'", "https://fonts.gstatic.com", "https://cdn.jsdelivr.net"],
    formAction: ["'self'"],
    frameAncestors: ["'self'"],
    imgSrc: ["'self'", "data:", "https://crafatar.com", "https://mc-heads.net"],
    objectSrc: ["'none'"],
    scriptSrc: ["'self'", "'unsafe-inline'", "https://cdn.jsdelivr.net"],
    scriptSrcAttr: ["'none'"],
    styleSrc: ["'self'", "'unsafe-inline'", "https://cdn.jsdelivr.net", "https://fonts.googleapis.com"],
};

// Only add upgrade-insecure-requests directive when HTTPS is enabled
// When HTTP-only, we omit it entirely to prevent browser upgrades to HTTPS
if (isHttpsEnabled) {
    cspDirectives.upgradeInsecureRequests = [];
}

app.use(helmet({
    contentSecurityPolicy: {
        useDefaults: false, // Don't use helmet's defaults, use our custom directives
        directives: cspDirectives,
    },
    // Disable HSTS to allow HTTP connections (don't force HTTPS)
    hsts: false,
}));

// Rate limiting for API endpoints
const apiLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // limit each IP to 100 requests per windowMs
    message: 'Too many requests, please try again later.'
});
app.use('/api/', apiLimiter);

// Rate limiting for authentication routes (stricter)
const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 20, // limit each IP to 20 requests per windowMs for auth
    message: 'Too many authentication attempts, please try again later.'
});
app.use('/auth/', authLimiter);

// General rate limiting for all routes
const generalLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 200, // limit each IP to 200 requests per windowMs
    message: 'Too many requests, please try again later.'
});
app.use(generalLimiter);

// Body parsing
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());

// Static files
app.use(express.static(path.join(__dirname, 'public')));

// View engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, '../views'));

// Session configuration
app.use(session({
    secret: process.env.SESSION_SECRET || 'default-secret-change-me',
    resave: false,
    saveUninitialized: false,
    proxy: process.env.FORCE_HTTPS === 'true',
    cookie: {
        // Allow HTTP by setting secure to false unless explicitly enabled via FORCE_HTTPS=true
        secure: process.env.FORCE_HTTPS === 'true',
        httpOnly: true,
        // Use 'lax' to allow cookies during same-site navigations (e.g., form submissions, redirects)
        sameSite: 'lax',
        maxAge: 24 * 60 * 60 * 1000 // 24 hours
    }
}));

// Flash messages
app.use(flash());

// Passport initialization
passportConfig(passport);
app.use(passport.initialize());
app.use(passport.session());

// CSRF protection middleware
app.use((req, res, next) => {
    // Generate CSRF token if not exists
    if (!req.session.csrfToken) {
        req.session.csrfToken = crypto.randomBytes(32).toString('hex');
    }
    res.locals.csrfToken = req.session.csrfToken;
    next();
});

// CSRF validation for state-changing requests
app.use((req, res, next) => {
    // Skip CSRF check for API routes (they use API keys) and GET/HEAD/OPTIONS requests
    if (req.path.startsWith('/api/') || req.path.startsWith('/webhook/') || 
        ['GET', 'HEAD', 'OPTIONS'].includes(req.method)) {
        return next();
    }
    
    const csrfToken = req.body._csrf || req.headers['x-csrf-token'];
    if (!csrfToken || csrfToken !== req.session.csrfToken) {
        req.flash('error_msg', 'Invalid or expired form submission. Please try again.');
        return res.redirect('back');
    }
    next();
});

// Global variables for templates
app.use((req, res, next) => {
    res.locals.user = req.user || null;
    res.locals.success_msg = req.flash('success_msg');
    res.locals.error_msg = req.flash('error_msg');
    res.locals.error = req.flash('error');
    next();
});

// Routes
app.use('/auth', authRoutes);
app.use('/dashboard', dashboardRoutes);
app.use('/staff', staffRoutes);
app.use('/punishments', punishmentsRoutes);
app.use('/api', apiRoutes);
app.use('/webhook', webhookRoutes);

// Home page redirect
app.get('/', (req, res) => {
    if (req.isAuthenticated()) {
        res.redirect('/dashboard');
    } else {
        res.redirect('/auth/login');
    }
});

// 404 handler
app.use((req, res) => {
    res.status(404).render('pages/404', { title: 'Page Not Found' });
});

// Error handler
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).render('pages/error', { 
        title: 'Error',
        message: process.env.NODE_ENV === 'development' ? err.message : 'Something went wrong!'
    });
});

// Initialize database and start server
const PORT = process.env.PORT || 3000;

db.initialize().then(() => {
    app.listen(PORT, () => {
        console.log(`StaffSystem Dashboard running on port ${PORT}`);
        console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
    });
}).catch(err => {
    console.error('Failed to initialize database:', err);
    process.exit(1);
});

module.exports = app;
