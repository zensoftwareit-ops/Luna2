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

Al termine accedere come `SUPERUSER` a **Stato del sistema**. La pagina deve mostrare tutte le migrazioni applicate e nessuna estensione o directory mancante. Gli errori applicativi sono registrati in `storage/logs/application-YYYY-MM-DD.log` con lo stesso codice di riferimento mostrato a video. Non abilitare `APP_DEBUG` in produzione.

## Primo avvio

```bash
cp .env.example .env
php bin/luna key:generate
php bin/luna migrate
```

L’output della prima `migrate` contiene le credenziali casuali del `SUPERUSER` e non le mostrerà una seconda volta. In Plesk eseguire quindi la migrazione tramite **Attività pianificate → Esegui uno script PHP**, salvare subito le credenziali mostrate nell’output e rimuovere l’attività temporanea.

Dopo il login aprire **Aziende e utenti** per:

1. creare e inizializzare l’azienda;
2. creare il primo `OWNER` e gli altri utenti aziendali;
3. sostituire la password iniziale del superuser;
4. aprire **Gestione moduli** e scegliere le aree disponibili per l’azienda.

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
