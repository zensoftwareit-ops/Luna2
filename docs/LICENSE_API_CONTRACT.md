# Luna Commerce — Contratto API licenze Luna2 v1

## Endpoint

Base URL configurata in `LUNA_LICENSE_API_URL`, obbligatoriamente HTTPS.

- `POST /wp-json/luna/v1/licenses/activate`
- `POST /wp-json/luna/v1/licenses/validate`
- `POST /wp-json/luna/v1/licenses/deactivate`

## Autenticazione richiesta

Header:

```text
Content-Type: application/json
Accept: application/json
X-Luna-License: <chiave licenza>
X-Luna-Request-Id: <UUID v4>
X-Luna-Timestamp: <Unix timestamp>
X-Luna-Nonce: <48 caratteri esadecimali casuali>
X-Luna-Signature: <HMAC SHA-256 base64>
```

Stringa firmata con la chiave licenza:

```text
timestamp\nnonce\nMETHOD\n/path\nsha256-corpo-json
```

Il server deve:

- accettare una finestra temporale massima di 5 minuti;
- registrare nonce e request ID già consumati e rifiutarne il riuso;
- applicare rate limiting per chiave, IP e installazione;
- confrontare la firma in tempo costante;
- non registrare mai `X-Luna-License` nei log.

## Corpo richiesta

```json
{
  "request_id": "550e8400-e29b-41d4-a716-446655440000",
  "installation_uuid": "3d173e0d-a202-4c24-a1d9-bc995c03c6e5",
  "instance_id": "luna2-00000000000000000000",
  "domain": "app.example.it",
  "environment": "production",
  "software_version": "6.4.0-php"
}
```

## Risposta firmata

```json
{
  "request_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1785888000,
  "payload": {
    "request_id": "550e8400-e29b-41d4-a716-446655440000",
    "response_timestamp": 1785888000,
    "license_id": "lc_123456",
    "status": "ACTIVE",
    "plan_code": "PROFESSIONAL",
    "max_users": 10,
    "max_organizations": 1,
    "modules": ["accounting", "professional", "communications"],
    "installation_uuid": "3d173e0d-a202-4c24-a1d9-bc995c03c6e5",
    "domain": "app.example.it",
    "issued_at": "2026-08-05 10:00:00",
    "valid_from": "2026-08-05 00:00:00",
    "valid_until": "2027-08-04 23:59:59"
  },
  "signature": "<firma Ed25519 base64 del JSON canonico di payload>"
}
```

`request_id` e `response_timestamp` devono essere inclusi nel payload firmato. Luna2 rifiuta risposte nelle quali non coincidono con i valori esterni e con la richiesta corrente.

Il JSON canonico:

- ordina alfabeticamente le chiavi di ogni oggetto;
- conserva l'ordine degli array;
- usa UTF-8, senza escape delle barre o dei caratteri Unicode;
- non contiene spaziatura aggiuntiva.

## Stati

- `ACTIVE`
- `PAST_DUE`
- `SUSPENDED`
- `EXPIRED`
- `REVOKED`

L'endpoint di disattivazione restituisce comunque il payload completo firmato con stato `REVOKED`.

## Chiavi crittografiche

Il server firma con una chiave privata Ed25519 custodita esclusivamente lato Luna Commerce. Luna2 contiene soltanto la chiave pubblica, configurata come:

```dotenv
LUNA_LICENSE_PUBLIC_KEY=base64:<chiave-pubblica-32-byte>
```

La rotazione può essere introdotta aggiungendo un `key_id` firmato e un insieme di chiavi pubbliche ammesse. La prima versione usa `ED25519-V1`.

## Errori

Il server restituisce JSON senza dati sensibili:

```json
{
  "request_id": "550e8400-e29b-41d4-a716-446655440000",
  "error": "LICENSE_NOT_FOUND",
  "message": "Licenza non disponibile."
}
```

Codici HTTP consigliati: `400`, `401`, `403`, `404`, `409`, `422`, `429`, `500`, `503`.
