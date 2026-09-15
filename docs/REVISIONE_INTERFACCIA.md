# Revisione dell’interfaccia

## Intervento

- Terminologia operativa italiana nelle viste di documenti, contabilità, personale e integrazioni.
- Rimossi riferimenti di sviluppo e promesse di funzionalità future; mantenuti i limiti fiscali, le istruzioni sulle credenziali e gli avvisi sulle simulazioni paghe.
- Etichette di stato centralizzate in `View::label()`, senza cambiare codici nel database, valori dei form o classi CSS di stato. I codici sconosciuti restano visibili.
- Anteprima delle importazioni in campi leggibili, con dettagli strutturati espandibili e contenuti sempre escapati.
- Stati vuoti delle tabelle operative, allineamenti dei pannelli, moduli su schermi piccoli e pulsanti delle righe documento.
- Regole CSS nuove limitate allo schermo: nessuna modifica alle stampe fiscali.
- Versione degli asset incrementata a 6.4.0 per invalidare la cache del browser.

## Verifiche riproducibili

1. `php tests/run.php`: controlli generali e sintassi PHP.
2. `php tests/ui_review.php`: genera in `storage/private/ui-review` nove pagine dalle vere viste con dati di prova, senza database. Verifica rendering, etichette, valori originali dei form ed escaping.
3. `node tests/ui_browser.cjs`: richiede Playwright. La variabile facoltativa `LUNA_UI_BROWSER` indica l’eseguibile Chromium da usare; `NODE_PATH` può indicare i pacchetti installati. Verifica le pagine a 1440, 900 e 390 pixel, errori JavaScript, overflow di pagina e interazioni delle righe documento. Le richieste sono intercettate localmente, senza chiamate a servizi reali.

Il controllo browser usa fixture, non dati del cliente: non costituisce una verifica completa delle operazioni sulla sua installazione. I test non inviano e-mail, non importano dati e non modificano il database.

## Distribuzione

Dopo la pubblicazione del commit: pull e deploy del branch `luna2-php`. Per questo intervento non occorrono nuove migrazioni. Restano necessarie le migrazioni eventualmente ancora pendenti dalle versioni precedenti.
