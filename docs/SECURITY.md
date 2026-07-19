# Sicurezza operativa

## Azione immediata sul repository Java

La cronologia del branch `main` contiene credenziali applicative inserite nel codice/configurazione. Devono essere considerate compromesse anche se il repository è privato.

1. ruotare client secret OAuth Google, segreto state e chiave di cifratura calendario;
2. ruotare password database e qualunque token presente nei commit precedenti;
3. revocare le vecchie credenziali presso i provider;
4. valutare la riscrittura della cronologia Git dopo avere coordinato tutti i clone;
5. verificare log di accesso e utilizzo anomalo.

Luna2 PHP usa esclusivamente `.env`, non contiene password predefinite e ignora file sensibili.

## Controlli inclusi

- password Argon2id;
- rate limiting persistente sul login;
- prepared statement PDO senza emulazione;
- token CSRF su tutte le POST;
- rigenerazione session ID al login;
- cookie `HttpOnly`, `Secure` su HTTPS e `SameSite=Lax`;
- CSP, anti-framing, no-sniff, referrer e permissions policy;
- escaping HTML centralizzato;
- isolamento `organization_id`;
- audit degli accessi e delle modifiche;
- upload con whitelist, limiti, checksum, ZIP traversal e zip-bomb guard;
- import fuori webroot con anteprima, transazione e rollback;
- idempotenza per fonti esterne.

## Prima della produzione

- aggiungere MFA per amministratori e rate limiting dedicato agli endpoint di integrazione;
- cifrare con chiave applicativa le credenziali di integrazione;
- definire ruoli/permessi per singola azione, non solo profili generali;
- scansione dipendenze Composer, SAST e test OWASP;
- logging centralizzato senza dati fiscali sensibili;
- retention GDPR, registro trattamenti e accordi con responsabili esterni;
- backup cifrati, off-site, immutabili e testati;
- provider qualificato per conservazione a norma;
- accordo/canale SDI reale e gestione certificati;
- verifica del tracciato FatturaPA con XSD e casi fiscali del cliente;
- revisione di commercialista/consulente paghe per motori normativi.
