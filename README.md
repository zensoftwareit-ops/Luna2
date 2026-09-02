# Luna2 API

Servizio centrale, indipendente dal gestionale, per le integrazioni esterne di Luna2. Comprende ricerca anagrafica da partita IVA e canale SDICoop per invio/ricezione di fatture elettroniche private e PA.

## Requisiti

- PHP 8.3 o 8.4 con `curl`, `dom`, `json`, `libxml` e `openssl`
- HTTPS
- Apache con `mod_rewrite` oppure document root impostata su `public/`
- account e token per [OpenAPI Company](https://console.openapi.com/it/apis/company/documentation)

Non è necessario eseguire Composer: il repository include un autoloader minimale. Composer può comunque essere usato per controllare i requisiti della piattaforma.

## Installazione su Plesk

1. Collegare `api.gestionaleluna.it` al branch orfano `luna2-api` e distribuire i file in `httpdocs`.
2. Impostare PHP 8.3 o 8.4 e abilitare `curl`.
3. Copiare `.env.example` in `.env` tramite File Manager di Plesk.
4. Generare un token lungo per `LUNA_API_TOKENS` e inserire il token del provider in `OPENAPI_TOKEN`.
5. Lasciare `OPENAPI_ENV=sandbox` durante le prove; impostare `production` solo dopo l’attivazione del credito/provider.
6. Assicurarsi che `storage/cache` sia scrivibile dall’utente PHP.
7. Visitare `https://api.gestionaleluna.it/health`.

Per il canale SdI seguire anche [la guida Plesk e accreditamento](docs/SDI_SETUP_PLESK.md).

Se il dominio usa `httpdocs` come document root, il front controller e il `.htaccess` presenti nella radice funzionano direttamente. È comunque preferibile impostare la document root su `httpdocs/public`; in tal caso copiare anche le regole di rewrite o abilitare il fallback al front controller.

## Configurazione nel gestionale Luna2

Nel pannello superuser, configurare l’endpoint `VAT_LOOKUP`:

| Campo | Valore |
|---|---|
| Base URL | `https://api.gestionaleluna.it` |
| Percorso ricezione | `/v1/vat-lookup` |
| Autenticazione | `BEARER` |
| Riferimento segreto | `ENV:LUNA_VAT_LOOKUP_TOKEN` |
| Stato | Attivo |

In Plesk, sull’installazione del gestionale, `LUNA_VAT_LOOKUP_TOKEN` deve avere lo stesso valore presente tra i token `LUNA_API_TOKENS` dell’API.

## Endpoint

### Health check

```http
GET /health
```

Non richiede autenticazione e non espone configurazioni o credenziali.

### Ricerca partita IVA

```http
GET /v1/vat-lookup?country=IT&vat_number=12485671007
Authorization: Bearer <token-luna>
```

Risposta:

```json
{
  "business_name": "OPENAPI S.P.A.",
  "tax_code": "12485671007",
  "vat_number": "12485671007",
  "address": "VIALE F TOMMASO MARINETTI 221",
  "postal_code": "00143",
  "city": "ROMA",
  "province": "RM",
  "country_code": "IT",
  "status": "ACTIVE",
  "sdi_code": "USAL8PV",
  "provider_updated_at": "2024-05-11T20:43:21+00:00",
  "request_id": "..."
}
```

Le risposte positive vengono conservate in cache per sette giorni per ridurre costi e latenza. Il limite predefinito è 60 richieste al minuto per token e indirizzo IP.

### Fatturazione elettronica

Gli endpoint REST usati dalle installazioni Luna2 sono disponibili sia in produzione sia con prefisso `/test`:

| Metodo | Produzione | Collaudo |
|---|---|---|
| POST | `/v1/einvoices/outbound` | `/test/v1/einvoices/outbound` |
| GET | `/v1/einvoices?direction=inbound` | `/test/v1/einvoices?direction=inbound` |
| GET | `/v1/einvoices?direction=outbound` | `/test/v1/einvoices?direction=outbound` |
| GET | `/v1/einvoices?direction=notifications` | `/test/v1/einvoices?direction=notifications` |
| POST | `/v1/einvoices/{direzione}/{id}/ack` | `/test/v1/einvoices/{direzione}/{id}/ack` |

L’invio accetta `multipart/form-data` con campo `invoice` oppure JSON:

```json
{
  "filename": "IT01234567890_00001.xml",
  "content_base64": "...",
  "recipient_type": "PA"
}
```

`recipient_type=PA` impone un tracciato `FPA12` e un codice ufficio di sei caratteri. Il sistema riconosce anche il normale tracciato privati `FPR12`.

Gli endpoint SOAP chiamati direttamente da SdI sono:

| Funzione | Produzione | Collaudo |
|---|---|---|
| Fatture passive | `/sdi/ricezione-fatture` | `/test/sdi/ricezione-fatture` |
| Notifiche invii | `/sdi/trasmissione-fatture` | `/test/sdi/trasmissione-fatture` |

I contratti sono disponibili aggiungendo `?wsdl`; gli XSD aggiungendo `?xsd=1`.

I due ambienti usano endpoint SdI, certificati e storage logicamente separati. Tutti i payload vengono cifrati a riposo mediante AES-256-GCM.

## Sicurezza

- Non inserire mai token reali nel repository.
- Usare token diversi per installazioni o gruppi di installazioni separati; `LUNA_API_TOKENS` accetta valori separati da virgola.
- Ruotare un token aggiungendo temporaneamente vecchio e nuovo valore, aggiornando i client e infine rimuovendo il vecchio.
- `APP_DEBUG` deve rimanere `false` in produzione.
- Lasciare `SDI_REQUIRE_MTLS=true` dopo la configurazione dei certificati del canale.
- Non riutilizzare `SDI_STORAGE_KEY` come token client e conservarne una copia nel gestore segreti: senza tale chiave i documenti archiviati non sono recuperabili.
- L’API non interroga né effettua scraping della pagina pubblica dell’Agenzia delle Entrate.

## Test locali

```bash
php tests/run.php
```
