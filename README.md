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
- endpoint e-invoice configurabili senza memorizzare segreti né eseguire chiamate esterne;
- import DATEV Koinos con staging, anteprima, idempotenza, log, quadrature e rollback per CSV/XLSX/XML/ZIP.
- area `SUPERUSER` per creare aziende e utenti, reimpostare gli accessi e configurare i moduli;
- diagnostica integrata per migrazioni, tabelle, estensioni PHP e permessi storage;
- log degli errori con codice di riferimento, senza esporre dettagli tecnici agli utenti.

Solo il `SUPERUSER` di piattaforma vede **Aziende e utenti**, **Gestione moduli** e **Stato del sistema**. Gli utenti aziendali, inclusi `OWNER` e `ADMIN`, non possono modificare la composizione del gestionale. Se lo schema non è completo, l’interfaccia indica la migrazione o la tabella mancante invece di mostrare un errore generico. I dettagli delle eccezioni sono salvati in `storage/logs/application-YYYY-MM-DD.log`.

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
```

Il comando `key:generate` stampa una riga `APP_KEY=...`: copiarla in `.env`. Alla prima esecuzione, `migrate` crea anche il `SUPERUSER` e stampa email e password casuale una sola volta. Conservare subito le credenziali in un password manager; le esecuzioni successive non rigenerano né ristampano la password. Dopo il login il superuser crea l’azienda, gli utenti aziendali e la configurazione dei moduli interamente dall’interfaccia web.

Impostare la document root del dominio su `public/`. Se Plesk non consente di cambiare la document root, il file `.htaccess` nella radice inoltra le richieste a `public/` e blocca le cartelle private; la webroot dedicata resta la configurazione raccomandata.

Configurare un task pianificato giornaliero:

```bash
php /var/www/vhosts/example.it/luna2-php/bin/luna cron:daily
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

Gli URL del futuro servizio si configurano in **Contabilità → Endpoint e-invoice**. Le credenziali restano fuori dal database e vengono indicate soltanto tramite riferimenti `ENV:NOME_VARIABILE` o `vault://…`. Il connettore concreto sarà sviluppato dopo la scelta contrattuale.

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
