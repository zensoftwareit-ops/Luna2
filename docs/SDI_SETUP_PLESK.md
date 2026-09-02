# Configurazione SDICoop, Plesk e accreditamento PA

## Indirizzi da comunicare a SdI

Produzione:

- ricezione fatture passive: `https://api.gestionaleluna.it/sdi/ricezione-fatture`
- ricezione notifiche degli invii: `https://api.gestionaleluna.it/sdi/trasmissione-fatture`

Collaudo/accreditamento:

- ricezione fatture: `https://api.gestionaleluna.it/test/sdi/ricezione-fatture`
- ricezione notifiche: `https://api.gestionaleluna.it/test/sdi/trasmissione-fatture`

I WSDL sono disponibili con `?wsdl`. L’ambiente `/test` usa configurazione, certificati e archivio separati dalla produzione e può quindi essere usato per le prove di interoperabilità PA senza contaminare i dati reali.

## Certificati

Caricare mediante File Manager di Plesk, fuori dalle directory pubbliche:

- certificato client del canale per il collegamento verso SdI;
- relativa chiave privata, oppure contenitore P12/PFX;
- catena CA indicata nel pacchetto di accreditamento;
- CA utilizzata per verificare il certificato client presentato da SdI alle nostre callback.

La cartella `certificates/` del progetto è esclusa da Git, ma è preferibile usare una directory privata del dominio. I file devono essere leggibili dall’utente PHP e non dal pubblico.

## Variabili applicative

```env
APP_BASE_URL=https://api.gestionaleluna.it
LUNA_API_CLIENTS=cliente-1=TOKEN_LUNGO_CLIENTE_1
SDI_RECIPIENT_MAP=PARTITA_IVA_CLIENTE_1=cliente-1
SDI_STORAGE_KEY=SEGRETO_CASUALE_DI_ALMENO_32_CARATTERI
SDI_REQUIRE_MTLS=true

SDI_PRODUCTION_ENDPOINT=URL_RICEVIFILE_PRODUZIONE_FORNITO_DA_SDI
SDI_PRODUCTION_CERT_PATH=/percorso/privato/cert-produzione.pem
SDI_PRODUCTION_CERT_TYPE=PEM
SDI_PRODUCTION_KEY_PATH=/percorso/privato/key-produzione.pem
SDI_PRODUCTION_CERT_PASSWORD=
SDI_PRODUCTION_CA_PATH=/percorso/privato/ca-sdi-produzione.pem

SDI_TEST_ENDPOINT=URL_RICEVIFILE_TEST_FORNITO_DA_SDI
SDI_TEST_CERT_PATH=/percorso/privato/cert-test.pem
SDI_TEST_CERT_TYPE=PEM
SDI_TEST_KEY_PATH=/percorso/privato/key-test.pem
SDI_TEST_CERT_PASSWORD=
SDI_TEST_CA_PATH=/percorso/privato/ca-sdi-test.pem
```

Per un P12/PFX impostare `CERT_TYPE=P12`, indicare il contenitore in `CERT_PATH`, lasciare vuoto `KEY_PATH` e valorizzare `CERT_PASSWORD`.

## Mutual TLS dietro il proxy Plesk

Il certificato client di SdI deve essere validato dal web server, non soltanto da PHP. Quando nginx termina TLS davanti ad Apache, nelle direttive nginx aggiuntive del dominio configurare la CA ricevuta con il pacchetto SdI, richiedere il certificato in modalità opzionale a livello di virtual host e trasmettere ad Apache soltanto l’esito calcolato da nginx:

```nginx
ssl_client_certificate /percorso/privato/ca-client-sdi.pem;
ssl_verify_client optional;
ssl_verify_depth 4;
proxy_set_header X-SDI-Client-Verify $ssl_client_verify;
```

La modalità è `optional` perché sullo stesso dominio esistono endpoint REST usati da Luna2. L’applicazione rende comunque il certificato obbligatorio esclusivamente sulle route `/sdi/...` e `/test/sdi/...`.

Configurare:

```env
SDI_TRUST_PROXY_MTLS=true
SDI_TRUSTED_PROXY_IPS=127.0.0.1,::1
```

Se PHP riceve TLS direttamente, lasciare `SDI_TRUST_PROXY_MTLS=false`: verrà usata la variabile server `SSL_CLIENT_VERIFY`.

Non impostare mai dall’esterno l’header `X-SDI-Client-Verify`; deve essere sovrascritto dal proxy. Dopo la configurazione, una richiesta senza certificato agli endpoint SOAP deve ricevere HTTP 403.

## Flussi implementati

### Invio

1. Luna2 invia XML o P7M all’API REST.
2. L’API verifica nome file, leggibilità, formato FPR/FPA e codice destinatario.
3. Il file viene cifrato e archiviato.
4. L’API effettua la chiamata SOAP/MTOM `RiceviFile` verso SdI usando mutual TLS.
5. Identificativo SdI e data di ricezione vengono associati alla trasmissione.

### Notifiche

Sono gestite e archiviate:

- ricevuta di consegna;
- mancata consegna;
- scarto;
- esito committente PA;
- decorrenza termini;
- attestazione di impossibilità di recapito.

La notifica aggiorna automaticamente lo stato della trasmissione collegata tramite `IdentificativoSdI`.

### Fatture passive

La callback `RiceviFatture` accetta SOAP base64 e MTOM, conserva file e metadati e risponde `ER01` soltanto dopo l’archiviazione. Il destinatario viene individuato dalla partita IVA/codice fiscale della fattura e dalla mappa `SDI_RECIPIENT_MAP`. Un destinatario non censito viene conservato nell’area `unmatched` con stato `ROUTING_REQUIRED`, evitando perdita del documento.

## Prove PA

Per l’invio verso Pubblica Amministrazione l’API applica queste verifiche aggiuntive:

- `FormatoTrasmissione=FPA12`;
- codice ufficio destinatario di sei caratteri;
- gestione esito committente `EC01`/`EC02`;
- gestione decorrenza termini e attestazione.

Durante la procedura di accreditamento utilizzare soltanto gli URL sotto `/test` e configurare `SDI_TEST_*` con endpoint e certificati del simulatore/collaudo.

Il pacchetto WSDL/XSD generato dalla procedura di accreditamento è la fonte contrattuale definitiva. I contratti inclusi nel progetto seguono le interfacce SDICoop 1.0/1.1; quando sarà disponibile il pacchetto ufficiale associato al canale, confrontarlo o caricarlo nel progetto prima del test finale.

Riferimenti ufficiali:

- [Sistema di accreditamento SdI](https://accreditamento.fatturapa.gov.it/)
- [Specifiche SDICoop Trasmissione](https://www.fatturapa.gov.it/export/fatturazione/sdi/ws/trasmissione/v1.1/SDICoop_trasmissione_v1.1.pdf)
- [Elenco dei controlli effettuati da SdI](https://www.fatturapa.gov.it/export/documenti/Elenco-Controlli-versione-1.7.pdf)

## Checklist prima del collaudo

- HTTPS valido e raggiungibile pubblicamente.
- PHP 8.3/8.4 con curl, DOM, libxml e OpenSSL.
- `storage/sdi` scrivibile e incluso nei backup.
- chiave di storage salvata anche nel gestore segreti.
- certificato e CA di test caricati.
- mutual TLS verificato.
- WSDL di test raggiungibili.
- una fattura privata FPR12 di prova.
- una fattura PA FPA12 con codice ufficio di sei caratteri.
- verifica di ricezione scarto, consegna, esito PA e decorrenza termini.
