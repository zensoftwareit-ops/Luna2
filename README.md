# Luna2 PHP

Riscrittura PHP di Luna2 per hosting Plesk, con MySQL 8 e una sola webroot `public/`.

Il branch PHP non richiede Java, Maven, Tomcat, JSP o un processo applicativo residente. Comprende già:

- autenticazione Argon2id, isolamento per azienda, CSRF, audit e security header;
- anagrafiche clienti/fornitori, prodotti, CRM, magazzini, commesse, calendario, HR, e-commerce e noleggio;
- preventivi, ordini, DDT, proforma, fatture attive/passive e note di credito con righe, numerazione e PDF;
- generazione XML FatturaPA e struttura per provider SDI accreditato;
- piano dei conti, prima nota Dare/Avere, contabilizzazione fatture, libro giornale, mastrini e bilancio di verifica;
- schema per IVA, scadenze fiscali, pagamenti, banche, riconciliazione e cespiti;
- import DATEV Koinos con staging, anteprima, idempotenza, log, quadrature e rollback per CSV/XLSX/XML/ZIP.
- pannello amministrativo per attivare o disattivare i moduli per azienda;
- diagnostica integrata per migrazioni, tabelle, estensioni PHP e permessi storage;
- log degli errori con codice di riferimento, senza esporre dettagli tecnici agli utenti.

Gli utenti `OWNER` e `ADMIN` trovano nel menu **Configurazione** le pagine **Gestione moduli** e **Stato del sistema**. Se lo schema non è completo, l’interfaccia indica la migrazione o la tabella mancante invece di mostrare un errore generico. I dettagli delle eccezioni sono salvati in `storage/logs/application-YYYY-MM-DD.log`.

## Stato del progetto

Questa è la nuova fondazione eseguibile, non ancora il via libera alla produzione del primo cliente. Le funzioni che richiedono contratti o dati esterni (canale SDI, conservazione a norma, OAuth calendario, credenziali marketplace e formato proprietario dell’archivio Koinos) sono deliberatamente separate. La matrice puntuale è in [docs/FUNCTIONAL_PARITY.md](docs/FUNCTIONAL_PARITY.md).

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
php bin/luna setup:admin "Ragione Sociale Srl" admin@example.it "una-password-lunga-e-unica"
```

Il comando `key:generate` stampa una riga `APP_KEY=...`: copiarla in `.env`. Non esistono credenziali predefinite.

Impostare la document root del dominio su `public/`. Se Plesk non consente di cambiare la document root, il file `.htaccess` nella radice inoltra le richieste a `public/` e blocca le cartelle private; la webroot dedicata resta la configurazione raccomandata.

Configurare un task pianificato giornaliero:

```bash
php /var/www/vhosts/example.it/luna2-php/bin/luna cron:daily
```

## Import DATEV Koinos

Aprire **Contabilità → Import DATEV Koinos**. Ogni caricamento crea un lotto immutabile con checksum. La conferma è separata dal caricamento; dopo la conferma è disponibile il rollback. Ordine consigliato:

1. clienti e fornitori;
2. piano dei conti;
3. XML FatturaPA attivi e passivi;
4. prima nota storica;
5. pagamenti/incassi;
6. saldi, cespiti, scadenze e movimenti bancari dopo la quadratura del campione.

Per l’archivio proprietario **Esporta archivio** serve almeno un export reale e anonimizzato del cliente. Il piano completo è in [docs/DATEV_KOINOS_MIGRATION.md](docs/DATEV_KOINOS_MIGRATION.md).

## SDI

L’applicazione genera XML FatturaPA, ma non simula un endpoint pubblico inesistente. La trasmissione deve avvenire tramite:

- provider/intermediario con API e accordo di servizio;
- canale Web Service/FTP preventivamente accreditato presso SdI;
- PEC o upload manuale, per volumi contenuti.

Il file `.env` contiene `SDI_DRIVER` e i parametri del provider; il connettore concreto va configurato dopo la scelta contrattuale.

## Test

```bash
composer test
```

Il test esegue il lint di tutti i file PHP, controlla che ogni modulo configurato abbia tabella e colonne nelle migrazioni e cerca credenziali accidentalmente versionate.

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
