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
const GIT_USERNAME    = process.env.GIT_USERNAME    || 'oauth2';
const GITHUB_TOKEN    = process.env.GITHUB_TOKEN    || '';
const TOKEN_TTL_MS    = 24 * 60 * 60 * 1000;
const DB_APP_USER     = process.env.MYSQL_APP_USER  || 'luna2_user';
const DB_APP_PASS     = process.env.MYSQL_USER_PASSWORD || 'Luna2User@2024';

// ──────────────────────────────────────────────────────────────
// AUTH  –  in-memory token store
// ──────────────────────────────────────────────────────────────
const sessionStore = new Map();

// ──────────────────────────────────────────────────────────────
// TASK STORE  –  track async task status (instance creation, SSL, etc)
// ──────────────────────────────────────────────────────────────
const taskStore = new Map();

function createTask(type, domain) {
    const taskId = crypto.randomBytes(16).toString('hex');
    const task = {
        id: taskId,
        type,
        domain,
        status: 'running',  // running | completed | failed
        progress: 'Inizializzazione...',
        result: null,
        error: null,
        createdAt: Date.now(),
        expiresAt: Date.now() + (60 * 60 * 1000)  // 1 hour
    };
    taskStore.set(taskId, task);
    return taskId;
}

function getTask(taskId) {
    const task = taskStore.get(taskId);
    if (!task) return null;
    // Auto-cleanup scaduti
    if (task.expiresAt && Date.now() > task.expiresAt) {
        taskStore.delete(taskId);
        return null;
    }
    return task;
}

function updateTask(taskId, updates) {
    const task = getTask(taskId);
    if (task) Object.assign(task, updates);
    return task;
}

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

const exec$ = async (cmd, env = null, timeoutMs = 120000) => {
    try {
        const options = { timeout: timeoutMs };
        if (env) options.env = env;
        const { stdout, stderr } = await execAsync(cmd, options);
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
    
    // Crea task e ritorna subito
    const taskId = createTask('instance-create', domain);
    log('API', `Crea istanza: ${domain} / ${customer_name} (task: ${taskId})`);
    
    // Ritorna subito al client con task ID e loader
    res.json({ 
        success: true, 
        message: 'Creazione istanza avviata',
        taskId,
        statusUrl: `/api/tasks/${taskId}/status`
    });
    
    // Esegui in background (non aspettare)
    (async () => {
        try {
            updateTask(taskId, { progress: 'Compilazione WAR (questo può prendere 2-5 minuti)...' });
            
            // Delega tutto (build WAR + provisioning) allo script di gestione
            const r = await exec$(
                `cd "${WORKSPACE}" && bash "${SCRIPT_PATH}" add "${domain}" "${customer_name}"`,
                null,
                900000
            );
            
            if (!r.success) {
                updateTask(taskId, { 
                    status: 'failed',
                    error: r.error || 'Creazione fallita',
                    progress: 'Errore durante la creazione'
                });
                log('API', `Creazione istanza fallita: ${r.error}`);
                return;
            }
            
            // Recupera porte allocate dalla config
            const configFile = path.join(WORKSPACE, 'domains-config.txt');
            let instanceInfo = {};
            try {
                const configContent = fs.readFileSync(configFile, 'utf8');
                const line = configContent.split('\n').find(l => l.startsWith(domain + '|'));
                if (line) {
                    const p = line.split('|');
                    instanceInfo = {
                        domain: p[0],
                        customer: p[1],
                        mysqlPort: p[2],
                        appPort: p[3],
                        pmaPort: p[4]
                    };
                }
            } catch (e) { /* ignore */ }
            
            // Credenziali di default
            const mysqlRootPass = process.env.MYSQL_ROOT_PASSWORD || 'Luna2Root@2024';
            const mysqlUserPass = process.env.MYSQL_USER_PASSWORD || 'Luna2User@2024';
            
            const result = {
                domain,
                customer: customer_name,
                appUrl: `http://${domain}`,
                appUrlHttps: `https://${domain}`,
                pmaUrl: `http://phpmyadmin.${domain}`,
                pmaUrlDirect: instanceInfo.pmaPort ? `http://<IP-SERVER>:${instanceInfo.pmaPort}` : null,
                appPort: instanceInfo.appPort || null,
                pmaPort: instanceInfo.pmaPort || null,
                superUser: {
                    username: 'admin',
                    password: 'admin123',
                    role: 'ADMIN'
                },
                mysql: {
                    host: `mysql-${domain}`,
                    database: 'luna2',
                    rootUser: 'root',
                    rootPassword: mysqlRootPass,
                    appUser: 'luna2_user',
                    appPassword: mysqlUserPass
                }
            };
            
            updateTask(taskId, { 
                status: 'completed',
                progress: 'Istanza creata con successo!',
                result
            });
            log('API', `Istanza creata: ${domain}`);
        } catch (e) {
            updateTask(taskId, { 
                status: 'failed',
                error: e.message,
                progress: 'Errore inatteso'
            });
            log('API', `Errore creazione istanza: ${e.message}`);
        }
    })();
});

// Polling endpoint per status task
app.get('/api/tasks/:taskId/status', (req, res) => {
    const { taskId } = req.params;
    const task = getTask(taskId);
    
    if (!task) {
        return res.status(404).json({ success: false, error: 'Task non trovato o scaduto' });
    }
    
    res.json({ 
        success: true,
        taskId: task.id,
        status: task.status,
        progress: task.progress,
        result: task.result,
        error: task.error
    });
});

// ──────────────────────────────────────────────────────────────
// SERVER MAINTENANCE  –  cleanup, status, etc
// ──────────────────────────────────────────────────────────────

app.post('/api/server/cleanup', async (req, res) => {
    const { dryRun } = req.body || {};
    
    log('API', `Avvio pulizia server (dry_run: ${dryRun ? 'SÌ' : 'NO'})`);
    
    try {
        const cleanupScript = path.join(WORKSPACE, 'cleanup-server.sh');
        if (!fs.existsSync(cleanupScript)) {
            return res.status(400).json({ 
                success: false, 
                error: 'Script cleanup non trovato'
            });
        }
        
        // Esegui con OUTPUT_JSON=true per ricevere risultati strutturati
        const cmd = `OUTPUT_JSON=true bash "${cleanupScript}" ${dryRun ? '--dry-run' : ''}`;
        const r = await exec$(cmd, null, 300000);  // 5 min timeout
        
        let cleanupResults = {};
        try {
            // Estrai l'ultimo oggetto JSON dall'output
            const lines = r.stdout.split('\n').reverse();
            for (const line of lines) {
                if (line.trim().startsWith('{')) {
                    cleanupResults = JSON.parse(line);
                    break;
                }
            }
        } catch (e) {
            log('API', `JSON parse error (cleanup output non strutturato): ${e.message}`);
            cleanupResults = { 
                dry_run: dryRun,
                results: [],
                raw_output: r.stdout,
                error: 'JSON parsing fallito'
            };
        }
        
        res.json({
            success: r.success,
            dry_run: dryRun,
            cleanup: cleanupResults,
            message: r.success ? 'Pulizia completata' : 'Errore durante la pulizia'
        });
        
    } catch (e) {
        log('API', `Errore cleanup: ${e.message}`);
        res.status(500).json({ 
            success: false, 
            error: e.message,
            message: 'Errore durante l\'esecuzione della pulizia'
        });
    }
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
    const { deleteData } = req.body || {};
    
    log('API', `Elimina istanza completa: ${domain}`);
    
    try {
        const errors = [];
        const steps = [];
        
        // Step 1: Stop and remove containers
        steps.push({ step: 'stop-containers', status: 'running' });
        for (const c of [`luna2-${domain}`, `phpmyadmin-${domain}`, `mysql-${domain}`]) {
            const stopR = await exec$(`docker stop ${c} 2>/dev/null || true`);
            const rmR = await exec$(`docker rm ${c} 2>/dev/null || true`);
            if (!stopR.success || !rmR.success) {
                errors.push(`Container ${c}: ${stopR.error || rmR.error}`);
            }
        }
        steps[steps.length - 1].status = 'success';
        steps[steps.length - 1].output = 'Container stopped and removed';
        
        // Step 2: Remove docker-compose file
        steps.push({ step: 'remove-docker-compose', status: 'running' });
        const dockerFile = path.join(WORKSPACE, `docker-compose-customer-${domain}.yml`);
        if (fs.existsSync(dockerFile)) {
            try {
                fs.unlinkSync(dockerFile);
                steps[steps.length - 1].status = 'success';
                steps[steps.length - 1].output = `Removed ${dockerFile}`;
            } catch (e) {
                errors.push(`Failed to remove docker-compose: ${e.message}`);
                steps[steps.length - 1].status = 'failed';
            }
        } else {
            steps[steps.length - 1].status = 'success';
            steps[steps.length - 1].output = 'docker-compose file not found (already removed)';
        }
        
        // Step 3: Remove nginx config (+ host symlinks)
        steps.push({ step: 'remove-nginx-config', status: 'running' });
        const nginxFile = `${SCRIPT_PATH.replace('/manage-domains-multitenant.sh', '')}/docker/nginx/multi-tenant/${domain}.conf`;
        const nginxVhostFile = `${SCRIPT_PATH.replace('/manage-domains-multitenant.sh', '')}/docker/nginx/vhosts/${domain}.conf`;
        const outputs = [];
        
        if (fs.existsSync(nginxFile)) {
            try {
                fs.unlinkSync(nginxFile);
                outputs.push(`Removed ${nginxFile}`);
            } catch (e) {
                errors.push(`Failed to remove nginx config: ${e.message}`);
            }
        }
        
        if (fs.existsSync(nginxVhostFile)) {
            try {
                fs.unlinkSync(nginxVhostFile);
                outputs.push(`Removed ${nginxVhostFile}`);
            } catch (e) {
                errors.push(`Failed to remove nginx vhost: ${e.message}`);
            }
        }
        
        // Remove host nginx symlinks (critical for preprod)
        await exec$(`sudo rm -f /etc/nginx/conf.d/${domain}.conf /etc/nginx/sites-enabled/${domain}.conf 2>/dev/null || true`);
        outputs.push('Removed host nginx symlinks');
        
        steps[steps.length - 1].status = 'success';
        steps[steps.length - 1].output = outputs.join('; ');
        
        // Step 4: Update domains-config.txt
        steps.push({ step: 'update-domains-config', status: 'running' });
        try {
            if (fs.existsSync(DOMAINS_CONFIG)) {
                const content = fs.readFileSync(DOMAINS_CONFIG, 'utf8');
                const lines = content.split('\n').filter(line => !line.startsWith(domain + '|'));
                fs.writeFileSync(DOMAINS_CONFIG, lines.join('\n'));
                steps[steps.length - 1].status = 'success';
                steps[steps.length - 1].output = 'Updated domains-config.txt';
            } else {
                steps[steps.length - 1].status = 'success';
                steps[steps.length - 1].output = 'domains-config.txt not found';
            }
        } catch (e) {
            errors.push(`Failed to update domains-config: ${e.message}`);
            steps[steps.length - 1].status = 'failed';
        }
        
        // Step 5: Reload nginx
        steps.push({ step: 'reload-nginx', status: 'running' });
        const reloadR = await exec$(`nginx -t 2>&1 && systemctl reload nginx 2>&1 || true`);
        steps[steps.length - 1].status = 'success';
        steps[steps.length - 1].output = reloadR.stdout || 'Nginx reloaded';
        
        // Step 6: Optionally remove data
        if (deleteData === true) {
            steps.push({ step: 'remove-data', status: 'running' });
            try {
                for (const dataDir of [`data/mysql-${domain}`, `data/phpmyadmin-${domain}`, `logs/luna2-${domain}`]) {
                    const fullPath = path.join(WORKSPACE, dataDir);
                    if (fs.existsSync(fullPath)) {
                        exec$(`rm -rf "${fullPath}" 2>/dev/null`);
                    }
                }
                steps[steps.length - 1].status = 'success';
                steps[steps.length - 1].output = 'Data directories removed';
            } catch (e) {
                steps[steps.length - 1].status = 'failed';
                steps[steps.length - 1].output = `Warning: ${e.message}`;
            }
        }
        
        res.json({
            success: errors.length === 0,
            message: errors.length === 0 
                ? `Istanza ${domain} eliminata completamente` 
                : `Istanza ${domain} eliminata con avvertimenti`,
            steps,
            errors: errors.length > 0 ? errors : undefined
        });
        
    } catch (error) {
        log('ERROR', `Delete instance failed: ${error.message}`);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
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

// Rinnovo / Ottenimento Let's Encrypt
app.post('/api/instances/:domain/ssl/renew', async (req, res) => {
    const { domain } = req.params;
    log('API', `SSL cert: ${domain}`);
    
    // Controlla se il certificato esiste già
    const certPath = path.join(LETSENCRYPT_DIR, domain, 'fullchain.pem');
    const certExists = fs.existsSync(certPath);
    
    // Se non esiste, usa "ssl" (ottieni nuovo cert); se esiste, usa "renew"
    const action = certExists ? 'renew' : 'ssl';
    log('API', `SSL action: ${action} per ${domain}`);
    
    const r = await exec$(`cd "${WORKSPACE}" && bash "${SCRIPT_PATH}" ${action} "${domain}"`, null, 360000);
    if (!r.success) return res.status(400).json({ success: false, error: r.error, output: r.stderr || r.stdout });
    res.json({ success: true, message: `Certificato ${certExists ? 'rinnovato' : 'ottenuto'} per ${domain}`, output: r.stdout });
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
// SAFE UPDATES & MIGRATIONS
// ──────────────────────────────────────────────────────────────

// Helper: Backup database per istanza
async function backupDatabase(domain, dbUser, dbPass) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const backupName = `luna2-${domain}-backup-${timestamp}.sql`;
    const backupPath = path.join(WORKSPACE, 'backups', backupName);
    
    // Crea cartella backups se non esiste
    const backupsDir = path.join(WORKSPACE, 'backups');
    if (!fs.existsSync(backupsDir)) fs.mkdirSync(backupsDir, { recursive: true });
    
    const cmd = `docker exec mysql-${domain} mysqldump -u${dbUser} -p${dbPass} luna2 > "${backupPath}"`;
    const r = await exec$(cmd);
    
    return {
        success: r.success,
        backupPath: r.success ? backupPath : null,
        error: r.error
    };
}

// Helper: Restore database da backup
async function restoreDatabase(domain, dbUser, dbPass, backupPath) {
    if (!fs.existsSync(backupPath)) {
        return { success: false, error: `Backup non trovato: ${backupPath}` };
    }
    
    const cmd = `cat "${backupPath}" | docker exec -i mysql-${domain} mysql -u${dbUser} -p${dbPass} luna2`;
    const r = await exec$(cmd);
    return { success: r.success, error: r.error };
}

function unquoteEnvValue(value) {
    if (!value) return value;
    const trimmed = value.trim();
    if ((trimmed.startsWith('"') && trimmed.endsWith('"')) ||
        (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
        return trimmed.slice(1, -1);
    }
    return trimmed;
}

function getDbCredentialsForDomain(domain) {
    const composePath = path.join(WORKSPACE, `docker-compose-customer-${domain}.yml`);
    let dbUser = DB_APP_USER;
    let dbPass = DB_APP_PASS;

    try {
        if (fs.existsSync(composePath)) {
            const content = fs.readFileSync(composePath, 'utf8');
            const userMatch = content.match(/^\s*SPRING_DATASOURCE_USERNAME\s*:\s*(.+)$/m)
                || content.match(/^\s*MYSQL_USER\s*:\s*(.+)$/m);
            const passMatch = content.match(/^\s*SPRING_DATASOURCE_PASSWORD\s*:\s*(.+)$/m)
                || content.match(/^\s*MYSQL_PASSWORD\s*:\s*(.+)$/m);

            if (userMatch && userMatch[1]) {
                dbUser = unquoteEnvValue(userMatch[1]);
            }
            if (passMatch && passMatch[1]) {
                dbPass = unquoteEnvValue(passMatch[1]);
            }
        }
    } catch (e) {
        log('WARN', `Credenziali DB compose non leggibili per ${domain}: ${e.message}`);
    }

    return { dbUser, dbPass };
}

// Helper: Leggi migrazioni applicate
async function getAppliedMigrations(domain, dbUser, dbPass) {
    const cmd = `docker exec mysql-${domain} mysql -u${dbUser} -p${dbPass} luna2 -e "SELECT migration FROM schema_migrations ORDER BY applied_at" 2>/dev/null || echo ""`;
    const r = await exec$(cmd);
    if (!r.success) return [];
    
    return r.stdout
        .split('\n')
        .filter(line => line.trim() && !line.includes('migration'))
        .map(line => line.trim());
}

async function ensureSchemaMigrationsTable(domain, dbUser, dbPass) {
    const checkCmd = `docker exec mysql-${domain} mysql -u${dbUser} -p${dbPass} luna2 -e "SHOW TABLES LIKE 'schema_migrations'" 2>/dev/null`;
    const checkR = await exec$(checkCmd);
    if (!checkR.success) {
        return { success: false, exists: false, error: checkR.error || checkR.stderr };
    }

    const exists = checkR.stdout.includes('schema_migrations');
    if (exists) {
        return { success: true, exists: true, created: false };
    }

    const createCmd = `docker exec mysql-${domain} mysql -u${dbUser} -p${dbPass} luna2 -e "CREATE TABLE IF NOT EXISTS schema_migrations (migration VARCHAR(255) PRIMARY KEY, applied_at DATETIME NOT NULL)" 2>/dev/null`;
    const createR = await exec$(createCmd);
    if (!createR.success) {
        return { success: false, exists: false, error: createR.error || createR.stderr };
    }

    return { success: true, exists: false, created: true };
}

async function baselineMigrations(domain, dbUser, dbPass, migrations) {
    if (!migrations || migrations.length === 0) {
        return { success: true, count: 0 };
    }

    const now = new Date().toISOString().replace('T', ' ').slice(0, 19);
    for (const migFile of migrations) {
        const migName = migFile.replace('.sql', '');
        const cmd = `docker exec mysql-${domain} mysql -u${dbUser} -p${dbPass} luna2 -e "INSERT IGNORE INTO schema_migrations (migration, applied_at) VALUES ('${migName}', '${now}')" 2>/dev/null`;
        const r = await exec$(cmd);
        if (!r.success) {
            return { success: false, error: r.error || r.stderr, failedMigration: migName };
        }
    }

    return { success: true, count: migrations.length };
}

// Helper: Registra migrazione applicata
async function recordMigration(domain, migrationName, dbUser, dbPass) {
    const now = new Date().toISOString().replace('T', ' ').slice(0, 19);
    const cmd = `docker exec mysql-${domain} mysql -u${dbUser} -p${dbPass} luna2 -e "INSERT INTO schema_migrations (migration, applied_at) VALUES ('${migrationName}', '${now}')" 2>/dev/null || true`;
    await exec$(cmd);
}

async function runSafeUpdateTask(taskId = null) {
    const steps = [];

    const setProgress = (msg) => {
        if (taskId) {
            updateTask(taskId, { progress: msg });
        }
    };

    // 1. Git pull
    setProgress('Git pull in corso...');
    steps.push({ step: 'git-pull', status: 'running' });
    let gitCmd = 'git pull origin main';
    let gitEnv = { ...process.env };

    if (GITHUB_TOKEN) {
        const askPassScript = path.join(WORKSPACE, '.git-askpass.sh');
        fs.writeFileSync(askPassScript, `#!/bin/bash\necho "${GITHUB_TOKEN}"`, { mode: 0o755 });
        gitEnv.GIT_ASKPASS = askPassScript;
        gitEnv.GIT_USERNAME = GIT_USERNAME;
    }

    const pullR = await exec$(`cd "${WORKSPACE}" && ${gitCmd}`, gitEnv, 300000);
    if (!pullR.success) {
        steps[0].status = 'failed';
        steps[0].error = pullR.error;
        return { success: false, steps, error: 'Git pull fallito' };
    }
    steps[0].status = 'success';
    steps[0].output = pullR.stdout.trim();

    // 2. Maven build
    setProgress('Build Maven in corso (puo richiedere alcuni minuti)...');
    steps.push({ step: 'maven-build', status: 'running' });
    const buildR = await exec$(`cd "${WORKSPACE}" && mvn clean package -DskipTests`, null, 1200000);
    if (!buildR.success) {
        steps[1].status = 'failed';
        steps[1].error = buildR.error;
        return { success: false, steps, error: 'Maven build fallito' };
    }
    steps[1].status = 'success';
    steps[1].output = 'WAR compilato';

    const warPath = path.join(WORKSPACE, 'target/luna2.war');
    if (!fs.existsSync(warPath)) {
        return { success: false, steps, error: 'WAR non trovato dopo build' };
    }

    // 3. Rileva migrazioni disponibili
    const migrationDir = path.join(WORKSPACE, 'database/migrations');
    let availableMigrations = [];
    if (fs.existsSync(migrationDir)) {
        availableMigrations = fs.readdirSync(migrationDir)
            .filter(f => f.endsWith('.sql') && f[0].match(/\d/) && !/example/i.test(f))
            .sort();
    }

    steps.push({ step: 'migrations-detected', status: 'success', count: availableMigrations.length });

    // 4. Backup di tutte le istanze
    setProgress('Backup database di tutte le istanze...');
    steps.push({ step: 'backup-all-databases', status: 'running', backups: [] });
    const domains = await parseDomainConfig();
    const backups = {}; // { domain: backupPath }

    for (const d of domains) {
        const creds = getDbCredentialsForDomain(d.domain);
        const backupResult = await backupDatabase(d.domain, creds.dbUser, creds.dbPass);
        steps[steps.length - 1].backups.push({
            domain: d.domain,
            success: backupResult.success,
            path: backupResult.backupPath
        });
        if (backupResult.success) {
            backups[d.domain] = backupResult.backupPath;
        }
    }
    steps[steps.length - 1].status = 'success';

    // 5. Deploy a tutte le istanze (con migrazioni)
    steps.push({ step: 'deploy-all', status: 'running', instances: [] });

    for (const d of domains) {
        setProgress(`Deploy in corso su ${d.domain}...`);
        const instStep = { domain: d.domain, substeps: [] };
        let migrationFailed = false;
        const creds = getDbCredentialsForDomain(d.domain);

        const migTable = await ensureSchemaMigrationsTable(d.domain, creds.dbUser, creds.dbPass);
        if (!migTable.success) {
            instStep.substeps.push({
                action: 'ensure-schema-migrations',
                success: false,
                error: migTable.error || 'Impossibile verificare/creare schema_migrations'
            });
            instStep.success = false;
            steps[steps.length - 1].instances.push(instStep);
            continue;
        }

        if (migTable.created) {
            const baseline = await baselineMigrations(d.domain, creds.dbUser, creds.dbPass, availableMigrations);
            instStep.substeps.push({
                action: 'baseline-migrations',
                success: baseline.success,
                count: baseline.count || 0,
                error: baseline.error || null
            });
            if (!baseline.success) {
                instStep.success = false;
                steps[steps.length - 1].instances.push(instStep);
                continue;
            }
        }

        // 5a. Applica migrazioni
        const appliedMigs = await getAppliedMigrations(d.domain, creds.dbUser, creds.dbPass);
        const newMigs = availableMigrations.filter(m => !appliedMigs.includes(m.replace('.sql', '')));

        for (const migFile of newMigs) {
            const migPath = path.join(migrationDir, migFile);
            const migContent = fs.readFileSync(migPath, 'utf8');
            const migName = migFile.replace('.sql', '');

            const migCmd = `docker exec mysql-${d.domain} mysql -u${creds.dbUser} -p${creds.dbPass} luna2 -e "${migContent.replace(/"/g, '\\"')}" 2>&1`;
            const migR = await exec$(migCmd);

            instStep.substeps.push({
                migration: migName,
                success: migR.success,
                error: migR.success ? null : migR.error
            });

            if (migR.success) {
                await recordMigration(d.domain, migName, creds.dbUser, creds.dbPass);
            } else {
                migrationFailed = true;
                if (backups[d.domain]) {
                    log('API', `ROLLBACK: Ripristino ${d.domain} da backup`);
                    const restoreR = await restoreDatabase(d.domain, creds.dbUser, creds.dbPass, backups[d.domain]);
                    instStep.substeps.push({
                        action: 'rollback',
                        success: restoreR.success,
                        error: restoreR.error
                    });
                }
                break;
            }
        }

        // 5b. Deploya WAR (solo se migrazioni ok)
        if (!migrationFailed && instStep.substeps.every(s => s.success)) {
            const cpR = await exec$(`docker cp "${warPath}" luna2-${d.domain}:/usr/local/tomcat/webapps/ROOT.war`);
            if (cpR.success) {
                await exec$(`docker exec luna2-${d.domain} sh -lc 'rm -rf /usr/local/tomcat/webapps/luna2 /usr/local/tomcat/webapps/luna2.war'`);
            }
            instStep.substeps.push({ action: 'copy-war', success: cpR.success, error: cpR.error });

            if (cpR.success) {
                const restartR = await exec$(`docker restart luna2-${d.domain}`);
                instStep.substeps.push({ action: 'restart', success: restartR.success, error: restartR.error });
                instStep.success = restartR.success;
            } else {
                instStep.success = false;
            }
        } else {
            instStep.success = false;
        }

        steps[steps.length - 1].instances.push(instStep);
    }
    steps[steps.length - 1].status = 'success';

    const deployInstances = steps[steps.length - 1].instances;
    const allSuccess = deployInstances.every(i => i.success);
    const failedInstances = deployInstances.filter(i => !i.success);
    const errorSummary = failedInstances.length
        ? `Istanze fallite: ${failedInstances.map(i => {
            const failedStep = (i.substeps || []).find(s => s.success === false);
            const reason = failedStep ? (failedStep.action || failedStep.migration || 'errore') : 'errore';
            return `${i.domain} (${reason})`;
        }).join(', ')}`
        : null;

    return {
        success: allSuccess,
        message: allSuccess
            ? 'Update distributo a tutte le istanze con successo'
            : 'Update completato con errori - controlla backup',
        error: errorSummary,
        steps,
        backupLocations: backups
    };
}

// POST /api/system/safe-update - avvia update asincrono con polling task
app.post('/api/system/safe-update', async (req, res) => {
    log('API', 'SAFE-UPDATE: Pull + Build + Backup + Migrate + Deploy');

    const taskId = createTask('system-safe-update', 'all');
    updateTask(taskId, { progress: 'Preparazione update...' });

    res.json({
        success: true,
        message: 'Update avviato',
        taskId,
        statusUrl: `/api/tasks/${taskId}/status`
    });

    (async () => {
        try {
            const result = await runSafeUpdateTask(taskId);
            updateTask(taskId, {
                status: result.success ? 'completed' : 'failed',
                progress: result.success ? 'Update completato' : 'Update completato con errori',
                result,
                error: result.success ? null : result.error
            });
        } catch (e) {
            updateTask(taskId, {
                status: 'failed',
                progress: 'Errore inatteso durante update',
                error: e.message
            });
        }
    })();
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
            .filter(f => f.endsWith('.sql') && !/example/i.test(f))
            .sort();
        
        for (const sqlFile of files) {
            const sqlPath = path.join(migrationDir, sqlFile);
            const sqlContent = fs.readFileSync(sqlPath, 'utf8');
            
            // Esegui la migrazione
            const domains = await parseDomainConfig();
            const inst = domains.find(d => d.domain === domain);
            if (inst) {
                const creds = getDbCredentialsForDomain(domain);
                const mysqlCmd = `docker exec mysql-${domain} mysql -u${creds.dbUser} -p${creds.dbPass} luna2 -e "${sqlContent.replace(/"/g, '\\"')}" 2>&1 || true`;
                const migR = await exec$(mysqlCmd);
                steps[0].output = (steps[0].output || '') + `\n${sqlFile}: ${migR.success ? 'OK' : 'WARN'}`;
            }
        }
        steps[0].status = 'success';
    }
    
    // 2. Copia nuovo WAR nel container
    steps.push({ step: 'copy-war', status: 'running' });
    const cpR = await exec$(`docker cp "${warPath}" luna2-${domain}:/usr/local/tomcat/webapps/ROOT.war`);
    if (cpR.success) {
        await exec$(`docker exec luna2-${domain} sh -lc 'rm -rf /usr/local/tomcat/webapps/luna2 /usr/local/tomcat/webapps/luna2.war'`);
    }
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
        const cpR = await exec$(`docker cp "${warPath}" luna2-${d.domain}:/usr/local/tomcat/webapps/ROOT.war 2>&1`);
        if (cpR.success) {
            await exec$(`docker exec luna2-${d.domain} sh -lc 'rm -rf /usr/local/tomcat/webapps/luna2 /usr/local/tomcat/webapps/luna2.war'`);
        }
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
