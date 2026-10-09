# Documentazione Luna2

Stato verificato sul branch `luna2-php` il **9 ottobre 2026**. Questo indice distingue le procedure operative correnti dai documenti storici o progettuali.

## Procedure operative correnti

| Documento | Quando usarlo |
|---|---|
| [GUIDA_OPERATIVA_CLIENTE.md](GUIDA_OPERATIVA_CLIENTE.md) | Manuale completo da consegnare agli utenti aziendali per l'uso quotidiano del software |
| [PLESK_DEPLOY.md](PLESK_DEPLOY.md) | Prima installazione, aggiornamento, cron, backup e rollback del codice |
| [PROCEDURA_IMPORT_DATEV_2025_2026.md](PROCEDURA_IMPORT_DATEV_2025_2026.md) | Migrazione specifica BASIC da DATEV, file per file e con quadrature attese |
| [PROCEDURA_OPERATIVA_MENSILE.md](PROCEDURA_OPERATIVA_MENSILE.md) | Fatture XML, registri IVA e controlli contabili dei mesi successivi al cutover |
| [PARTITARI_CLIENTI_FORNITORI.md](PARTITARI_CLIENTI_FORNITORI.md) | Sottoconti analitici, associazione automatica e riconciliazione dello storico |
| [DOMINI_PERSONALIZZATI_PLESK.md](DOMINI_PERSONALIZZATI_PLESK.md) | CNAME cliente, alias Plesk, certificato e diagnostica HTTPS |
| [STAMPE_FISCALI_SETUP.md](STAMPE_FISCALI_SETUP.md) | Stampe ufficiali, numerazione, validazione e fascicolo di evidenza |
| [RIPRISTINO_COMPLETO.md](RIPRISTINO_COMPLETO.md) | Azzeramento controllato dei dati applicativi |

## Riferimenti tecnici e di collaudo

- [ARCHITECTURE.md](ARCHITECTURE.md): architettura e invarianti applicativi.
- [ACCOUNTING_PARITY.md](ACCOUNTING_PARITY.md): copertura contabile e gate prima del cutover.
- [DATEV_KOINOS_MIGRATION.md](DATEV_KOINOS_MIGRATION.md): requisiti generali di una migrazione DATEV.
- [IMPORTAZIONE_ORIGINALI_KOINOS.md](IMPORTAZIONE_ORIGINALI_KOINOS.md): comportamento dell’acquisizione probatoria degli originali.
- [FUNCTIONAL_PARITY.md](FUNCTIONAL_PARITY.md): matrice generale delle funzioni.
- [ERP_PARITY_RELEASE.md](ERP_PARITY_RELEASE.md): collaudo applicativo esteso.
- [SECURITY.md](SECURITY.md): sicurezza operativa e attività prima della produzione.
- [LICENSE_API_CONTRACT.md](LICENSE_API_CONTRACT.md) e [LICENSING_IMPLEMENTATION.md](LICENSING_IMPLEMENTATION.md): contratto e implementazione licenze.

## Documenti progettuali

`ROADMAP_LICENZE_RUOLI_SETUP.md` e `REVISIONE_INTERFACCIA.md` descrivono decisioni e fasi di realizzazione. Non sostituiscono le procedure operative sopra elencate.

## Regola di aggiornamento

Ogni modifica che cambia menu, comandi Plesk, ordine di importazione, automatismi contabili o responsabilità dell’operatore deve aggiornare nello stesso commit almeno il documento operativo interessato. I numeri di migrazione citati nei documenti indicano l’origine della funzione: in produzione va sempre eseguito `php bin/luna migrate`, senza tentare una singola migrazione manualmente.
