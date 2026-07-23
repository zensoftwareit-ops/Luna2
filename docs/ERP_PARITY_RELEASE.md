# Release ERP completa

## Attivazione

Dopo il pull del branch `luna2-php`, eseguire da **Plesk → Siti web e domini → Attività pianificate → Esegui ora**:

```text
/opt/plesk/php/8.4/bin/php /var/www/vhosts/DOMINIO/httpdocs/bin/luna migrate
```

Le migrazioni `008_full_erp_parity.sql` e `009_professional_workspace.sql` sono incrementali: non cancellare il database esistente. La `009` aggiunge centro professionale, notifiche, viste salvate, checklist, stampe controllate e import bancari. Poi aprire **Stato del sistema** e verificare che tutte le migrazioni risultino applicate.

Il task ricorrente consigliato, ogni 5 minuti, è:

```text
/opt/plesk/php/8.4/bin/php /var/www/vhosts/DOMINIO/httpdocs/bin/luna cron:daily
```

Il comando è idempotente e gestisce code e-mail/e-commerce, calendari, SLA, fatture ricorrenti e notifiche operative. Il nome storico `cron:daily` non impedisce una frequenza più alta.

## Centro professionale

L’area è disponibile a `OWNER`, `ADMIN` e `ACCOUNTANT` quando i moduli **Contabilità** e **Centro professionale** sono attivi. Comprende:

- controlli di qualità e collegamento alla checklist di avviamento;
- libro giornale, bilancio di verifica, registri IVA e registro cespiti in PDF numerato con checksum;
- validazione esplicita e blocco delle stampe;
- dossier JSON di controllo per F24, Intrastat, CU, 770, IVA annuale, LIPE, XBRL, Redditi e IRAP;
- import estratti conto CSV, CAMT.053 e MT940;
- suggerimenti di riconciliazione basati su importo, data, controparte e riferimento.

I dossier fiscali sono artefatti di controllo: non sono file telematici ministeriali e non effettuano invii. Lo stato `SUBMITTED` registra esclusivamente un invio compiuto tramite un endpoint esterno configurato.

## Segreti degli adapter

I campi `secret_reference` contengono esclusivamente `ENV:NOME_VARIABILE`. Il valore della variabile è JSON e va configurato in Plesk, mai nel database.

| Provider | JSON segreto | Impostazioni JSON canale |
|---|---|---|
| WooCommerce | `{"consumer_key":"...","consumer_secret":"...","webhook_secret":"..."}` | `{}` |
| Shopify | `{"access_token":"...","webhook_secret":"..."}` | `{"api_version":"2026-07","location_id":"gid://shopify/Location/..."}` |
| eBay | `{"access_token":"...","webhook_secret":"..."}` | `{"marketplace_id":"EBAY_IT","locale":"it-IT","currency":"EUR"}` |
| Amazon SP-API | `{"refresh_token":"...","client_id":"...","client_secret":"..."}` | `{"marketplace_id":"APJ6JRA9NG5V4","seller_id":"...","currency":"EUR","product_type":"PRODUCT"}` |
| Google Calendar | `{"refresh_token":"...","client_id":"...","client_secret":"..."}` | configurazione dall’interfaccia |
| iCloud / CalDAV | `{"username":"...","password":"..."}` | endpoint HTTPS e password specifica app |

Per Amazon Europa usare normalmente `https://sellingpartnerapi-eu.amazon.com`; per Shopify la base è il dominio `https://negozio.myshopify.com`; per eBay produzione `https://api.ebay.com`.

## Payroll di produzione

`CONFIGURED_RATES` genera solo simulazioni gestionali e non può essere chiuso o pagato. Il percorso ammesso in produzione è:

1. creare l’elaborazione `IMPORTED_PAYSLIPS`;
2. importare CSV con `employee_code,gross,employee_contributions,employer_contributions,tax,reimbursements,deductions,net`;
3. correggere tutte le righe scartate e la quadratura del netto;
4. inserire il riferimento della validazione del consulente;
5. bloccare l’elaborazione e, successivamente, marcarla pagata.

Il file accetta separatore virgola o punto e virgola e numeri sia italiani sia internazionali.

## Collaudo prima del cutover

- completare una conversione preventivo → ordine → DDT → fattura, anche parziale;
- creare e annullare un picking, completarne un secondo e verificare giacenza/impegno;
- fatturare ore, spese e milestone di una commessa;
- verificare invio, apertura e clic di una e-mail di prova;
- provare approvazione e annullamento ferie su più giorni;
- eseguire pull ordini/catalogo e push stock/prezzo in sandbox per ogni marketplace usato;
- verificare una fattura ricorrente noleggio e un superamento SLA;
- esportare e riaprire il report direzionale XLSX;
- sincronizzare un evento in entrambe le direzioni;
- importare un cedolino campione e verificarne la quadratura.
- generare, validare, scaricare e bloccare una stampa contabile campione;
- creare un dossier fiscale, risolverne le anomalie e provarne il workflow fino a `READY`;
- importare un estratto conto campione, verificare duplicati e accettare/scartare le proposte di riconciliazione;
- controllare ricerca globale, filtri, viste salvate, operazioni massive e responsive su desktop/mobile.
