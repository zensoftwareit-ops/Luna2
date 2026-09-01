# Luna2 PHP

Riscrittura PHP di Luna2 per hosting Plesk, con MySQL 8 e una sola webroot `public/`.

Il branch PHP non richiede Java, Maven, Tomcat, JSP o un processo applicativo residente. Comprende già:

- autenticazione Argon2id, isolamento per azienda, CSRF, audit e security header;
- anagrafiche clienti/fornitori, prodotti, CRM, magazzini, commesse, calendario, HR, e-commerce e noleggio;
- preventivi, ordini, DDT, proforma, fatture attive/passive e note di credito con righe, numerazione e PDF;
- generazione XML FatturaPA e struttura per provider SDI accreditato;
- piano dei conti gerarchico e classificato, causali, sezionali, prima nota, automatismi, giornale, mastrini e bilancio di verifica;
- registri IVA con regimi/esigibilità/detraibilità, IVA per cassa, pro-rata, liquidazioni, rettifiche e blocchi periodo;
- partite clienti/fornitori, incassi/pagamenti, ritenute, banche e riconciliazione manuale controllata;
- stato patrimoniale e conto economico riclassificati, assestamenti, chiusura/apertura e cespiti civilistici/fiscali;
- prospetti di raccordo LIPE e IVA annuale, esplicitamente non trasmissibili fino alla validazione del futuro servizio;
- endpoint e-invoice e verifica Partita IVA configurabili senza memorizzare segreti nel database;
- import DATEV Koinos con staging, anteprima, idempotenza, log, quadrature e rollback per CSV/XLSX/XML/ZIP.
- conversioni guidate e parziali preventivo → ordine → DDT → fattura con tracciamento delle quantità residue;
- giacenze atomiche, trasferimenti, picking barcode, impegni e annullamenti controllati;
- consuntivazione e fatturazione commesse, e-mail con coda/tracking e workflow ferie;
- adapter WooCommerce, Shopify, Amazon SP-API ed eBay per ordini, catalogo, stock e prezzi;
- automazioni noleggio/ticket, calendari Google/iCloud/CalDAV e report direzionale XLSX a otto fogli;
- payroll controllato: simulazioni separate dal percorso di produzione basato su cedolini certificati e validazione professionale;
- centro professionale con qualità dati, stampe numerate e validate, fascicoli fiscali controllati e archivio con checksum;
- import estratti conto CSV, CAMT.053 e MT940 con deduplica e proposte motivate di riconciliazione;
- ricerca globale, notifiche operative, viste salvate, filtri avanzati, paginazione e operazioni massive sugli archivi;
- checklist di avviamento aziendale con controlli automatici e passaggi di collaudo espliciti;
- area `SUPERUSER` per creare aziende e utenti, reimpostare gli accessi e configurare i moduli;
- diagnostica integrata per migrazioni, tabelle, estensioni PHP e permessi storage;
- log degli errori con codice di riferimento, senza esporre dettagli tecnici agli utenti.

Solo il `SUPERUSER` di piattaforma vede **Aziende e utenti**, **Gestione moduli** e **Stato del sistema**. Gli utenti aziendali, inclusi `OWNER` e `ADMIN`, non possono modificare la composizione del gestionale. Se lo schema non è completo, l’interfaccia indica la migrazione o la tabella mancante invece di mostrare un errore generico. I dettagli delle eccezioni sono salvati in `storage/logs/application-YYYY-MM-DD.log`.

## Stato del progetto

Questa è la release candidate 6.4 della riscrittura PHP. Il via libera al primo cliente resta subordinato alla quadratura di un export DATEV reale, ai collaudi con credenziali dei provider e all’UAT amministrativa/fiscale. SDI, conservazione a norma, file telematici ministeriali e formato proprietario Koinos restano dipendenze esterne. La matrice puntuale è in [docs/FUNCTIONAL_PARITY.md](docs/FUNCTIONAL_PARITY.md) e il collaudo della release in [docs/ERP_PARITY_RELEASE.md](docs/ERP_PARITY_RELEASE.md).

## Requisiti

- PHP 8.2 o successivo (consigliato PHP 8.4 su Plesk)
- MySQL 8.0 o MariaDB compatibile con JSON, window function e `CHECK`
- Composer 2
- estensioni PHP: `ctype`, `dom`, `fileinfo`, `gd`, `json`, `libxml`, `mbstring`, `pdo_mysql`, `simplexml`, `zip`
- HTTPS

## Installazione

```bash
git clone --branch luna2-php https://github.com/zensoftwareit-ops/Luna2.git luna2-php
cd luna2-php
cp .env.example .env
composer install --no-dev --optimize-autoloader
php bin/luna key:generate
php bin/luna migrate
```

Il comando `key:generate` stampa una riga `APP_KEY=...`: copiarla in `.env`. Alla prima esecuzione, `migrate` crea anche il `SUPERUSER` e stampa email e password casuale una sola volta. Conservare subito le credenziali in un password manager; le esecuzioni successive non rigenerano né ristampano la password. Dopo il login il superuser crea l’azienda, gli utenti aziendali e la configurazione dei moduli interamente dall’interfaccia web.

Impostare la document root del dominio su `public/`. Se Plesk non consente di cambiare la document root, il file `.htaccess` nella radice inoltra le richieste a `public/` e blocca le cartelle private; la webroot dedicata resta la configurazione raccomandata.

Configurare in Plesk un task pianificato ogni 5 minuti (il comando è idempotente):

```bash
/opt/plesk/php/8.4/bin/php /var/www/vhosts/example.it/luna2-php/bin/luna cron:daily
```

## Import DATEV Koinos

Aprire **Importazioni**. Ogni caricamento crea un lotto immutabile con checksum. La conferma è separata dal caricamento; dopo la conferma è disponibile il rollback. Ordine consigliato:

1. piano dei conti gerarchico e anagrafiche;
2. XML FatturaPA attivi e passivi;
3. prima nota e registri IVA storici;
4. partite aperte e pagamenti/incassi;
5. cespiti e movimenti bancari;
6. quadratura di saldi, IVA, partite, banche e registro cespiti sul campione Koinos.

Per l’archivio proprietario **Esporta archivio** serve almeno un export reale e anonimizzato del cliente. Il piano completo è in [docs/DATEV_KOINOS_MIGRATION.md](docs/DATEV_KOINOS_MIGRATION.md).

## SDI

L’applicazione genera XML FatturaPA, ma non simula un endpoint pubblico inesistente. La trasmissione deve avvenire tramite:

- provider/intermediario con API e accordo di servizio;
- canale Web Service/FTP preventivamente accreditato presso SdI;
- PEC o upload manuale, per volumi contenuti.

Gli URL dei servizi si configurano in **Contabilità → Endpoint e-invoice**. Le credenziali restano fuori dal database e vengono indicate soltanto tramite riferimenti `ENV:NOME_VARIABILE`. Il connettore di ricezione usa l’endpoint `EINVOICE`: la risposta deve contenere `invoices`, con `filename` e `content_base64` per ciascun XML. Da **Importazioni e fatture passive** si acquisiscono i file in staging e si confermano solo dopo l’anteprima. Restano disponibili anche XML, P7M e ZIP caricati manualmente.

## Automatismi fiscali e anagrafici

- clienti e fornitori normalizzano la Partita IVA, ne verificano formalmente il formato italiano e bloccano i duplicati con un messaggio esplicito;
- il pulsante di compilazione assistita usa l’endpoint `VAT_LOOKUP`, che riceve `country` e `vat_number` e restituisce JSON con almeno `business_name`;
- giorni, fine mese e metodo di pagamento salvati in anagrafica calcolano la scadenza del documento; ABI, CAB e banca vengono richiamati automaticamente per la Ri.Ba.;
- ritenuta, imponibile e aliquota possono essere configurati sull’azienda o sul fornitore e sono riportati su documento, XML e PDF;
- le fatture passive FatturaPA importano anche dati fiscali del fornitore, scadenza, modalità, banca e ritenuta.

## Test

```bash
composer test
```

Il test esegue il lint di tutti i file PHP, controlla schema e rotte, renderizza le viste principali, verifica i contratti HTML/CSS e cerca credenziali accidentalmente versionate.

## Struttura

```text
app/                 core, controller e servizi
bin/luna             CLI migrazioni/setup/cron
config/              configurazione e metadati moduli
database/migrations/ schema MySQL versionato
docs/                parità, migrazione, Plesk e sicurezza
public/              unica webroot, asset e front controller
storage/             file privati non versionati
tests/               controlli strutturali
views/               interfaccia server-rendered
```

Copyright © 2026 Zen Software. Tutti i diritti riservati.
