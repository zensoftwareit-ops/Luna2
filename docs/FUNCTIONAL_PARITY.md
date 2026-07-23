# Matrice di parità funzionale

Inventario sorgente: 61 entità Hibernate, 313 action Struts e 26 namespace applicativi del branch Java `main`.

Legenda: **Operativo** = workflow applicativo completo nella release PHP; **Validazione cliente** = implementato, ma da riconciliare sui dati reali; **Dipendenza esterna** = richiede contratto, credenziali o un export del fornitore.

| Area Luna2 Java / DATEV | Stato PHP | Evidenza principale |
|---|---|---|
| Accesso, ruoli, aziende, moduli, CSRF e audit | Operativo | Superuser separato, tenant filter, audit e pannello moduli riservato |
| Esperienza operativa | Operativo | Ricerca globale, notifiche, checklist, filtri, viste salvate, azioni massive e interfaccia responsive |
| Clienti, fornitori, prodotti, listini e CRM | Operativo | Anagrafiche, ricerca, import/export e pipeline |
| Preventivo → ordine → DDT → fattura | Operativo | Conversione guidata parziale/totale, quantità residue, collegamenti e numerazioni |
| Documenti, PDF, XML FatturaPA e stati | Operativo | Attivi/passivi, note credito, proforma e contabilizzazione |
| E-mail e tracking | Operativo / Dipendenza esterna | Coda SMTP/Sendmail, retry, allegati privati, aperture e clic; richiede account di posta |
| Endpoint SDI e conservazione | Dipendenza esterna | Endpoint configurabili e segreti ENV; adapter da collegare al futuro servizio API scelto |
| Piano dei conti e prima nota | Operativo | Piano gerarchico, causali, sezionali, automatismi e scritture quadrate |
| Giornale, mastrini e bilancio di verifica | Operativo | Filtri periodo, progressivi e controlli Dare/Avere |
| Stato patrimoniale e conto economico | Operativo / Validazione cliente | Riclassificazione civilistica/fiscale e confronto esercizi |
| Registri e liquidazioni IVA | Operativo / Validazione fiscale | IVA per cassa, esigibilità, detraibilità, pro-rata, rettifiche e blocchi |
| LIPE e raccordo IVA annuale | Operativo / Validazione fiscale | Prospetti di controllo non trasmissivi |
| Pagamenti, partite, banche e riconciliazione | Operativo | Allocazioni, insoluti, storni, import CSV/CAMT.053/MT940 e matching assistito controllato |
| Ratei, risconti, chiusura/apertura | Operativo / Validazione cliente | Assestamenti, blocco esercizio e riapertura patrimoniale |
| Cespiti e ammortamenti | Operativo / Validazione fiscale | Libro cespiti e quote civilistiche/fiscali |
| Magazzino atomico | Operativo | Saldi bloccati in transazione, no negativo, costo medio e movimenti idempotenti |
| Trasferimenti, picking e barcode | Operativo | Partenza/ricezione, impegni, scansioni, evasione e annullamenti con rilascio |
| Commesse | Operativo | Ore/spese/milestone, approvazione, margine e fattura da consuntivo |
| Ferie e approvazioni | Operativo | Saldi, sovrapposizioni, doppia approvazione vietata, presenze e annullamento |
| Payroll | Operativo con percorso controllato | Simulazioni separate; produzione solo da cedolini CSV certificati, quadrati e validati dal consulente |
| WooCommerce, Shopify, Amazon ed eBay | Operativo / Dipendenza esterna | Ordini, catalogo, mapping SKU, stock, prezzi, webhook e coda retry; servono credenziali e test account |
| Noleggio, ticket e scadenze | Operativo | Letture km, SLA, rinnovi e fatture ricorrenti idempotenti |
| Calendari Google, iCloud e CalDAV | Operativo / Dipendenza esterna | Sync bidirezionale, ETag e log; servono OAuth o password specifica app |
| Report direzionali XLSX | Operativo | Otto fogli, KPI, formule, controlli e grafico; archivio export privato |
| Stampe e fascicoli professionali | Operativo / Validazione fiscale | PDF numerati con hash e blocco; dossier fiscali controllati, non telematici |
| Import DATEV Koinos | Operativo per formati aperti | CSV/XLSX/ZIP, FatturaPA XML, staging, checksum, anteprima, quadrature e rollback |

## Cosa significa “pacchetto completo”

La release PHP contiene tutti i workflow applicativi sopra elencati e le migrazioni `008_full_erp_parity.sql` e `009_professional_workspace.sql`. Non sostituisce le attività che dipendono da terze parti o da responsabilità professionali:

1. gli adapter marketplace, calendario, SMTP e il futuro SDI vanno collaudati con credenziali reali;
2. un esercizio DATEV anonimizzato va importato e quadrato contro bilancio, IVA, partitari, banche e cespiti;
3. il payroll di produzione accetta soltanto valori provenienti dai cedolini certificati e richiede un riferimento di validazione professionale;
4. prima del cutover restano obbligatori backup/ripristino, test di sicurezza e UAT con responsabile amministrativo e consulenti.

La matrice contabile dettagliata è in [ACCOUNTING_PARITY.md](ACCOUNTING_PARITY.md); configurazione e collaudo dei nuovi workflow sono in [ERP_PARITY_RELEASE.md](ERP_PARITY_RELEASE.md).
