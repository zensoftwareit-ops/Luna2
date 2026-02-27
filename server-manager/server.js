#!/usr/bin/env node

/**
 * Luna2 Server Manager v2.0
 * Gestione istanze multi-tenant con autenticazione, SSL e pannello completo
 */

const express = require('express');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { exec } = require('child_process');
const { promisify } = require('util');
const cors = require('cors');
const bodyParser = require('body-parser');
const cookieParser = require('cookie-parser');
const multer = require('multer');

const execAsync = promisify(exec);
const readFileAsync = promisify(fs.readFile);
const writeFileAsync = promisify(fs.writeFile);

const app = express();
const PORT            = process.env.PORT            || 8888;
const WORKSPACE       = process.env.WORKSPACE_DIR   || '/workspaces/Luna2';
const DOMAINS_CONFIG  = path.join(WORKSPACE, 'domains-config.txt');
const SCRIPT_PATH     = path.join(WORKSPACE, 'manage-domains-multitenant.sh');
const LETSENCRYPT_DIR = process.env.LETSENCRYPT_DIR || '/etc/letsencrypt/live';
const ADMIN_USER      = process.env.ADMIN_USER      || 'admin';
const ADMIN_PASS      = process.env.ADMIN_PASS      || 'Luna2Admin!';
const TOKEN_TTL_MS    = 24 * 60 * 60 * 1000;

// ──────────────────────────────────────────────────────────────
// AUTH  –  in-memory token store
// ──────────────────────────────────────────────────────────────
const sessionStore = new Map();

function generateToken()  { return crypto.randomBytes(32).toString('hex'); }

function createSession(user) {
    const token = generateToken();
    sessionStore.set(token, { user, expiresAt: Date.now() + TOKEN_TTL_MS });
    return token;
}

function validateToken(token) {
    if (!token) return null;
    const s = sessionStore.get(token);
    if (!s) return null;
    if (Date.now() > s.expiresAt) { sessionStore.delete(token); return null; }
    return s;
}

function authMw(req, res, next) {
    const h = req.headers['authorization'] || '';
    const token = h.startsWith('Bearer ') ? h.slice(7) : null;
    const s = validateToken(token);
    if (!s) return res.status(401).json({ success: false, error: 'Non autenticato' });
    req.user = s.user;
    next();
}

// ──────────────────────────────────────────────────────────────
// MULTER  –  upload certificati in memoria
// ──────────────────────────────────────────────────────────────
const certUpload = multer({
    storage: multer.memoryStorage(),
    limits: { fileSize: 1024 * 1024 }
});

// ──────────────────────────────────────────────────────────────
// MIDDLEWARE globale
// ──────────────────────────────────────────────────────────────
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

// ──────────────────────────────────────────────────────────────
// UTILITY
// ──────────────────────────────────────────────────────────────
const log = (level, msg) => console.log(`[${new Date().toISOString()}] [${level}] ${msg}`);

const exec$ = async (cmd) => {
    try {
        const { stdout, stderr } = await execAsync(cmd, { timeout: 120000 });
        return { success: true, stdout, stderr };
    } catch (e) {
        return { success: false, error: e.message, stdout: e.stdout || '', stderr: e.stderr || '' };
    }
};

const parseDomainConfig = async () => {
    if (!fs.existsSync(DOMAINS_CONFIG)) return [];
    const content = await readFileAsync(DOMAINS_CONFIG, 'utf8');
    return content.split('\n')
        .filter(l => l.trim() && !l.startsWith('#'))
        .map(l => { const p = l.split('|');
            return p.length >= 5 ? {
                domain: p[0], customer: p[1], mysqlPort: p[2],
                appPort: p[3], phpadminPort: p[4],
                created: p[5] || 'N/D', status: p[6] || 'active'
            } : null; })
        .filter(Boolean);
};

const containerStatus = async (name) => {
    const r = await exec$(`docker inspect ${name} --format='{{.State.Status}}' 2>/dev/null`);
    return r.success ? r.stdout.trim().replace(/'/g,'') : 'absent';
};

const getSSLInfo = async (domain) => {
    const certPath = path.join(LETSENCRYPT_DIR, domain, 'fullchain.pem');
    if (!fs.existsSync(certPath)) return { exists: false };
    const r = await exec$(`openssl x509 -enddate -startdate -issuer -noout -in "${certPath}" 2>/dev/null`);
    if (!r.success) return { exists: true, error: 'Impossibile leggere il certificato' };
    const lines = r.stdout;
    const expiry    = (lines.match(/notAfter=(.+)/)  || [])[1] || null;
    const start     = (lines.match(/notBefore=(.+)/) || [])[1] || null;
    const issuer    = (lines.match(/issuer=(.+)/)    || [])[1] || null;
    const expiryDate = expiry ? new Date(expiry) : null;
    const daysLeft  = expiryDate ? Math.ceil((expiryDate - Date.now()) / 86400000) : null;
    const isLE      = (issuer || '').includes("Let's Encrypt") || (issuer || '').includes('R3') || (issuer || '').includes('E5') || (issuer || '').includes('E6');
    return { exists: true, expiry, start, issuer, daysLeft, isLetsEncrypt: isLE };
};

const enrichInstances = async (domains) => {
    const out = [];
    for (const d of domains) {
        const [mysql, appC, pma] = await Promise.all([
            containerStatus(`mysql-${d.domain}`),
            containerStatus(`luna2-${d.domain}`),
            containerStatus(`phpmyadmin-${d.domain}`)
        ]);
        const ssl = await getSSLInfo(d.domain);
        out.push({
            ...d,
            containers: {
                mysql:      { name: `mysql-${d.domain}`,      status: mysql },
                app:        { name: `luna2-${d.domain}`,      status: appC  },
                phpmyadmin: { name: `phpmyadmin-${d.domain}`, status: pma   }
            },
            ssl,
            enabled: appC === 'running'
        });
    }
    return out;
};

// ──────────────────────────────────────────────────────────────
// AUTH  ROUTES  (pubbliche)
// ──────────────────────────────────────────────────────────────

app.post('/api/auth/login', (req, res) => {
    const { username, password } = req.body;
    if (username === ADMIN_USER && password === ADMIN_PASS) {
        const token = createSession(username);
        log('AUTH', `Login OK: ${username}`);
        return res.json({ success: true, token });
    }
    log('AUTH', `Login FAILED: ${username}`);
    res.status(401).json({ success: false, error: 'Credenziali errate' });
});

app.post('/api/auth/logout', (req, res) => {
    const token = (req.headers['authorization'] || '').replace('Bearer ','');
    sessionStore.delete(token);
    res.json({ success: true });
});

app.get('/api/auth/check', (req, res) => {
    const token = (req.headers['authorization'] || '').replace('Bearer ','');
    const s = validateToken(token);
    res.json({ authenticated: !!s, user: s?.user || null });
});

app.get('/api/health', (req, res) => {
    res.json({ status: 'OK', timestamp: new Date().toISOString(), version: '2.0' });
});

// ──────────────────────────────────────────────────────────────
// TUTTE LE ROTTE SEGUENTI  →  richiedono autenticazione
// ──────────────────────────────────────────────────────────────
app.use('/api', authMw);

// ──────────────────────────────────────────────────────────────
// INSTANCES
// ──────────────────────────────────────────────────────────────

app.get('/api/instances', async (req, res) => {
    try {
        const domains = await parseDomainConfig();
        const instances = await enrichInstances(domains);
        res.json({ success: true, count: instances.length, instances });
    } catch (e) { res.status(500).json({ success: false, error: e.message }); }
});

app.get('/api/instances/:domain', async (req, res) => {
    try {
        const domains = await parseDomainConfig();
        const d = domains.find(x => x.domain === req.params.domain);
        if (!d) return res.status(404).json({ success: false, error: 'Istanza non trovata' });
        const [inst] = await enrichInstances([d]);
        res.json({ success: true, instance: inst });
    } catch (e) { res.status(500).json({ success: false, error: e.message }); }
});

app.post('/api/instances', async (req, res) => {
    const { domain, customer_name } = req.body;
    if (!domain || !customer_name)
        return res.status(400).json({ success: false, error: 'Campi domain e customer_name obbligatori' });
    log('API', `Crea istanza: ${domain} / ${customer_name}`);
    const r = await exec$(`cd "${WORKSPACE}" && bash "${SCRIPT_PATH}" add "${domain}" "${customer_name}"`);
    if (!r.success) return res.status(400).json({ success: false, error: r.error, output: r.stderr });
    res.json({ success: true, message: `Istanza ${domain} creata`, output: r.stdout });
});

app.post('/api/instances/:domain/enable', async (req, res) => {
    const { domain } = req.params;
    log('API', `Abilita: ${domain}`);
    for (const c of [`mysql-${domain}`, `luna2-${domain}`, `phpmyadmin-${domain}`])
        await exec$(`docker start ${c} 2>/dev/null || true`);
    res.json({ success: true, message: `Istanza ${domain} abilitata` });
});

app.post('/api/instances/:domain/disable', async (req, res) => {
    const { domain } = req.params;
    log('API', `Disabilita: ${domain}`);
    for (const c of [`luna2-${domain}`, `phpmyadmin-${domain}`, `mysql-${domain}`])
        await exec$(`docker stop ${c} 2>/dev/null || true`);
    res.json({ success: true, message: `Istanza ${domain} disabilitata` });
});

app.delete('/api/instances/:domain', async (req, res) => {
    const { domain } = req.params;
    log('API', `Rimuovi istanza: ${domain}`);
    for (const c of [`luna2-${domain}`, `phpmyadmin-${domain}`, `mysql-${domain}`])
        await exec$(`docker stop ${c} 2>/dev/null || true && docker rm ${c} 2>/dev/null || true`);
    const r = await exec$(`cd "${WORKSPACE}" && echo "y" | bash "${SCRIPT_PATH}" remove "${domain}" 2>&1 || true`);
    res.json({ success: true, message: `Istanza ${domain} rimossa`, output: r.stdout });
});

// ──────────────────────────────────────────────────────────────
// CONTAINERS
// ──────────────────────────────────────────────────────────────

app.get('/api/instances/:domain/containers', async (req, res) => {
    const { domain } = req.params;
    const [mysql, app, pma] = await Promise.all([
        containerStatus(`mysql-${domain}`),
        containerStatus(`luna2-${domain}`),
        containerStatus(`phpmyadmin-${domain}`)
    ]);
    res.json({ success: true, containers: { mysql, app, phpmyadmin: pma } });
});

app.post('/api/instances/:domain/containers/:container/restart', async (req, res) => {
    const { domain, container } = req.params;
    const name = `${container}-${domain}`;
    const r = await exec$(`docker restart ${name}`);
    if (!r.success) return res.status(400).json({ success: false, error: r.error });
    await new Promise(resolve => setTimeout(resolve, 2000));
    res.json({ success: true, container: name, status: await containerStatus(name) });
});

// ──────────────────────────────────────────────────────────────
// SSL
// ──────────────────────────────────────────────────────────────

app.get('/api/instances/:domain/ssl', async (req, res) => {
    res.json({ success: true, ssl: await getSSLInfo(req.params.domain) });
});

// Rinnovo Let's Encrypt
app.post('/api/instances/:domain/ssl/renew', async (req, res) => {
    const { domain } = req.params;
    log('API', `Rinnovo LE: ${domain}`);
    const r = await exec$(`cd "${WORKSPACE}" && bash "${SCRIPT_PATH}" renew "${domain}"`);
    if (!r.success) return res.status(400).json({ success: false, error: r.error, output: r.stderr });
    res.json({ success: true, message: `Certificato rinnovato per ${domain}`, output: r.stdout });
});

// Upload certificato custom (cert = fullchain/cert PEM, key = private key PEM)
app.post('/api/instances/:domain/ssl/custom',
    certUpload.fields([{ name: 'cert', maxCount: 1 }, { name: 'key', maxCount: 1 }]),
    async (req, res) => {
        const { domain } = req.params;
        const certFile = req.files?.cert?.[0];
        const keyFile  = req.files?.key?.[0];

        if (!certFile || !keyFile)
            return res.status(400).json({ success: false, error: 'Richiesti: file "cert" e file "key"' });

        const certPem = certFile.buffer.toString('utf8');
        const keyPem  = keyFile.buffer.toString('utf8');

        if (!certPem.includes('-----BEGIN CERTIFICATE-----'))
            return res.status(400).json({ success: false, error: 'Il file "cert" non è un certificato PEM valido' });
        if (!keyPem.includes('PRIVATE KEY-----'))
            return res.status(400).json({ success: false, error: 'Il file "key" non è una chiave privata PEM valida' });

        const destDir = path.join(LETSENCRYPT_DIR, domain);
        try {
            if (!fs.existsSync(destDir)) fs.mkdirSync(destDir, { recursive: true });
            await writeFileAsync(path.join(destDir, 'fullchain.pem'), certPem);
            await writeFileAsync(path.join(destDir, 'privkey.pem'),  keyPem);
            fs.chmodSync(path.join(destDir, 'privkey.pem'), 0o600);
        } catch (e) {
            return res.status(500).json({ success: false, error: `Errore salvataggio: ${e.message}` });
        }

        await exec$(`docker exec nginx nginx -s reload 2>/dev/null || sudo nginx -s reload 2>/dev/null || true`);
        log('API', `Certificato custom installato per ${domain}`);
        res.json({ success: true, message: `Certificato custom installato per ${domain}` });
    }
);

// ──────────────────────────────────────────────────────────────
// LOGS
// ──────────────────────────────────────────────────────────────

app.get('/api/instances/:domain/logs/:container', async (req, res) => {
    const { domain, container } = req.params;
    const lines = Math.min(parseInt(req.query.lines) || 200, 1000);
    const name = container.includes('-') ? container : `${container}-${domain}`;
    const r = await exec$(`docker logs --tail ${lines} ${name} 2>&1`);
    res.json({ success: true, container: name, logs: r.stdout + r.stderr });
});

// ──────────────────────────────────────────────────────────────
// SYSTEM
// ──────────────────────────────────────────────────────────────

app.get('/api/system/status', async (req, res) => {
    try {
        const [domains, diskR, memR, dockerR] = await Promise.all([
            parseDomainConfig(),
            exec$("df -h / | awk 'NR==2{print $3\"/\"$2\" (\"$5\")\"}'"),
            exec$("free -h | awk '/^Mem/{print $7\" libera su \"$2}'"),
            exec$("docker ps --format '{{.Names}}' | wc -l")
        ]);
        res.json({
            success: true,
            timestamp: new Date().toISOString(),
            instances:          domains.length,
            disk:               diskR.success  ? diskR.stdout.trim()  : '--',
            memory:             memR.success   ? memR.stdout.trim()   : '--',
            containersRunning:  dockerR.success ? parseInt(dockerR.stdout.trim()) : 0
        });
    } catch (e) { res.status(500).json({ success: false, error: e.message }); }
});

app.post('/api/system/nginx/reload', async (req, res) => {
    const r = await exec$(`docker exec nginx nginx -s reload 2>/dev/null || sudo nginx -s reload 2>/dev/null`);
    res.json({ success: r.success, message: r.success ? 'Nginx ricaricato' : r.error });
});

app.post('/api/system/nginx/test', async (req, res) => {
    const r = await exec$(`docker exec nginx nginx -t 2>&1 || sudo nginx -t 2>&1`);
    res.json({ success: r.success, output: r.stdout + r.stderr });
});

// ──────────────────────────────────────────────────────────────
// UPDATE & DEPLOY
// ──────────────────────────────────────────────────────────────

// POST /api/system/update - Pull da GitHub + build nuovo WAR
app.post('/api/system/update', async (req, res) => {
    log('API', 'UPDATE: Pull da GitHub + rebuild WAR');
    
    const steps = [];
    
    // 1. Git pull
    steps.push({ step: 'git-pull', status: 'running' });
    const pullR = await exec$(`cd "${WORKSPACE}" && git pull origin main`);
    if (!pullR.success) {
        steps[0].status = 'failed';
        steps[0].error = pullR.error;
        return res.status(400).json({ success: false, steps, error: 'Git pull fallito' });
    }
    steps[0].status = 'success';
    steps[0].output = pullR.stdout.trim();
    
    // 2. Maven build
    steps.push({ step: 'maven-build', status: 'running' });
    const buildR = await exec$(`cd "${WORKSPACE}" && mvn clean package -DskipTests`);
    if (!buildR.success) {
        steps[1].status = 'failed';
        steps[1].error = buildR.error;
        return res.status(400).json({ success: false, steps, error: 'Maven build fallito' });
    }
    steps[1].status = 'success';
    steps[1].output = 'WAR generato con successo';
    
    const warPath = path.join(WORKSPACE, 'target/luna2.war');
    const warExists = fs.existsSync(warPath);
    
    res.json({
        success: true,
        message: 'Update completato. Ora puoi fare deploy alle istanze.',
        steps,
        warPath: warExists ? warPath : null
    });
});

// POST /api/instances/:domain/deploy - Deploy update a singola istanza
app.post('/api/instances/:domain/deploy', async (req, res) => {
    const { domain } = req.params;
    log('API', `DEPLOY update a istanza: ${domain}`);
    
    const warPath = path.join(WORKSPACE, 'target/luna2.war');
    if (!fs.existsSync(warPath)) {
        return res.status(400).json({
            success: false,
            error: 'WAR non trovato. Esegui prima /api/system/update'
        });
    }
    
    const steps = [];
    
    // 1. Applica migrazioni DB (se esistono)
    const migrationDir = path.join(WORKSPACE, 'database/migrations');
    if (fs.existsSync(migrationDir)) {
        steps.push({ step: 'db-migrations', status: 'running' });
        
        // Leggi tutte le migrazioni SQL
        const files = fs.readdirSync(migrationDir)
            .filter(f => f.endsWith('.sql'))
            .sort();
        
        for (const sqlFile of files) {
            const sqlPath = path.join(migrationDir, sqlFile);
            const sqlContent = fs.readFileSync(sqlPath, 'utf8');
            
            // Esegui la migrazione
            const domains = await parseDomainConfig();
            const inst = domains.find(d => d.domain === domain);
            if (inst) {
                const mysqlCmd = `docker exec mysql-${domain} mysql -uluna2 -pluna2pass luna2 -e "${sqlContent.replace(/"/g, '\\"')}" 2>&1 || true`;
                const migR = await exec$(mysqlCmd);
                steps[0].output = (steps[0].output || '') + `\n${sqlFile}: ${migR.success ? 'OK' : 'WARN'}`;
            }
        }
        steps[0].status = 'success';
    }
    
    // 2. Copia nuovo WAR nel container
    steps.push({ step: 'copy-war', status: 'running' });
    const cpR = await exec$(`docker cp "${warPath}" luna2-${domain}:/usr/local/tomcat/webapps/luna2.war`);
    if (!cpR.success) {
        steps[steps.length - 1].status = 'failed';
        steps[steps.length - 1].error = cpR.error;
        return res.status(400).json({ success: false, steps, error: 'Copia WAR fallita' });
    }
    steps[steps.length - 1].status = 'success';
    
    // 3. Riavvia container app
    steps.push({ step: 'restart-app', status: 'running' });
    const restartR = await exec$(`docker restart luna2-${domain}`);
    if (!restartR.success) {
        steps[steps.length - 1].status = 'failed';
        return res.status(400).json({ success: false, steps, error: 'Riavvio fallito' });
    }
    steps[steps.length - 1].status = 'success';
    
    res.json({
        success: true,
        message: `Deploy completato per ${domain}`,
        steps
    });
});

// POST /api/system/deploy-all - Deploy a tutte le istanze attive
app.post('/api/system/deploy-all', async (req, res) => {
    log('API', 'DEPLOY-ALL: aggiornamento a tutte le istanze');
    
    const warPath = path.join(WORKSPACE, 'target/luna2.war');
    if (!fs.existsSync(warPath)) {
        return res.status(400).json({
            success: false,
            error: 'WAR non trovato. Esegui prima /api/system/update'
        });
    }
    
    const domains = await parseDomainConfig();
    const results = [];
    
    for (const d of domains) {
        const r = { domain: d.domain, steps: [] };
        
        // 1. Copia WAR
        const cpR = await exec$(`docker cp "${warPath}" luna2-${d.domain}:/usr/local/tomcat/webapps/luna2.war 2>&1`);
        r.steps.push({ step: 'copy-war', success: cpR.success, output: cpR.stdout.trim() || cpR.stderr.trim() });
        
        // 2. Riavvia
        if (cpR.success) {
            const restartR = await exec$(`docker restart luna2-${d.domain}`);
            r.steps.push({ step: 'restart', success: restartR.success, output: restartR.stdout.trim() });
        }
        
        r.success = r.steps.every(s => s.success);
        results.push(r);
    }
    
    const allSuccess = results.every(r => r.success);
    res.json({
        success: allSuccess,
        message: allSuccess ? 'Deploy completato su tutte le istanze' : 'Alcuni deploy sono falliti',
        total: results.length,
        succeeded: results.filter(r => r.success).length,
        failed: results.filter(r => !r.success).length,
        results
    });
});

// ──────────────────────────────────────────────────────────────
// CATCH-ALL
// ──────────────────────────────────────────────────────────────
app.get('*', (req, res) => {
    if (req.path.startsWith('/api')) return res.status(404).json({ error: 'Not found' });
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.use((err, req, res, next) => {
    log('ERROR', err.message);
    res.status(500).json({ success: false, error: err.message });
});

app.listen(PORT, () => {
    log('INFO', `Luna2 Server Manager v2.0 – porta ${PORT}`);
    log('INFO', `UI: http://localhost:${PORT}`);
    log('INFO', `Credenziali: ${ADMIN_USER} / [ADMIN_PASS env var]`);
});
