# Migrazione da DATEV Koinos

## Obiettivo

Trasferire storico documentale e contabile mantenendo tracciabilità, quadratura e possibilità di ripetere o annullare il caricamento. Gli originali restano immutabili e ogni record Luna2 conserva il lotto sorgente.

## Export da richiedere al cliente

Richiedere un primo campione anonimizzato e, al cutover, l’export definitivo:

1. **Esporta archivio** completo, includendo l’archivio Simulazione fiscale/rettifiche quando disponibile;
2. anagrafiche clienti, fornitori, banche e condizioni di pagamento in XLSX/CSV;
3. piano dei conti gerarchico, causali, codici IVA e centri di costo;
4. prima nota analitica con protocollo, data, conto, Dare/Avere, descrizione, documento e controparte;
5. saldi di apertura/chiusura e bilanci di verifica per ogni esercizio;
6. registri IVA, liquidazioni, crediti riportati, LIPE e protocolli;
7. partitari clienti/fornitori e scadenze aperte/chiuse;
8. estratti conto e riconciliazioni, preferibilmente CAMT.053/MT940 o CSV;
9. registro cespiti e piani ammortamento;
10. XML FatturaPA attivi, passivi, notifiche e relativi PDF/allegati;
11. corrispettivi e documenti non elettronici;
12. scadenzario fiscale e F24 se gestiti nell’applicativo.

Il formato tedesco DATEV non va assunto uguale al prodotto italiano DATEV Koinos. Il parser dell’archivio proprietario si implementa solo dopo aver verificato un file reale e la relativa licenza d’uso.

## Pipeline Luna2

1. **Acquisizione** – salva file originale fuori webroot, checksum SHA-256, dimensione e utente.
2. **Staging** – estrae ZIP in modo sicuro, legge CSV/XLSX/XML e normalizza intestazioni senza toccare dati operativi.
3. **Anteprima** – mostra le prime righe, tipo, conteggi ed errori.
4. **Mapping** – associa campi Koinos a Luna2; i mapping specifici del cliente saranno salvati in `import_mappings`.
5. **Validazione** – controlla obbligatorietà, P.IVA/CF, conti esistenti, date, duplicati e quadratura Dare/Avere.
6. **Importazione** – usa transazioni, chiavi esterne idempotenti e `import_records`.
7. **Riconciliazione** – confronta conteggi e totali con report Koinos firmati dal cliente.
8. **Conferma o rollback** – il lotto rimane auditabile; il rollback ripristina record aggiornati e rimuove quelli creati.

## Ordine di caricamento

1. configurazione azienda, utenti e periodi;
2. codici IVA, piano dei conti e centri di costo;
3. clienti, fornitori, prodotti, banche e cespiti;
4. fatture XML e altri documenti;
5. prima nota per esercizio, dal più vecchio;
6. pagamenti, scadenze e movimenti bancari;
7. registri IVA, liquidazioni e saldi di apertura;
8. allegati e documenti di conservazione.

Se si importano sia fatture sia prima nota, le fatture storiche non vanno contabilizzate automaticamente: prevalgono le scritture originali Koinos, evitando duplicazioni.

## Quadrature obbligatorie

Per ogni esercizio e per il totale storico:

- numero clienti/fornitori e duplicati per P.IVA/CF;
- numero documenti per tipo/anno e totale imponibile/IVA/totale;
- Dare = Avere per singola registrazione, mese ed esercizio;
- saldi per conto uguali al bilancio di verifica Koinos;
- conto economico e stato patrimoniale uguali ai prospetti approvati;
- registri IVA e liquidazioni uguali per periodo, inclusi crediti riportati;
- partitari e scadenze aperte uguali alla data di cutover;
- saldo di ogni banca e cassa uguale all’estratto conto;
- costo storico, fondo e valore netto dei cespiti uguali al registro;
- checksum e conteggio degli XML FatturaPA originali.

Le differenze vanno classificate: arrotondamento, mapping, record mancante, duplicato o regola contabile. Nessuna differenza viene corretta direttamente nel database senza una registrazione o una regola documentata.

## Cutover

- prova completa su copia dati;
- verbale di quadratura firmato da amministrazione/commercialista;
- finestra di freeze su Koinos;
- export delta dall’ultima prova;
- backup Luna2 verificato e test di ripristino;
- import definitivo, quadratura e apertura utenti;
- Koinos mantenuto in sola lettura per il periodo concordato;
- rollback applicativo e DNS/document root pronti.
