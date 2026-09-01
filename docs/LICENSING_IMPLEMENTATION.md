# Licenze e autorizzazioni — stato implementazione

## Primo blocco integrato

Il primo blocco della roadmap introduce:

- policy automatica per tutte le route (`READ`, `WRITE`, `EXPORT`, `APPROVE`);
- permessi centralizzati e mappatura compatibile dei ruoli legacy;
- nuovi profili `MANAGER` e `OPERATOR`;
- identità persistente dell'installazione;
- archivio locale di licenza, moduli, sincronizzazioni e override;
- modalità progressive `shadow`, `warn`, `enforce`;
- distinzione fra moduli licenziati e moduli attivati dall'azienda;
- accesso di `OWNER` e `ADMIN` a dati aziendali, utenti e moduli;
- limite atomico degli utenti umani, con esclusione degli account tecnici;
- protezione dei cron e del webhook e-commerce;
- diagnostica tecnica della licenza nel pannello Stato del sistema e da CLI.

## Deploy del blocco

1. Lasciare nel file `.env`:

   ```dotenv
   LUNA_AUTHORIZATION_ENFORCEMENT=shadow
   LUNA_LICENSE_ENFORCEMENT=shadow
   ```

2. Eseguire da Plesk il comando già usato per le migrazioni:

   ```text
   bin/luna migrate
   ```

   Vengono applicate `010_license_authorization_foundation.sql`, `011_company_admin_user_limits.sql` e `012_license_lifecycle.sql`. La migrazione genera inoltre l'identità dell'installazione.

3. Verificare dalla pagina **Stato del sistema** oppure eseguire:

   ```text
   bin/luna license:status
   ```

4. Non impostare `enforce` finché non sono disponibili una licenza firmata e il servizio Luna Commerce.

In modalità `shadow` il comportamento operativo precedente rimane disponibile: le nuove policy vengono calcolate, mentre i vincoli commerciali non bloccano l'installazione.

## Prossimo blocco

- implementato nella versione 6.3: cifratura AES-256-GCM della chiave licenza;
- implementato nella versione 6.3: client API Luna Commerce con HMAC, nonce, timestamp e protezione replay;
- implementato nella versione 6.3: verifica Ed25519, sincronizzazione e cache firmata di sette giorni;
- implementato nella versione 6.3: pannello tecnico di attivazione, sync, revoca e diagnostica;
- implementato nella versione 6.3: storico degli entitlement per lettura/export dopo downgrade;

Passi ancora da sviluppare:

- implementazione degli endpoint WordPress secondo `docs/LICENSE_API_CONTRACT.md`;
- wizard persistente e riprendibile;
- conversione completa dei controlli residui dei controller in permessi dichiarativi.

## Configurazione versione 6.3

```dotenv
LUNA_LICENSE_API_URL=https://licenze.example.it
LUNA_LICENSE_PUBLIC_KEY=base64:<chiave-pubblica-ed25519>
LUNA_AUTHORIZATION_ENFORCEMENT=shadow
LUNA_LICENSE_ENFORCEMENT=shadow
```

Comandi disponibili:

```text
bin/luna license:status
bin/luna license:sync
bin/luna cron:license
```

Si consiglia di pianificare `cron:license` ogni ora. Il servizio effettua realmente la chiamata soltanto quando `next_sync_at` è scaduto; dopo un successo il controllo successivo è previsto dopo 12 ore, dopo un errore dopo un'ora.
