# Deploy Plesk

## Configurazione dominio

1. selezionare PHP 8.2+ con handler FPM;
2. impostare la document root su `luna2-php/public`;
3. abilitare HTTPS e redirect permanente da HTTP;
4. disabilitare directory listing;
5. impostare `display_errors=Off`, `log_errors=On`, `session.cookie_httponly=1`, `session.cookie_secure=1`;
6. impostare `upload_max_filesize` e `post_max_size` coerenti con `IMPORT_MAX_BYTES`;
7. abilitare le estensioni elencate nel README.

## Deploy da branch

Nel pannello Git di Plesk collegare `zensoftwareit-ops/Luna2` e selezionare il branch `luna2-php`. Dopo ogni pull eseguire come azioni aggiuntive:

```bash
composer install --no-dev --optimize-autoloader --no-interaction
php bin/luna storage:init
php bin/luna migrate
```

Il file `.env` viene creato una sola volta sul server e non è versionato. Le directory `storage/*` devono essere scrivibili dall’utente PHP ma non pubbliche.

## Primo avvio

```bash
cp .env.example .env
php bin/luna key:generate
php bin/luna migrate
php bin/luna setup:admin "Cliente Srl" amministrazione@cliente.it "password-unica-di-almeno-12-caratteri"
```

Completare poi anagrafica fiscale dell’organizzazione nel database/pannello amministrativo prima di generare XML.

## Cron

Task giornaliero:

```bash
php /percorso/luna2-php/bin/luna cron:daily
```

I worker per email, SDI, marketplace e calendari saranno aggiunti come task separati quando i provider sono configurati.

## Backup e rollback

- dump MySQL cifrato prima di ogni migrazione;
- copia di `.env` e `storage/private` nel backup protetto;
- almeno una prova mensile di ripristino;
- per rollback codice, selezionare il commit precedente in Plesk; non fare downgrade delle migrazioni senza procedura specifica;
- per cutover Koinos, conservare anche l’export originale con checksum.
