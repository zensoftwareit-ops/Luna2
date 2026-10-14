# Audit funzionale Luna2 rispetto a DATEV Koinos

Data verifica: 14 ottobre 2026.

## Metodo e fonti

Il confronto usa funzioni documentate pubblicamente da DATEV Koinos e le confronta con rotte, servizi, schema dati, controlli automatici e test presenti in Luna2. Fonti ufficiali consultate:

- note di versione **Contabilità generale e cespiti**: https://studi.datev.it/datev_resources/INFORMAZIONI_DK/extcnt/Contabilita/Datev.IT.Contabilita.Contabilita.pdf
- guida DATEV ai dati di testata e alla data di liquidazione IVA: https://docs.datev.it/help/0/efat/ita/documenti/altri_dati_di_testata_dei_documenti.htm
- guida DATEV alle impostazioni fiscali e documentali: https://docs.datev.it/help/0/efat/ita/impostazioni/impostazioni.htm
- API e funzioni SuperBill/Impresa: https://developer.datev.it/Docs/ChangeLog
- servizi IntegraSDI: https://developer.datev.it/Docs/IntegraSDI

Una funzione è considerata **operativa** solo se dispone di inserimento/importazione, controllo, consultazione, audit o blocco coerente e test. La sola presenza di una tabella non è sufficiente.

## Matrice di copertura

| Area | Stato Luna2 | Valutazione |
|---|---|---|
| Piano dei conti, causali e automatismi | Operativo | Adeguato per impresa; configurazione iniziale da validare |
| Prima nota e partita doppia | Operativo | Bozze/provvisorie, revisioni, modifica controllata, quadratura |
| Data documento, registrazione e competenza | Operativo | Date distinte; riallineamento coordinato di documento, contabilità e IVA |
| Bilancio di verifica e mastrini | Operativo | Progressivi, saldi iniziali, ricerca conti anche senza saldo |
| Sottoconti clienti/fornitori | Operativo | Estratto completo separato dallo scadenzario |
| Fatture XML e note di credito | Operativo | Import idempotente, righe, TD04/TD08, contabilità e IVA |
| Scadenze e netto da pagare | Operativo | Rate XML multiple; ricalcolo se non quadrano; ritenuta sottratta |
| Pagamenti, incassi e riconciliazione | Operativo | Parziali, storni, CSV/CAMT.053/MT940, matching assistito |
| Registri e liquidazioni IVA | Operativo da collaudare | Registri multipli, credito precedente, detraibilità, reverse/autofatture |
| Classificazione IVA beni ammortizzabili | Operativo | Separata da conto e detraibilità, riportata nel riepilogo annuale |
| Cespiti | Operativo da collaudare | Categorie, acquisto, nuovo/usato, quote civili/fiscali e registro |
| Ratei, risconti, chiusura e apertura | Operativo da collaudare | Workflow presente; richiede quadratura su caso reale |
| Ritenute | Operativo da collaudare | Importo, netto, scadenza e versamento; casistiche da validare |
| Stampe ufficiali | Operativo | PDF numerati, snapshot, checksum, validazione e blocco |
| LIPE/IVA annuale/CU/770/Redditi/IRAP/XBRL | Parziale | Dossier di controllo, non dichiarativi telematici completi |
| Invio/ricezione SdI e conservazione a norma | Integrazione esterna necessaria | Non dichiarare incluso senza provider e accordo collaudato |
| Regime del margine e regimi speciali estesi | Parziale | Motore dati presente solo per casistiche configurate |
| Paghe e dichiarativi da studio | Fuori perimetro | Luna2 non sostituisce la suite professionale DATEV completa |

## Valutazione commerciale

Luna2 è vendibile come gestionale ERP e contabile per impresa, con collaborazione e validazione del commercialista. Non deve essere commercializzato come sostituto integrale e universale della suite DATEV Koinos per studi professionali.

Il rischio principale non è la quantità di funzioni, ma dichiarare completa una funzione prima del collaudo su casistiche reali. Per ogni nuovo cliente deve essere definito il perimetro fiscale utilizzato e completato un periodo parallelo con il precedente gestionale.

## Gate obbligatorio di rilascio cliente

1. configurazione e approvazione di piano dei conti, causali, sezionali e mapping;
2. importazione di un mese campione con fatture, note di credito, ritenute e scadenze;
3. quadratura di prima nota, mastrini, partitari, bilancio e banca;
4. quadratura di registri e liquidazione IVA;
5. prova di un cespite e relativo ammortamento;
6. verifica di chiusura/apertura su copia del database;
7. verbale delle differenze residue e approvazione del responsabile contabile;
8. backup e piano di ripristino verificato prima del go-live.

## Priorità successive

- integrare un provider SdI/conservazione se il prodotto deve coprire l'intero ciclo elettronico;
- produrre file telematici solo dopo implementazione degli schemi ministeriali e test di conformità;
- aggiungere i regimi speciali soltanto in base alle casistiche dei clienti effettivi;
- mantenere test di regressione su un dataset contabile anonimizzato e firmato dal professionista.
