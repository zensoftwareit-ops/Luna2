# 🚀 Deployment UI Update - Server Manager Preprod

## Commit Appena Sottomesso
```
a76476f - feat: Professional UI redesign + complete instance deletion cleanup
```

## Novità Implementate ✨

### 1. **Nuova Veste Grafica Professionale**
- **Sidebar Navigation**: Navigazione fissa su sinistra con gradient background scuro
- **Modern Stat Cards**: Metriche istanze, container, disco, memoria con hover effects
- **Professional Color Scheme**: Colori moderni (viola #667eea primario, grigio scuro per testi)
- **Font Awesome Icons**: Icone professionali in tutto il pannello
- **Responsive Design**: Perfetto su mobile (breakpoints 1024px, 768px)
- **Smooth Animations**: Login slide-up, modal scale, card hover transitions
- **Modern Typography**: Tipografia professionale con spacing ottimizzato

### 2. **Pulizia Completa Istanze (Quando Elimini)**
Prima eliminava solo i container, lasciando orphaned files:
- ❌ docker-compose-customer-{DOMAIN}.yml rimasto
- ❌ docker/nginx/multi-tenant/{DOMAIN}.conf rimasto
- ❌ Entry in domains-config.txt rimasto

Ora fa cleanup completo:
- ✅ Stop e rimozione container (app, mysql, phpmyadmin)
- ✅ Cancellazione file docker-compose
- ✅ Cancellazione config nginx
- ✅ Rimozione entry da domains-config.txt
- ✅ Reload nginx
- ✅ Opzionale: cancellazione dati (database e logs)

## Deploy su Preprod Server

### Opzione A: Deployment Manuale SSH

```bash
# 1. Accedi al server preprod
ssh root@TUO_SERVER_IP

# 2. Vai nella cartella Luna2
cd /root/Luna2

# 3. Pull dei nuovi cambiamenti
git pull origin main

# 4. Arresta il Server Manager (se in esecuzione con PM2/systemd)
# Se via systemd:
sudo systemctl stop luna2-manager

# Se via npm (in backgroundprocesso):
# Ctrl+C o kill il processo

# 5. Riavvia il Server Manager
# Se systemd:
sudo systemctl start luna2-manager

# Se npm:
cd server-manager
npm start &
```

### Opzione B: Docker Compose (Se Containerizzato)

Se il Server Manager è dentro Docker:

```bash
ssh root@TUO_SERVER_IP
cd /root/Luna2/server-manager

# Stop e ricompilazione
docker-compose down
docker-compose up -d --build

# Verifica
curl http://localhost:8888
```

### Opzione C: Deployment Automatico (Se Hai CI/CD)

Se hai GitHub Actions o simile, il push automaticamente:
1. Compila le modifiche
2. Testa il build
3. Deploy su staging
4. Deploy su produzione (con approvazione)

## Verificare il Deployment ✓

Una volta aggiornato il server:

1. **Apri il browser**
   ```
   http://TUO_SERVER_IP:8888
   ```

2. **Verifica nuova interfaccia:**
   - ✓ Sidebar scura su sinistra
   - ✓ Topbar con titolo e pulsanti
   - ✓ Stat cards con numeri
   - ✓ Form per nuova istanza
   - ✓ Tabella istanze con colori status

3. **Testa authentication:**
   - Accedi con credenziali da `.env`
   - Defaulta: `admin` / `Luna2Admin!ChangeMe`
   - Verifica token bearer viene salvato

4. **Testa eliminazione istanza:**
   - Crea istanza test
   - Clicca "Rimuovi"
   - Verifica no orphaned files rimasti:
     ```bash
     ls docker-compose-customer-TEST.yml 2>/dev/null || echo "✓ Docker-compose rimosso"
     grep "TEST" domains-config.txt || echo "✓ Domains config pulito"
     ls docker/nginx/multi-tenant/TEST.conf 2>/dev/null || echo "✓ Nginx config rimosso"
     ```

## Struttura Nuova Interfaccia

```
┌─────────────────────────────────────────────┐
│ Luna2 Manager      ┌─ Topbar (sticky)      │
├─────────┬─────────────────────────────────┤
│         │                                 │
│ Sidebar │  Main Content Area              │
│         │                                 │
│ • Dashboard ├─ Stat Cards Grid            │
│ • Istanze   ├─ Create Instance Form       │
│ • SSL       ├─ Instances Table            │
│ • Deploy    │                             │
│ • Esci      │    Domain | Status | Actions│
│             │    ────────────────────────│
├─────────┴─────────────────────────────────┤
│ (Mobile: sidebar becomes top menu)        │
└─────────────────────────────────────────────┘
```

## File Modificati

| File | Cambio | Linee |
|------|--------|-------|
| `server-manager/public/index.html` | Redesign completo | 1500+ |
| `server-manager/server.js` (DELETE endpoint) | 6-step cleanup | 100+ |

## Rollback (Se Necessario)

Se qualcosa non funziona:

```bash
cd /root/Luna2

# Torna al commit precedente
git reset --hard ac16cad

# Riavvia il server
sudo systemctl restart luna2-manager
```

## Note Importanti

1. **Backward Compatibility**: Il server.js mantiene tutti gli endpoint, solo DELETE è ricritto
2. **No Database Changes**: Nessuna migrazione DB necessaria
3. **No Dependencies**: Nessun nuovo pacchetto npm (Font Awesome via CDN)
4. **Responsive**: Funziona perfetto su mobile, tablet, desktop
5. **Performance**: CSS puro (no framework), JS vanilla, load immediato

## Supporto

Se hai problemi:

1. Verifica il log del server:
   ```bash
   sudo journalctl -u luna2-manager -f
   # o
   tail -f /var/log/luna2-manager.log
   ```

2. Testa la API direttamente:
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8888/api/instances
   ```

3. Verifica Git status:
   ```bash
   cd /root/Luna2 && git log --oneline -5 && git status
   ```

---

**Deploy Date**: 2025-03-03
**Commit**: a76476f
**Version**: v2.0
