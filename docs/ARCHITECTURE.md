# Architettura Luna2 PHP

## Principi

Luna2 PHP è una monolite modulare: un solo deploy Plesk, un solo database per installazione, confini applicativi chiari. Questa scelta riduce gestione e costi rispetto a Tomcat/Struts, senza concentrare la logica fiscale nelle viste.

- `public/index.php` è l’unico punto di ingresso HTTP.
- `Router` applica autenticazione e CSRF prima dei controller.
- i controller validano il perimetro `organization_id` per evitare accessi tra aziende;
- i servizi contengono logica con transazioni: contabilità, FatturaPA e import;
- le viste non eseguono query e fanno escaping dell’output;
- le migrazioni sono SQL esplicito e rieseguibile;
- file, import e credenziali non sono mai nella webroot.

## Modello dati

Il modello unifica documenti commerciali in `documents` + `document_lines`, mantenendo tipi e workflow distinti. Le scritture contabili sono `journal_entries` + `journal_entry_lines`; ogni riga accetta solo Dare oppure Avere e il servizio rifiuta registrazioni non quadrate.

Tutte le tabelle operative hanno `organization_id`. Gli identificativi esterni usano chiavi uniche per rendere import e sincronizzazioni idempotenti. Gli aggiornamenti significativi confluiscono in `audit_logs`.

## Integrazioni

Le integrazioni sono adapter, non dipendenze del dominio:

- SDI/provider: XML, checksum, trasmissione e notifiche restano separati;
- Google/iCloud: token cifrati in `calendar_accounts`;
- WooCommerce, Shopify, Amazon ed eBay: credenziali cifrate e log di sincronizzazione;
- DATEV Koinos: file originali, righe di staging, mapping, record creati/aggiornati e rollback.

## Vincoli contabili

- un documento emesso/ricevuto viene contabilizzato una sola volta grazie a `source_type + source_id`;
- i protocolli sono assegnati dentro transazione e bloccati con `SELECT … FOR UPDATE`;
- una registrazione deve avere almeno due righe e totale Dare = totale Avere al centesimo;
- correzioni e chiusure devono produrre scritture, non modificare silenziosamente il passato;
- gli esercizi chiusi e la conservazione documentale richiederanno un blocco applicativo nel gate di produzione.

## Estensioni previste

I workflow non ancora completi si aggiungono come servizi e controller dedicati. I CRUD configurabili sono utili per le anagrafiche, ma non sostituiscono servizi per liquidazioni IVA, ammortamenti, riconciliazione, payroll, SDI e marketplace.
