# Parità contabile DATEV Koinos

## Principio

La presenza di una tabella non equivale a una funzione contabile completa. Ogni voce viene considerata operativa solo quando esistono inserimento, controlli, consultazione, audit, blocco del periodo e una prova di quadratura su dati Koinos.

Le note ufficiali DATEV mostrano che la gestione contabile comprende, oltre alla prima nota, registri e liquidazioni, anche sezionali IVA, corrispettivi, pro-rata, regimi speciali, schede contabili, importazioni e collegamenti con dichiarazioni e bilanci:

- [Note DATEV Contabilità 2026](https://studi.datev.it/datev_resources/INFORMAZIONI_DK/extcnt/Contabilita/Datev.IT.Contabilita.Contabilita.pdf)
- [Portale DATEV Koinos](https://www.datev.it/)
- [DK Konto e riconciliazione bancaria](https://dkkonto.datev.it/)

## Operativo in Luna2 3.3

| Funzione | Copertura |
|---|---|
| Prima nota | Scritture manuali, bozze modificabili, protocolli, dettaglio righe e contabilizzazione con quadratura obbligatoria |
| Scritture automatiche | Fatture attive, passive e note di credito generano scritture in partita doppia |
| Libro giornale e mastrini | Consultazione cronologica, filtri periodo, progressivi per conto |
| Bilancio di verifica | Totali Dare/Avere e saldi per conto |
| Registri IVA | Vendite, acquisti e corrispettivi; movimenti da documenti o manuali; riepilogo per codice IVA |
| Detraibilità | Importo detraibile distinto per gli acquisti, anche inferiore all’IVA del movimento |
| Liquidazioni IVA | Periodicità mensile/trimestrale, credito precedente, interessi inseriti e verificati, saldo e dettaglio per registro/codice |
| Blocco periodo IVA | Liquidazioni definitive/pagate impediscono la modifica dei movimenti; riapertura esplicita e auditata |
| Storico esistente | Sincronizzazione idempotente dei documenti già emessi/ricevuti nei registri IVA |

## Obbligatorio prima della sostituzione completa di Koinos

| Priorità | Funzione da completare | Criterio di accettazione |
|---|---|---|
| P0 | Causali contabili configurabili e automatismi | Stesse scritture del campione Koinos per ogni causale utilizzata dal cliente |
| P0 | Sezionali e protocolli IVA multipli | Numerazione, prefissi/suffissi e stampe uguali ai registri Koinos |
| P0 | Split payment, reverse charge, IVA per cassa e indetraibilità | Quadratura per codice IVA e casistica reale del cliente |
| P0 | Pro-rata e rettifiche detrazione | Risultato annuale uguale al prospetto del commercialista |
| P0 | LIPE e dichiarazione IVA | Prospetto, controlli ed export validati sul periodo campione; invio solo tramite canale autorizzato |
| P0 | Chiusura/apertura esercizio, ratei e risconti | Bilancio di chiusura e saldi di riapertura uguali a Koinos |
| P0 | Partitari e scadenzari clienti/fornitori | Saldi e partite aperte uguali alla data di cutover |
| P1 | Stato patrimoniale e conto economico riclassificati | Stampe confrontate con bilanci approvati |
| P1 | Ritenute, professionisti e principio di cassa | Casistiche del cliente validate dal consulente |
| P1 | Regime del margine e altri regimi speciali | Implementare soltanto quelli effettivamente utilizzati |
| P1 | Cespiti civilistici/fiscali e ammortamenti | Registro e scritture annuali quadrati con Koinos |
| P1 | Banche e riconciliazione | Saldi banca/cassa e movimenti riconciliati uguali agli estratti conto |
| P1 | Stampe ufficiali e conservazione | PDF numerati, auditati e conservati secondo il processo concordato |

## Evidenze richieste al primo cliente

1. elenco delle causali, dei registri e dei regimi IVA realmente utilizzati;
2. export anonimizzato di prima nota, piano dei conti, registri IVA e liquidazioni per almeno un trimestre;
3. bilancio di verifica dello stesso periodo;
4. partite aperte clienti/fornitori e saldi banca/cassa alla medesima data;
5. registro cespiti e ultima liquidazione/dichiarazione IVA annuale;
6. verbale di quadratura firmato da amministrazione e commercialista.

Il passaggio definitivo può avvenire solo quando tutte le voci P0 applicabili al cliente sono chiuse e i totali coincidono con Koinos.
