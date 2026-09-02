# Luna2 API

Servizio centrale, indipendente dal gestionale, per le integrazioni esterne di Luna2. Il primo endpoint implementato risolve una partita IVA italiana tramite il provider Company di OpenAPI e restituisce il contratto normalizzato atteso da Luna2.

## Requisiti

- PHP 8.3 o 8.4 con `curl` e `json`
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

## Sicurezza

- Non inserire mai token reali nel repository.
- Usare token diversi per installazioni o gruppi di installazioni separati; `LUNA_API_TOKENS` accetta valori separati da virgola.
- Ruotare un token aggiungendo temporaneamente vecchio e nuovo valore, aggiornando i client e infine rimuovendo il vecchio.
- `APP_DEBUG` deve rimanere `false` in produzione.
- L’API non interroga né effettua scraping della pagina pubblica dell’Agenzia delle Entrate.

## Test locali

```bash
php tests/run.php
```

