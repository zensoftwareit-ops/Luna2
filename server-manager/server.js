#!/usr/bin/env node

/**
 * Multi-Tenant Server Manager
 * Gestisce multiple istanze di Luna2 con UI web
 * API REST + Web Dashboard
 */

const express = require('express');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');
const { promisify } = require('util');
const cors = require('cors');
const bodyParser = require('body-parser');

const execAsync = promisify(exec);
const readFileAsync = promisify(fs.readFile);

// Configuration
const app = express();
const PORT = process.env.PORT || 8888;
const WORKSPACE_DIR = process.env.WORKSPACE_DIR || '/workspaces/Luna2';
const DOMAINS_CONFIG = path.join(WORKSPACE_DIR, 'domains-config.txt');
const SCRIPT_PATH = path.join(WORKSPACE_DIR, 'manage-domains-multitenant.sh');

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

// Utility functions
const log = (level, msg) => {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] [${level}] ${msg}`);
};

const executeBash = async (command) => {
    try {
        log('DEBUG', `Executing: ${command}`);
        const { stdout, stderr } = await execAsync(command);
        return { success: true, stdout, stderr };
    } catch (error) {
        log('ERROR', `Command failed: ${error.message}`);
        return { success: false, error: error.message };
    }
};

const parseDomainConfig = async () => {
    try {
        let content = '';
        if (fs.existsSync(DOMAINS_CONFIG)) {
            content = await readFileAsync(DOMAINS_CONFIG, 'utf8');
        }
        
        const domains = [];
        content.split('\n').forEach(line => {
            if (!line.startsWith('#') && line.trim()) {
                const parts = line.split('|');
                if (parts.length >= 5) {
                    domains.push({
                        domain: parts[0],
                        customer: parts[1],
                        mysqlPort: parts[2],
                        appPort: parts[3],
                        phpadminPort: parts[4],
                        created: parts[5] || 'Unknown',
                        status: parts[6] || 'unknown'
                    });
                }
            }
        });
        return domains;
    } catch (error) {
        log('ERROR', `Error parsing domain config: ${error.message}`);
        return [];
    }
};

const getContainerStatus = async (containerName) => {
    try {
        const result = await executeBash(
            `docker inspect ${containerName} --format='{{.State.Status}}' 2>/dev/null`
        );
        return result.stdout.trim();
    } catch (e) {
        return 'unknown';
    }
};

//################################################################################
// API ENDPOINTS - CUSTOMERS
//################################################################################

/**
 * GET /api/customers
 * Lista tutti i clienti con status
 */
app.get('/api/customers', async (req, res) => {
    try {
        log('API', 'GET /api/customers');
        const domains = await parseDomainConfig();
        
        // Arricchisci con status container
        for (let domain of domains) {
            domain.containers = {
                mysql: {
                    name: `mysql-${domain.domain}`,
                    status: await getContainerStatus(`mysql-${domain.domain}`)
                },
                app: {
                    name: `luna2-${domain.domain}`,
                    status: await getContainerStatus(`luna2-${domain.domain}`)
                },
                phpmyadmin: {
                    name: `phpmyadmin-${domain.domain}`,
                    status: await getContainerStatus(`phpmyadmin-${domain.domain}`)
                }
            };
        }
        
        res.json({ 
            success: true, 
            count: domains.length,
            customers: domains 
        });
    } catch (error) {
        log('ERROR', error.message);
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * GET /api/customers/:domain
 * Ottiene dettagli specifici di un cliente
 */
app.get('/api/customers/:domain', async (req, res) => {
    try {
        const { domain } = req.params;
        log('API', `GET /api/customers/${domain}`);
        
        const domains = await parseDomainConfig();
        const customer = domains.find(d => d.domain === domain);
        
        if (!customer) {
            return res.status(404).json({ success: false, error: 'Customer not found' });
        }
        
        // Dettagli container
        customer.containers = {
            mysql: {
                name: `mysql-${domain}`,
                status: await getContainerStatus(`mysql-${domain}`),
                port: customer.mysqlPort
            },
            app: {
                name: `luna2-${domain}`,
                status: await getContainerStatus(`luna2-${domain}`),
                port: customer.appPort,
                url: `https://${domain}`
            },
            phpmyadmin: {
                name: `phpmyadmin-${domain}`,
                status: await getContainerStatus(`phpmyadmin-${domain}`),
                port: customer.phpadminPort,
                url: `https://phpmyadmin.${domain}`
            }
        };
        
        res.json({ success: true, customer });
    } catch (error) {
        log('ERROR', error.message);
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * POST /api/customers
 * Aggiunge un nuovo cliente
 */
app.post('/api/customers', async (req, res) => {
    try {
        const { domain, customer_name } = req.body;
        
        if (!domain || !customer_name) {
            return res.status(400).json({ 
                success: false, 
                error: 'Missing domain or customer_name' 
            });
        }
        
        log('API', `POST /api/customers - Adding ${domain}`);
        
        const result = await executeBash(
            `cd "${WORKSPACE_DIR}" && bash "${SCRIPT_PATH}" add "${domain}" "${customer_name}"`
        );
        
        if (!result.success) {
            return res.status(400).json({ success: false, error: result.error });
        }
        
        res.json({ 
            success: true, 
            message: `Customer added successfully: ${domain}`,
            output: result.stdout 
        });
    } catch (error) {
        log('ERROR', error.message);
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * DELETE /api/customers/:domain
 * Rimuove un cliente
 */
app.delete('/api/customers/:domain', async (req, res) => {
    try {
        const { domain } = req.params;
        
        log('API', `DELETE /api/customers/${domain}`);
        
        const result = await executeBash(
            `cd "${WORKSPACE_DIR}" && bash "${SCRIPT_PATH}" remove "${domain}" <<< "y"`
        );
        
        res.json({ 
            success: true, 
            message: `Customer removed: ${domain}`,
            output: result.stdout 
        });
    } catch (error) {
        log('ERROR', error.message);
        res.status(500).json({ success: false, error: error.message });
    }
});

//################################################################################
// API ENDPOINTS - CUSTOMER CONTAINERS
//################################################################################

/**
 * GET /api/customers/:domain/containers
 * Status di tutti i container di un cliente
 */
app.get('/api/customers/:domain/containers', async (req, res) => {
    try {
        const { domain } = req.params;
        log('API', `GET /api/customers/${domain}/containers`);
        
        const containers = {
            mysql: await getContainerStatus(`mysql-${domain}`),
            app: await getContainerStatus(`luna2-${domain}`),
            phpmyadmin: await getContainerStatus(`phpmyadmin-${domain}`)
        };
        
        res.json({ success: true, domain, containers });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * POST /api/customers/:domain/containers/:container/restart
 * Riavvia un container specifico
 */
app.post('/api/customers/:domain/containers/:container/restart', async (req, res) => {
    try {
        const { domain, container } = req.params;
        const containerName = `${container}-${domain}`;
        
        log('API', `Restarting container: ${containerName}`);
        
        const result = await executeBash(`docker restart ${containerName}`);
        
        if (!result.success) {
            return res.status(400).json({ success: false, error: result.error });
        }
        
        await new Promise(resolve => setTimeout(resolve, 3000));
        const newStatus = await getContainerStatus(containerName);
        
        res.json({ 
            success: true, 
            container: containerName,
            status: newStatus
        });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * POST /api/customers/:domain/containers/:container/stop
 * Ferma un container
 */
app.post('/api/customers/:domain/containers/:container/stop', async (req, res) => {
    try {
        const { domain, container } = req.params;
        const containerName = `${container}-${domain}`;
        
        log('API', `Stopping container: ${containerName}`);
        
        const result = await executeBash(`docker stop ${containerName}`);
        
        if (!result.success) {
            return res.status(400).json({ success: false, error: result.error });
        }
        
        res.json({ success: true, message: `Container stopped: ${containerName}` });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * POST /api/customers/:domain/containers/:container/start
 * Avvia un container
 */
app.post('/api/customers/:domain/containers/:container/start', async (req, res) => {
    try {
        const { domain, container } = req.params;
        const containerName = `${container}-${domain}`;
        
        log('API', `Starting container: ${containerName}`);
        
        const result = await executeBash(`docker start ${containerName}`);
        
        if (!result.success) {
            return res.status(400).json({ success: false, error: result.error });
        }
        
        await new Promise(resolve => setTimeout(resolve, 3000));
        const newStatus = await getContainerStatus(containerName);
        
        res.json({ 
            success: true, 
            message: `Container started: ${containerName}`,
            status: newStatus
        });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

//################################################################################
// API ENDPOINTS - LOGS
//################################################################################

/**
 * GET /api/customers/:domain/logs/:container
 * Ottiene i logs di un container
 */
app.get('/api/customers/:domain/logs/:container', async (req, res) => {
    try {
        const { domain, container } = req.params;
        const { lines = 100 } = req.query;
        const containerName = `${container}-${domain}`;
        
        log('API', `GET logs for ${containerName}`);
        
        const result = await executeBash(`docker logs --tail ${lines} ${containerName}`);
        
        if (!result.success) {
            return res.status(400).json({ success: false, error: result.error });
        }
        
        res.json({ 
            success: true, 
            container: containerName,
            logs: result.stdout 
        });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

//################################################################################
// API ENDPOINTS - SSL
//################################################################################

/**
 * POST /api/customers/:domain/ssl
 * Richiede certificato SSL
 */
app.post('/api/customers/:domain/ssl', async (req, res) => {
    try {
        const { domain } = req.params;
        log('API', `POST /api/customers/${domain}/ssl`);
        
        const result = await executeBash(
            `cd "${WORKSPACE_DIR}" && bash "${SCRIPT_PATH}" ssl "${domain}"`
        );
        
        if (!result.success) {
            return res.status(400).json({ success: false, error: result.error });
        }
        
        res.json({ 
            success: true, 
            message: `SSL certificate requested for ${domain}`,
            output: result.stdout 
        });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * POST /api/customers/:domain/ssl/renew
 * Rinnova certificato SSL
 */
app.post('/api/customers/:domain/ssl/renew', async (req, res) => {
    try {
        const { domain } = req.params;
        log('API', `POST /api/customers/${domain}/ssl/renew`);
        
        const result = await executeBash(
            `cd "${WORKSPACE_DIR}" && bash "${SCRIPT_PATH}" renew "${domain}"`
        );
        
        if (!result.success) {
            return res.status(400).json({ success: false, error: result.error });
        }
        
        res.json({ 
            success: true, 
            message: `SSL certificate renewed for ${domain}`,
            output: result.stdout 
        });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

//################################################################################
// API ENDPOINTS - SYSTEM
//################################################################################

/**
 * GET /api/status
 * Status generale del sistema
 */
app.get('/api/status', async (req, res) => {
    try {
        log('API', 'GET /api/status');
        
        const domains = await parseDomainConfig();
        
        res.json({
            success: true,
            timestamp: new Date().toISOString(),
            customers: domains.length,
            version: '2.0'
        });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * GET /api/health
 * Health check
 */
app.get('/api/health', (req, res) => {
    res.json({ 
        status: 'OK', 
        timestamp: new Date().toISOString(),
        version: '2.0'
    });
});

//################################################################################
// WEB ROUTES
//################################################################################

/**
 * GET /
 * Serve index.html
 */
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

//################################################################################
// 404 & ERROR HANDLING
//################################################################################

app.use((req, res) => {
    res.status(404).json({ error: 'Not found' });
});

app.use((err, req, res, next) => {
    log('ERROR', err.message);
    res.status(500).json({ error: err.message });
});

//################################################################################
// START SERVER
//################################################################################

app.listen(PORT, () => {
    log('INFO', `Multi-Tenant Server Manager listening on port ${PORT}`);
    log('INFO', `Web UI: http://localhost:${PORT}`);
    log('INFO', `API: http://localhost:${PORT}/api`);
    log('INFO', `Health: http://localhost:${PORT}/api/health`);
    log('INFO', `Workspace: ${WORKSPACE_DIR}`);
});
