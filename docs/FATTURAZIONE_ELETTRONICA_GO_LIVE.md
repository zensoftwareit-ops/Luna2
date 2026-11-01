# Go-live fatturazione elettronica Luna2

Per la procedura completa di accreditamento, caricamento certificati e collegamento delle singole istanze vedere [GUIDA_ACCREDITAMENTO_CANALE_SDI.md](GUIDA_ACCREDITAMENTO_CANALE_SDI.md).

Questa procedura mette in produzione il canale per fatture e note di credito verso privati (`FPR12`), notifiche SdI e fatture passive. L'invio verso Pubblica Amministrazione (`FPA12`) deve restare disattivato fino al successivo collaudo dedicato.

## 1. Prerequisito non sostituibile dal software

Il canale SDICoop di produzione deve essere già accreditato e devono essere disponibili endpoint `RiceviFile`, certificato client, chiave privata e CA assegnati al canale. Senza questi elementi il codice è distribuibile, ma non può trasmettere realmente allo SdI.

## 2. Distribuzione API centrale

Distribuire `api.gestionaleluna.it` dal branch `luna2-api`, document root `public/`, PHP 8.3/8.4 con curl, DOM, libxml e OpenSSL. La directory `storage/sdi` deve essere scrivibile e inclusa nei backup cifrati.

Configurazione minima dell'API:

```env
APP_ENV=production
APP_DEBUG=false
APP_BASE_URL=https://api.gestionaleluna.it
LUNA_API_CLIENTS=basic-srl=TOKEN_CASUALE_DI_ALMENO_32_BYTE
SDI_STORAGE_KEY=CHIAVE_CASUALE_DISTINTA_DI_ALMENO_32_BYTE
SDI_RECIPIENT_MAP=02572350185=basic-srl
SDI_REQUIRE_MTLS=true
SDI_TRUST_PROXY_MTLS=true
SDI_TRUSTED_PROXY_IPS=127.0.0.1,::1
SDI_ENABLE_PA_OUTBOUND=false
SDI_PRODUCTION_ENDPOINT=URL_RICEVIFILE_ASSEGNATO_DA_SDI
SDI_PRODUCTION_CERT_PATH=/percorso/privato/certificato.pem
SDI_PRODUCTION_KEY_PATH=/percorso/privato/chiave.pem
SDI_PRODUCTION_CA_PATH=/percorso/privato/ca-sdi.pem
```

Non usare il token REST come chiave di cifratura. Conservare una copia sicura di `SDI_STORAGE_KEY`: senza di essa i payload archiviati non sono recuperabili.

Configurare mutual TLS su Plesk/nginx come descritto nel branch API in `docs/SDI_SETUP_PLESK.md`. Gli endpoint callback pubblici sono:

- `https://api.gestionaleluna.it/sdi/ricezione-fatture`
- `https://api.gestionaleluna.it/sdi/trasmissione-fatture`

## 3. Verifiche API prima di Luna2

```bash
curl -fsS https://api.gestionaleluna.it/health
curl -fsS -H "Authorization: Bearer TOKEN_CLIENTE" https://api.gestionaleluna.it/v1/einvoices/readiness
```

Il secondo comando deve restituire `"ready":true`. Il controllo `pa_outbound_enabled` deve essere `false` e non impedisce il go-live privati. Una chiamata senza certificato client alle callback `/sdi/...` deve ricevere HTTP 403.

## 4. Distribuzione gestionale Luna2

Dopo il pull del branch `luna2-php`:

```bash
composer install --no-dev --optimize-autoloader
php bin/luna migrate
php bin/luna storage:init
```

Nel `.env` dell'istanza:

```env
LUNA_EINVOICE_TOKEN=LO_STESSO_TOKEN_ASSOCIATO_A_BASIC_SRL_NELL_API
```

In **Impostazioni → Fatturazione elettronica** salvare:

| Campo | Valore |
|---|---|
| Chiave servizio | `EINVOICE_SDI` |
| Ambiente | `PRODUCTION` |
| Base URL | `https://api.gestionaleluna.it` |
| Percorso invio | `/v1/einvoices/outbound` |
| Percorso ricezione | `/v1/einvoices` |
| Percorso stato | `/v1/einvoices/outbound/{id}` |
| Autenticazione | `BEARER` |
| Riferimento segreto | `ENV:LUNA_EINVOICE_TOKEN` |
| Verifica TLS | attiva |
| Configurazione abilitata | attiva |

Premere **Verifica collegamento**: deve confermare “canale privati pronto; invio PA disattivato come previsto”.

## 5. Sincronizzazione automatica

In Plesk aggiungere un task ogni cinque minuti, eseguito dalla root dell'applicazione:

```bash
php bin/luna einvoice:sync
```

Il job:

1. acquisisce le notifiche e aggiorna lo stato delle fatture attive;
2. archivia localmente le fatture passive in un lotto di importazione;
3. invia l'ack all'API soltanto dopo che il lotto locale è stato creato correttamente.

Le fatture passive restano da verificare e confermare in **Importazioni**, evitando contabilizzazioni automatiche non controllate.

## 6. Collaudo smoke test prima del primo invio reale

1. Creare una fattura attiva di importo minimo verso un soggetto privato con codice destinatario di sette caratteri o `0000000` e PEC.
2. Portarla a stato **Emessa**.
3. Aprire la fattura e premere **Invia allo SdI** una sola volta.
4. Verificare la presenza di ID trasmissione e, dopo l'accettazione, Identificativo SdI.
5. Eseguire `php bin/luna einvoice:sync` e controllare che lo stato venga aggiornato.
6. Verificare ricezione di una fattura passiva, comparsa del lotto in **Importazioni**, anteprima e conferma.
7. Ripetere l'invio dello stesso documento: non deve generare una seconda trasmissione dello stesso XML.
8. Tentare con un cliente avente codice destinatario di sei caratteri: Luna2 deve bloccare l'invio PA.

## 7. Criteri di via libera

- readiness API positiva;
- certificati non scaduti e mutual TLS verificato;
- backup di database, `storage/sdi` e `SDI_STORAGE_KEY` verificato;
- smoke test FPR12 completato con notifica SdI;
- ricezione passiva completata e confermata;
- `SDI_ENABLE_PA_OUTBOUND=false` verificato;
- task `einvoice:sync` attivo e monitorato.

Se uno dei primi cinque punti manca, non inviare fatture reali: mantenere disponibile il download XML e correggere il prerequisito.

## 8. Rollback operativo

Disabilitare il canale `EINVOICE_SDI` dal pannello Luna2 e sospendere il task `einvoice:sync`. Non cancellare trasmissioni, notifiche o file cifrati già ricevuti. Ripristinare il codice solo dopo avere conservato database e storage; gli stati fiscali già assegnati dallo SdI non possono essere annullati da un rollback applicativo.
