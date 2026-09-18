# Domini personalizzati Luna2 su Plesk

La funzione consente al titolare o amministratore dell'azienda di pubblicare autonomamente l'istanza Luna2 su un proprio sottodominio. Luna2 predispone l'alias web tramite Plesk, restituisce il record CNAME esatto, verifica la propagazione DNS e controlla il certificato emesso e rinnovato da SSL It!.

## 1. Prerequisiti Plesk

1. Installare e attivare le estensioni **SSL It!** e **Let's Encrypt**.
2. Nel piano di servizio usato dalle istanze Luna2, attivare **Mantieni protetti i siti web** e includere gli alias di dominio nel certificato.
3. Creare in Plesk una chiave API amministrativa. Se possibile, limitarla all'indirizzo IP locale del server applicativo.
4. Il dominio tecnico dell'istanza deve già essere raggiungibile in HTTPS.

Luna2 non memorizza la chiave Plesk nel database e non la mostra nell'interfaccia.

## 2. Configurazione `.env`

```dotenv
PLESK_API_URL=https://plesk-host.example.it:8443
PLESK_API_KEY=incollare-qui-la-chiave-amministrativa
PLESK_API_VERIFY_TLS=true
PLESK_PRIMARY_DOMAIN=abc123.app.gestionaleluna.it
CUSTOM_DOMAIN_CNAME_TARGET=abc123.app.gestionaleluna.it
```

Usare per `PLESK_API_URL` un nome o indirizzo coperto da un certificato attendibile. Lasciare `PLESK_API_VERIFY_TLS=true`. `PLESK_PRIMARY_DOMAIN` identifica il webspace Plesk; se vuoto, Luna2 usa il dominio di `APP_URL`. `CUSTOM_DOMAIN_CNAME_TARGET` può essere omesso quando coincide con il dominio tecnico.

## 3. Database e controllo periodico

Dopo il pull:

```bash
/opt/plesk/php/8.4/bin/php /percorso/httpdocs/bin/luna migrate
```

Aggiungere in **Plesk > Operazioni pianificate** un'esecuzione ogni 5 minuti:

```bash
/opt/plesk/php/8.4/bin/php /percorso/httpdocs/bin/luna custom-domains:sync
```

Il comando è idempotente: ricontrolla DNS e certificato senza duplicare gli alias. Anche `cron:daily` esegue un controllo riepilogativo.

## 4. Procedura cliente

1. Il titolare o amministratore apre **Azienda > Dominio personalizzato**, inserisce per esempio `gestionale.cliente.it` e conferma.
2. Luna2 crea immediatamente l'alias web, senza posta e senza zona DNS, tramite l'API Plesk.
3. Luna2 mostra il record **CNAME** completo: nome scelto e destinazione tecnica dell'istanza.
4. Il cliente copia quel valore nella propria zona DNS.
5. Il controllo periodico riconosce la propagazione esclusivamente quando il CNAME porta all'istanza corretta.
6. SSL It! include l'alias nel certificato; Luna2 controlla hostname, catena e scadenza prima di mostrare lo stato **Attivo**.

La propagazione DNS e l'emissione del certificato possono richiedere alcuni minuti. Finché HTTPS non è valido, lo stato resta **SSL in attesa** con il dettaglio dell'ultimo controllo.

## 5. Sicurezza e rimozione

Solo gli utenti aziendali con ruolo **Titolare** o **Amministratore** possono creare, verificare o rimuovere domini. Il superuser di piattaforma non espone questo pannello nel proprio menu. La chiave Plesk rimane esclusivamente nel file `.env` e non viene mai mostrata al cliente. La rimozione elimina prima l'alias da Plesk e soltanto dopo il record Luna2. Gli eventi di attivazione restano consultabili finché il dominio è presente.
