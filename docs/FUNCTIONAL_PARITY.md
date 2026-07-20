# Matrice di parità funzionale

Inventario sorgente: 61 entità Hibernate, 313 action Struts e 26 namespace applicativi nel branch Java `main`.

Legenda: **Operativo** = flusso utilizzabile nella base PHP; **Base pronta** = schema/UI presenti, workflow specialistico da completare; **Dipendenza esterna** = richiede contratto, credenziali o file campione.

| Area Luna2 Java / DATEV | Stato PHP | Note e gate |
|---|---|---|
| Login, ruoli, aziende, CSRF, audit | Operativo | Argon2id, session cookie sicuro, tenant filter e audit |
| Dashboard KPI | Operativo | Clienti, documenti, crediti/debiti, scadenze, scorte |
| Clienti e fornitori | Operativo | CRUD, export CSV, import CSV/XLSX Koinos |
| Prodotti e listini | Operativo / Base pronta | Catalogo operativo; listini multipli nello schema |
| CRM lead, pipeline, attività e task | Operativo / Base pronta | CRUD operativo; automazioni e conversione guidata da completare |
| Preventivi | Operativo | Righe, totali, IVA, PDF, stati |
| Ordini clienti/fornitori | Operativo | Righe, totali e stati; evasione automatica da completare |
| DDT | Operativo / Base pronta | Creazione e PDF; conversione guidata e metadati trasporto avanzati da completare |
| Proforma, fatture attive e note credito | Operativo | Numerazione, righe, PDF, XML FatturaPA, contabilizzazione |
| Fatture passive | Operativo | Inserimento/import XML, scadenza e contabilizzazione |
| Invio email e tracking | Base pronta | Tabelle presenti; coda SMTP e pixel/link firmati da completare |
| SDI invio/ricezione/notifiche | Base pronta / Dipendenza esterna | Endpoint test/produzione configurabili senza chiamate o segreti; connettore e servizio API da sviluppare dopo la scelta del canale |
| Conservazione a norma | Dipendenza esterna | Necessario provider/accordo di conservazione |
| Pagamenti, rate e acconti | Operativo | Partite, scadenze, incassi/pagamenti, allocazione, aggiornamento saldo e storno controllato |
| Piano dei conti e prima nota | Operativo | Gerarchia, classificazione civilistica/fiscale, causali, sezionali, mappature, bozze e scritture manuali/automatiche quadrate |
| Libro giornale, mastrini, bilancio verifica | Operativo | Filtri periodo, progressivo mastrino e totali Dare/Avere |
| Conto economico e stato patrimoniale | Operativo / validazione cliente | Riclassificazione dal piano dei conti e saldi per esercizi anche non solari; stampa ufficiale da validare |
| Registri IVA e liquidazioni | Operativo / validazione fiscale | Sezionali, regimi, esigibilità, IVA per cassa, detraibilità, pro-rata, rettifiche, liquidazioni e raccordi LIPE/annuale non telematici |
| Ratei, risconti, chiusura/apertura | Operativo / validazione cliente | Assestamenti, controlli, chiusura economica, risultato, apertura patrimoniale e blocco esercizio |
| Cespiti e ammortamenti | Operativo / validazione fiscale | Categorie, valori civilistici/fiscali, primo anno, quote e scritture automatiche |
| Banche e riconciliazione | Operativo manuale | Conti, movimenti, matching parziale/totale e annullamento; open banking resta successivo |
| Scadenzario fiscale | Operativo | CRUD, dashboard e aggiornamento automatico scaduti |
| Magazzini e movimenti | Operativo / Base pronta | CRUD; aggiornamento atomico giacenze, trasferimenti/picking/barcode da completare |
| Commesse | Operativo / Base pronta | CRUD e budget; avanzamento/fatturazione/consuntivo da completare |
| Calendari Google/iCloud | Base pronta / Dipendenza esterna | Eventi locali operativi; OAuth/CalDAV richiedono credenziali |
| Presenze e approvazioni | Operativo / Base pronta | CRUD; workflow ferie e approvazioni da completare |
| Payroll | Base pronta | Non usare in produzione senza motore normativo verificato e consulente paghe |
| e-commerce Woo/Shopify/Amazon/eBay | Base pronta / Dipendenza esterna | Canali, ordini, prodotti e log; adapter API/webhook da completare |
| Noleggio, ticket e scadenze | Operativo / Base pronta | Contratti e ticket CRUD; automazioni SLA/km/revisioni da completare |
| Report PDF/CSV/XLSX | Operativo / Base pronta | PDF documenti e CSV CRUD; report direzionali/XLSX da completare |
| Import DATEV Koinos | Operativo per formati aperti | CSV/XLSX/ZIP per piano conti, anagrafiche, prima nota, IVA, partite, pagamenti, cespiti e banca; XML FatturaPA; archivio proprietario richiede campione |

## Gate prima del primo cliente

1. ricevere l’elenco delle funzioni DATEV effettivamente usate e un export anonimizzato;
2. scegliere provider SDI e conservazione a norma;
3. completare pagamenti/scadenze e quadrature per il caso reale;
4. importare un esercizio campione e riconciliare bilancio, registri IVA, partitari, banche e cespiti;
5. eseguire UAT con commercialista e responsabile amministrativo;
6. penetration test, backup/ripristino e prova di rollback cutover.

La parità non va dichiarata sulla sola presenza delle tabelle: il gate si chiude solo con evidenze di quadratura e test utente.

La matrice contabile dettagliata e i criteri di accettazione sono in [ACCOUNTING_PARITY.md](ACCOUNTING_PARITY.md).
