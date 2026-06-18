# Luna2 – Funzionalità Amministrative e Contabilità

---

## 1. Fatturazione Attiva

- Creazione fatture (proforma e reali) con numerazione automatica per anno
- Gestione stati: Emessa → Da Pagare → Pagata / Parzialmente Pagata / Scaduta
- Calcolo automatico imponibile, IVA e totale
- Conversione preventivo accettato in fattura
- Export PDF con logo aziendale
- Invio via email con tracciamento aperture e download
- **Fatturazione elettronica SDI**: generazione, validazione e invio XML FatturaPA
- Export formato Assosoftware (singole fatture e lotti)

---

## 2. Fatturazione Passiva (Fornitori)

- Ricezione automatica fatture da SDI tramite polling
- Gestione stati pagamento: Da Pagare → Pagata / Parzialmente Pagata / Scaduta
- Registrazione pagamenti singoli o multipli
- Import/export XML, associazione automatica fornitore tramite P.IVA
- Export formato Assosoftware

---

## 3. Liquidazione IVA

- Calcolo automatico liquidazione periodica (mensile o trimestrale)
- IVA a debito (fatture attive) – IVA a credito (fatture passive) = IVA netta
- Gestione crediti IVA da periodi precedenti
- Stati: Bozza → Calcolata → Presentata → Pagata / Scaduta
- Calcolo automatico data di scadenza pagamento
- Registro IVA nazionale ed export per conformità fiscale

---

## 4. Preventivi

- Numerazione automatica, stati: Bozza → Inviato → Accettato / Rifiutato / Convertito
- Calcolo automatico imponibile, IVA, totale
- Export PDF (versione tecnica e descrittiva)
- Invio email con tracciamento aperture e download
- Conversione diretta in fattura con un click

---

## 5. Ordini Clienti

- Numerazione automatica, gestione stati (Confermato, Evaso, Cancellato)
- Righe con prodotto, quantità, prezzo unitario, sconto percentuale, IVA
- Calcolo automatico totali
- Export PDF e invio email con tracciamento

---

## 6. Documenti di Trasporto (DDT)

- Generazione DDT con protocollo automatico
- Causale trasporto, aspetto beni, numero colli, trasportatore
- Indirizzo di destinazione, righe prodotto con quantità e prezzi
- Export PDF

---

## 7. Scadenzario Fiscale

- Gestione scadenze: IVA, IRPEF, INPS, F24, contributi
- Stati: Aperta → In corso → Completata / Scaduta
- Periodicità mensile, trimestrale, annuale
- Importo previsto per ogni scadenza
- KPI dashboard: scadenze prossime 7 giorni, scadute, in corso

---

## 8. Pagamenti

- Tracciamento pagamenti per fattura (inclusi pagamenti rateali e acconti)
- Metodi supportati: Bonifico, Carta, Contanti, Assegno, Vaglia, RID, F24, Credito, Altro
- Numero versamento, ID transazione, banca mittente, causale
- Riconciliazione con estratto conto bancario (flag + data riconciliazione)
- Storico completo movimenti per fattura

---

## 9. Contabilità Generale

- **Prima nota** con registrazioni dare/avere e piano dei conti gerarchico
- **Libro giornale** cronologico esportabile
- **Bilancio di verifica** con totali dare/avere per controllo
- **Conto economico** (ricavi, costi, risultato d'esercizio)
- **Stato patrimoniale** (attivo, passivo, patrimonio netto)
- **Mastrini** per singoli conti
- **Partitari** clienti e fornitori per scadenze
- Gestione rettifiche e chiusure d'esercizio
- **Cespiti**: registro immobilizzazioni con calcolo ammortamenti automatico

---

## 10. Clienti e Fornitori

**Clienti:**
- Anagrafica completa: ragione sociale, P.IVA, codice fiscale, IBAN, email, telefono
- Indirizzi multipli (sede legale + destinazioni di spedizione)
- Condizioni di pagamento predefinite per cliente
- Codice cliente generato automaticamente (CLI001, CLI002, …)
- Import da file (Excel/CSV)

**Fornitori:**
- Anagrafica: ragione sociale, P.IVA, contatti, sede legale
- Codice fornitore automatico
- Listini prezzi di acquisto per prodotto
- Import da file

---

## 11. Prodotti e Magazzino

**Catalogo Prodotti:**
- Codice prodotto, nome, descrizione, unità di misura
- Prezzo di vendita, costo unitario
- Codice EAN, SKU
- Categorie prodotto, flag attivo/inattivo

**Gestione Magazzino:**
- Giacenze per prodotto con scorta minima
- Movimenti: entrate, uscite, rettifiche con causale e data
- Storico movimenti completo
- Valorizzazione totale magazzino
- Identificazione automatica prodotti sotto scorta
- Picking list per ordini
- Scanner barcode per picking
- Generazione etichette PDF con barcode
- Gestione magazzini multipli

---

## 12. Paghe e Risorse Umane

- Configurazione dipendente: tariffa oraria, aliquote fiscali e contributive
- Registrazione presenze mensili e ore lavorate
- Calcolo automatico: stipendio lordo, netto, detrazioni fiscali, contributi
- Gestione straordinari con moltiplicatore configurabile (default 1.30x)
- Generazione e export cedolini

---

## 13. SDI e Fatturazione Elettronica

- Generazione XML FatturaPA per fatture reali
- Validazione schema XML prima dell'invio
- Trasmissione diretta al Sistema di Interscambio (SDI)
- Ricezione e gestione notifiche: accettazione, scarto, errori
- Polling automatico (cron job ogni 5 minuti) per notifiche e fatture passive
- Tracciamento stato: Inviata → Accettata / Errore
- Import automatico fatture passive ricevute via SDI

---

## 14. Report e Analitiche

- Vendite per periodo con grafici
- Top clienti per importo fatturato
- Top prodotti più venduti
- Report magazzino (giacenze e valorizzazione)
- Report ordini per stato e cliente
- Dashboard KPI con indicatori principali
- Export in PDF, Excel (XLSX), XML
- Filtri per date, cliente, prodotto, stato

---

## 15. Integrazioni

- **SDI / Agenzia delle Entrate**: fatturazione elettronica bidirezionale
- **Assosoftware**: export dati per software contabili di terze parti
- **eCommerce** (modulo MVP-3): centralizzazione ordini da WooCommerce, Shopify, Amazon, eBay con sync automatico

---

## Flusso Operativo Completo

```
Preventivo → Ordine → DDT → Fattura → Pagamento → Prima Nota → Liquidazione IVA
```

Luna2 è un gestionale ERP completo che copre l'intero ciclo aziendale,
dalla fase commerciale fino agli adempimenti fiscali, con piena conformità
alla normativa italiana sulla fatturazione elettronica.
