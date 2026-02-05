# Luna2 API Documentation

## Overview

Luna2 fornisce una serie di endpoint REST JSON per l'integrazione con sistemi esterni. Tutte le API richiedono autenticazione tramite session cookie o API key.

## Autenticazione

### Login
```
POST /api/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

Response:
{
  "success": true,
  "user": {
    "id": 1,
    "username": "admin",
    "nomeCompleto": "Amministratore",
    "email": "admin@example.com"
  },
  "token": "session-token"
}
```

## Clienti

### Lista Clienti
```
GET /api/clienti
Headers: Cookie: JSESSIONID=...

Response:
[
  {
    "id": 1,
    "codice": "CLI001",
    "ragioneSociale": "Acme Corp",
    "partitaIva": "12345678901",
    "email": "info@acme.com",
    "telefono": "+39 02 1234567"
  }
]
```

### Dettaglio Cliente
```
GET /api/clienti/{id}

Response:
{
  "id": 1,
  "codice": "CLI001",
  "ragioneSociale": "Acme Corp",
  "partitaIva": "12345678901",
  "codiceFiscale": "RSSMRA85M01H501Z",
  "indirizzo": "Via Roma 1",
  "cap": "20100",
  "citta": "Milano",
  "provincia": "MI",
  "email": "info@acme.com",
  "telefono": "+39 02 1234567"
}
```

### Crea Cliente
```
POST /api/clienti
Content-Type: application/json

{
  "codice": "CLI002",
  "ragioneSociale": "Test SRL",
  "partitaIva": "98765432109",
  "tipo": "AZIENDA",
  "email": "info@test.it"
}

Response:
{
  "success": true,
  "id": 2,
  "message": "Cliente creato con successo"
}
```

### Aggiorna Cliente
```
PUT /api/clienti/{id}
Content-Type: application/json

{
  "email": "nuovo@email.com",
  "telefono": "+39 02 9999999"
}

Response:
{
  "success": true,
  "message": "Cliente aggiornato con successo"
}
```

## Prodotti

### Lista Prodotti
```
GET /api/prodotti?categoria=hardware

Response:
[
  {
    "id": 1,
    "codice": "PROD001",
    "nome": "Mouse Wireless",
    "categoria": "Hardware",
    "prezzo": 29.99,
    "aliquotaIva": 22,
    "quantitaDisponibile": 150
  }
]
```

### Cerca Prodotti
```
GET /api/prodotti/search?q=mouse

Response:
[
  {
    "id": 1,
    "codice": "PROD001",
    "nome": "Mouse Wireless",
    "prezzo": 29.99
  }
]
```

## Ordini

### Crea Ordine
```
POST /api/ordini
Content-Type: application/json

{
  "clienteId": 1,
  "dataOrdine": "2024-01-15",
  "note": "Ordine urgente",
  "righe": [
    {
      "prodottoId": 1,
      "quantita": 10,
      "prezzo": 29.99,
      "sconto": 0
    }
  ]
}

Response:
{
  "success": true,
  "id": 123,
  "numero": "ORD-2024-001",
  "totale": 366.78
}
```

### Lista Ordini
```
GET /api/ordini?anno=2024&stato=CONFERMATO

Response:
[
  {
    "id": 123,
    "numero": "ORD-2024-001",
    "dataOrdine": "2024-01-15",
    "cliente": {
      "id": 1,
      "ragioneSociale": "Acme Corp"
    },
    "totale": 366.78,
    "stato": "CONFERMATO"
  }
]
```

## Fatture

### Lista Fatture
```
GET /api/fatture?anno=2024&mese=1

Response:
[
  {
    "id": 1,
    "numero": "FT-2024-001",
    "dataFattura": "2024-01-15",
    "cliente": {
      "id": 1,
      "ragioneSociale": "Acme Corp"
    },
    "imponibile": 300.00,
    "iva": 66.00,
    "totale": 366.00,
    "stato": "EMESSA"
  }
]
```

### Download PDF Fattura
```
GET /api/fatture/{id}/pdf

Response: Binary PDF file
Content-Type: application/pdf
```

## Magazzino

### Giacenze
```
GET /api/magazzino/giacenze

Response:
[
  {
    "prodotto": {
      "id": 1,
      "codice": "PROD001",
      "nome": "Mouse Wireless"
    },
    "quantita": 150,
    "ultimoAggiornamento": "2024-01-15T10:30:00"
  }
]
```

### Movimento di Carico
```
POST /api/magazzino/carico
Content-Type: application/json

{
  "prodottoId": 1,
  "quantita": 50,
  "causale": "Carico da fornitore",
  "dataMovimento": "2024-01-15"
}

Response:
{
  "success": true,
  "giacenzaAggiornata": 200
}
```

### Movimento di Scarico
```
POST /api/magazzino/scarico
Content-Type: application/json

{
  "prodottoId": 1,
  "quantita": 10,
  "causale": "Vendita a cliente",
  "documentoRiferimento": "ORD-2024-001"
}

Response:
{
  "success": true,
  "giacenzaAggiornata": 190
}
```

## Report

### Report Vendite
```
GET /api/report/vendite?dataInizio=2024-01-01&dataFine=2024-01-31

Response:
{
  "periodo": {
    "da": "2024-01-01",
    "a": "2024-01-31"
  },
  "totaleVendite": 15000.00,
  "numeroFatture": 25,
  "perCliente": [
    {
      "cliente": "Acme Corp",
      "totale": 5000.00,
      "numeroFatture": 5
    }
  ]
}
```

### Export Excel
```
GET /api/report/vendite/excel?dataInizio=2024-01-01&dataFine=2024-01-31

Response: Binary Excel file
Content-Type: application/vnd.ms-excel
Content-Disposition: attachment; filename="vendite_2024-01.xlsx"
```

## Codici di Errore

- `200 OK`: Operazione completata con successo
- `201 Created`: Risorsa creata con successo
- `400 Bad Request`: Dati non validi
- `401 Unauthorized`: Autenticazione richiesta
- `403 Forbidden`: Accesso negato
- `404 Not Found`: Risorsa non trovata
- `500 Internal Server Error`: Errore del server

## Esempi di Utilizzo

### cURL

```bash
# Login
curl -X POST http://localhost:8080/luna2/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt

# Lista clienti
curl -X GET http://localhost:8080/luna2/api/clienti \
  -b cookies.txt

# Crea ordine
curl -X POST http://localhost:8080/luna2/api/ordini \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{
    "clienteId": 1,
    "righe": [{"prodottoId": 1, "quantita": 10, "prezzo": 29.99}]
  }'
```

### JavaScript/Fetch

```javascript
// Login
const login = await fetch('/luna2/api/login', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({username: 'admin', password: 'admin123'})
});

// Lista prodotti
const prodotti = await fetch('/luna2/api/prodotti')
  .then(res => res.json());

// Crea cliente
const nuovoCliente = await fetch('/luna2/api/clienti', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    codice: 'CLI003',
    ragioneSociale: 'Nuovo Cliente SRL',
    partitaIva: '11223344556',
    tipo: 'AZIENDA'
  })
}).then(res => res.json());
```

### Python

```python
import requests

# Login
session = requests.Session()
login_data = {'username': 'admin', 'password': 'admin123'}
session.post('http://localhost:8080/luna2/api/login', json=login_data)

# Lista clienti
clienti = session.get('http://localhost:8080/luna2/api/clienti').json()

# Crea ordine
ordine = {
    'clienteId': 1,
    'righe': [
        {'prodottoId': 1, 'quantita': 10, 'prezzo': 29.99}
    ]
}
response = session.post('http://localhost:8080/luna2/api/ordini', json=ordine)
print(response.json())
```

## Rate Limiting

Le API sono soggette a rate limiting:
- 100 richieste per minuto per utente autenticato
- 10 richieste per minuto per IP non autenticato

## Webhook

Luna2 supporta webhook per notificare eventi esterni:

```json
{
  "event": "fattura.emessa",
  "timestamp": "2024-01-15T10:30:00",
  "data": {
    "fatturaId": 123,
    "numero": "FT-2024-001",
    "clienteId": 1,
    "totale": 366.00
  }
}
```

Eventi supportati:
- `cliente.creato`
- `ordine.creato`
- `ordine.confermato`
- `fattura.emessa`
- `pagamento.ricevuto`
- `magazzino.sottoscorta`

Configurare i webhook in `src/main/resources/webhook.properties`
