# Guida breve: accreditamento del canale SdI e collegamento Luna2

Questa guida riguarda il canale centrale `api.gestionaleluna.it`. Il primo rilascio abilita invio e ricezione di fatture tra privati (`FPR12`). L'invio verso Pubblica Amministrazione (`FPA12`) resta disabilitato con `SDI_ENABLE_PA_OUTBOUND=false` fino al relativo collaudo.

## 1. Endpoint da accreditare presso SdI

Nel portale di accreditamento scegliere il canale **SDICoop / Web Service**. Il canale espone due servizi distinti: uno riceve le fatture passive consegnate dallo SdI, l'altro riceve notifiche e ricevute relative alle fatture inviate.

### Ambiente di test/accreditamento

| Funzione | Endpoint SOAP | WSDL |
|---|---|---|
| Fatture passive | `https://api.gestionaleluna.it/test/sdi/ricezione-fatture` | `https://api.gestionaleluna.it/test/sdi/ricezione-fatture?wsdl` |
| Notifiche degli invii | `https://api.gestionaleluna.it/test/sdi/trasmissione-fatture` | `https://api.gestionaleluna.it/test/sdi/trasmissione-fatture?wsdl` |

### Ambiente di produzione

| Funzione | Endpoint SOAP | WSDL |
|---|---|---|
| Fatture passive | `https://api.gestionaleluna.it/sdi/ricezione-fatture` | `https://api.gestionaleluna.it/sdi/ricezione-fatture?wsdl` |
| Notifiche degli invii | `https://api.gestionaleluna.it/sdi/trasmissione-fatture` | `https://api.gestionaleluna.it/sdi/trasmissione-fatture?wsdl` |

Prima di iniziare verificare che tutti e quattro i WSDL siano raggiungibili via Internet con certificato HTTPS valido. I `POST` SOAP, invece, devono rifiutare con HTTP 403 le richieste prive del certificato client SdI.

Nella terminologia ufficiale:

- `SdIRiceviFile` è il servizio esposto dallo SdI al quale il nostro software invia le fatture;
- `TrasmissioneFatture` è esposto da noi e riceve dallo SdI scarti, consegne e altre notifiche;
- `RicezioneFatture` è esposto da noi e riceve le fatture passive destinate alle aziende collegate.

Riferimenti: [sistema di accreditamento](https://accreditamento.fatturapa.gov.it/) e [specifiche ufficiali SDICoop Trasmissione](https://www.fatturapa.gov.it/export/fatturazione/sdi/ws/trasmissione/v1.1/SDICoop_trasmissione_v1.1.pdf).

## 2. Capire i certificati

Servono elementi differenti:

1. **Certificato HTTPS del dominio**: quello normale di `api.gestionaleluna.it`, per esempio Let's Encrypt gestito da Plesk. Non è il certificato del canale SdI.
2. **Certificato client del canale e relativa chiave privata**: il nostro software li presenta quando chiama `SdIRiceviFile` dello SdI.
3. **CA del server SdI**: consente al nostro software di verificare il server remoto durante l'invio.
4. **CA dei client SdI**: viene configurata in nginx/Apache per verificare il certificato presentato dallo SdI quando chiama le nostre callback.

Test e produzione possono avere certificati, endpoint e CA differenti: non riutilizzare automaticamente quelli di test in produzione.

## 3. Caricare i certificati in Plesk

1. Creare una directory privata, fuori da `httpdocs`, per esempio:

   ```text
   /var/www/vhosts/gestionaleluna.it/private/sdi/test/
   /var/www/vhosts/gestionaleluna.it/private/sdi/production/
   ```

2. Caricare con File Manager/SFTP:

   ```text
   test/client.pem
   test/client.key
   test/server-ca.pem
   test/callback-client-ca.pem

   production/client.pem
   production/client.key
   production/server-ca.pem
   production/callback-client-ca.pem
   ```

3. I file devono essere leggibili dall'utente PHP del dominio ma non pubblici. Per la chiave privata usare permessi `600` o, se il gruppo PHP deve leggerla, `640`.

4. Per certificato e chiave PEM verificarne validità e corrispondenza:

   ```bash
   openssl x509 -in client.pem -noout -subject -issuer -dates
   openssl pkey -in client.key -check -noout
   openssl x509 -in client.pem -pubkey -noout | openssl pkey -pubin -outform der | openssl sha256
   openssl pkey -in client.key -pubout -outform der | openssl sha256
   ```

   Gli ultimi due hash devono coincidere.

5. Se viene consegnato un file P12/PFX, non è necessario estrarlo. Verificarlo con:

   ```bash
   openssl pkcs12 -in client.p12 -info -noout
   ```

   Conservare la password in Plesk/.env, mai nel repository.

6. Se le CA dei client SdI di test e produzione sono diverse, creare un unico bundle per nginx:

   ```bash
   cat test/callback-client-ca.pem production/callback-client-ca.pem > callback-client-ca-bundle.pem
   ```

## 4. Configurare l'API centrale

Nel `.env` del branch `luna2-api` configurare prima il test:

```env
APP_ENV=production
APP_DEBUG=false
APP_BASE_URL=https://api.gestionaleluna.it

SDI_STORAGE_KEY=CHIAVE_CASUALE_DI_ALMENO_32_CARATTERI
SDI_REQUIRE_MTLS=true
SDI_ENABLE_PA_OUTBOUND=false

SDI_TEST_ENDPOINT=URL_SDIRICEVIFILE_TEST_COMUNICATO_DA_SDI
SDI_TEST_CERT_PATH=/var/www/vhosts/gestionaleluna.it/private/sdi/test/client.pem
SDI_TEST_CERT_TYPE=PEM
SDI_TEST_KEY_PATH=/var/www/vhosts/gestionaleluna.it/private/sdi/test/client.key
SDI_TEST_CERT_PASSWORD=
SDI_TEST_CA_PATH=/var/www/vhosts/gestionaleluna.it/private/sdi/test/server-ca.pem
```

Terminato il collaudo, aggiungere la produzione:

```env
SDI_PRODUCTION_ENDPOINT=URL_SDIRICEVIFILE_PRODUZIONE_COMUNICATO_DA_SDI
SDI_PRODUCTION_CERT_PATH=/var/www/vhosts/gestionaleluna.it/private/sdi/production/client.pem
SDI_PRODUCTION_CERT_TYPE=PEM
SDI_PRODUCTION_KEY_PATH=/var/www/vhosts/gestionaleluna.it/private/sdi/production/client.key
SDI_PRODUCTION_CERT_PASSWORD=
SDI_PRODUCTION_CA_PATH=/var/www/vhosts/gestionaleluna.it/private/sdi/production/server-ca.pem
```

Per un P12/PFX usare:

```env
SDI_PRODUCTION_CERT_PATH=/percorso/client.p12
SDI_PRODUCTION_CERT_TYPE=P12
SDI_PRODUCTION_KEY_PATH=
SDI_PRODUCTION_CERT_PASSWORD=PASSWORD_DEL_P12
```

## 5. Configurare la verifica dei collegamenti in ingresso

In **Plesk → Apache & nginx Settings → Additional nginx directives** configurare la CA dei certificati client presentati dallo SdI. Il percorso seguente deve essere sostituito con quello reale:

Durante il solo collaudo il file può essere la CA client di test. Quando sono disponibili entrambi gli ambienti, usare il bundle test+produzione creato al punto precedente.

```nginx
ssl_client_certificate /var/www/vhosts/gestionaleluna.it/private/sdi/callback-client-ca-bundle.pem;
ssl_verify_client optional;
ssl_verify_depth 4;

# Se nginx inoltra ad Apache:
proxy_set_header X-SDI-Client-Verify $ssl_client_verify;

# Se nginx serve PHP-FPM direttamente:
fastcgi_param SSL_CLIENT_VERIFY $ssl_client_verify;
```

`optional` è necessario perché lo stesso dominio espone anche le API REST di Luna2; il codice rende il certificato obbligatorio soltanto sulle callback `/sdi/...` e `/test/sdi/...`.

Se viene usato l'header verso Apache:

```env
SDI_TRUST_PROXY_MTLS=true
SDI_TRUSTED_PROXY_IPS=127.0.0.1,::1
```

L'header deve sempre essere sovrascritto da nginx e non deve essere accettato direttamente da Internet.

Riavviare/ricaricare la configurazione del dominio. Una richiesta `POST` senza certificato a una callback deve restituire HTTP 403.

## 6. Verificare test e produzione

Ogni client Luna2 ha un token proprio. Nell'API centrale:

```env
LUNA_API_CLIENTS=basic-srl=TOKEN_BASIC_LUNGO_E_CASUALE
SDI_RECIPIENT_MAP=02572350185=basic-srl
```

Più aziende e più identificativi si separano con virgole:

```env
LUNA_API_CLIENTS=basic-srl=TOKEN_BASIC,cliente-due=TOKEN_DUE
SDI_RECIPIENT_MAP=02572350185=basic-srl,01234567890=cliente-due,RSSMRA80A01H501U=cliente-due
```

Verifica ambiente test:

```bash
curl -fsS -H "Authorization: Bearer TOKEN_BASIC" \
  https://api.gestionaleluna.it/test/v1/einvoices/readiness
```

Verifica produzione:

```bash
curl -fsS -H "Authorization: Bearer TOKEN_BASIC" \
  https://api.gestionaleluna.it/v1/einvoices/readiness
```

Entrambe devono restituire `"ready": true`. In produzione `pa_outbound_enabled` deve restare `false` durante il primo go-live.

## 7. Codice destinatario del nostro canale

Concluso l'accreditamento, SdI assegna al canale un **codice destinatario di 7 caratteri**. Per ricevere le fatture passive di ogni cliente:

1. entrare nell'area **Fatture e Corrispettivi** dell'azienda;
2. aprire **Registrazione dell'indirizzo telematico dove ricevere tutte le fatture elettroniche**;
3. registrare il codice destinatario di 7 caratteri assegnato al nostro canale;
4. in alternativa/comunque comunicarlo ai fornitori;
5. aggiungere partita IVA e, se utile, codice fiscale dell'azienda a `SDI_RECIPIENT_MAP`.

La registrazione dell'indirizzo prevale sul codice eventualmente scritto dal fornitore. Senza registrazione o comunicazione ai fornitori, le fatture non arriveranno necessariamente al nostro canale. [Guida ufficiale dell'Agenzia delle Entrate](https://www1.agenziaentrate.gov.it/web_app_entrate/fatturazione_elettronica.html/guide-gallery.html).

## 8. Collegare una singola istanza Luna2

### 8.1 Preparazione tecnica

Sul server dell'istanza:

```bash
git pull origin luna2-php
composer install --no-dev --optimize-autoloader
php bin/luna migrate
php bin/luna storage:init
```

Nel `.env` dell'istanza inserire il token assegnato a quella specifica azienda:

```env
LUNA_EINVOICE_TOKEN=TOKEN_BASIC_LUNGO_E_CASUALE
```

### 8.2 Configurazione di test in Luna2

In **Impostazioni → Fatturazione elettronica**:

| Campo | Valore test |
|---|---|
| Chiave servizio | `EINVOICE_SDI` |
| Ambiente | `TEST` |
| Base URL | `https://api.gestionaleluna.it/test` |
| Percorso invio | `/v1/einvoices/outbound` |
| Percorso ricezione | `/v1/einvoices` |
| Percorso stato | `/v1/einvoices/outbound/{id}` |
| Autenticazione | `BEARER` |
| Riferimento segreto | `ENV:LUNA_EINVOICE_TOKEN` |
| Verifica TLS | attiva |
| Configurazione abilitata | attiva |

Premere **Verifica collegamento**. Durante le prove lasciare disabilitata un'eventuale configurazione di produzione, perché Luna2 privilegia l'endpoint `PRODUCTION` quando entrambi sono attivi.

### 8.3 Passaggio alla produzione

Disabilitare il record di test e creare/abilitare:

| Campo | Valore produzione |
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

Premere nuovamente **Verifica collegamento**.

### 8.4 Sincronizzazione automatica

In Plesk aggiungere un'attività ogni cinque minuti. Se si sceglie **Esegui uno script PHP** impostare:

- percorso script: `httpdocs/bin/luna`;
- argomenti: `einvoice:sync`;
- versione PHP: la stessa del dominio, per esempio PHP 8.4.

Se invece si sceglie **Esegui un comando**, usare il percorso PHP reale del dominio:

```bash
/opt/plesk/php/8.4/bin/php /var/www/vhosts/DOMINIO/httpdocs/bin/luna einvoice:sync
```

Il job scarica notifiche e fatture passive; queste ultime vengono archiviate in **Importazioni** e richiedono anteprima/conferma prima della contabilizzazione.

## 9. Collaudo finale dell'istanza

1. Creare una fattura verso un privato con codice destinatario valido di 7 caratteri oppure `0000000` e PEC.
2. Portarla allo stato **Emessa**.
3. Aprirla e premere **Invia allo SdI**.
4. Controllare prima l'Identificativo SdI, poi ricevuta di consegna o mancata consegna.
5. Eseguire manualmente una volta `php bin/luna einvoice:sync`.
6. Verificare che una fattura passiva compaia in **Importazioni**, controllarla e confermarla.
7. Verificare che un destinatario PA di 6 caratteri venga bloccato.

Non aprire il canale ai clienti finché readiness, notifiche, ricezione passiva, backup di `storage/sdi` e backup di `SDI_STORAGE_KEY` non sono stati verificati.
