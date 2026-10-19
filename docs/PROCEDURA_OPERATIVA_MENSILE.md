# Procedura operativa mensile dopo la migrazione DATEV

Questa procedura si applica ai periodi successivi all’ultimo mese già migrato e riconciliato. Non va usata per ricaricare lo storico già presente.

## 1. Preparazione

1. Verificare che l’esercizio e il periodo IVA siano aperti.
2. Eseguire un backup di database e `storage`.
3. Annotare l’ultimo numero documento, protocollo IVA e data già presenti.
4. Non mescolare nello stesso lotto XML operativi e PDF probatori.

## 2. Fatture elettroniche XML

Per gli XML del nuovo mese aprire **Importazioni** e selezionare **Fatture XML FatturaPA**. Sono accettati XML, P7M e ZIP.

1. Caricare un archivio per volta e controllare l’anteprima.
2. Verificare azienda destinataria, tipo documento, date e controparti.
3. Confermare il lotto una sola volta.
4. Controllare `Importate`, `Già presenti` ed `Errori`.
5. Aprire **Fatture attive** e **Fatture passive** e verificare un campione.
6. Aprire **Tesoreria e partite** e controllare che le rate dell'XML coincidano con il netto dovuto; se l'anteprima segnala un ricalcolo, verificare la scadenza unica proposta.

L’import operativo:

- crea o riutilizza cliente/fornitore tramite Partita IVA;
- crea documento e righe;
- genera prima nota, movimento IVA e partita aperta;
- alimenta automaticamente il sottoconto della controparte.
- conserva le rate XML quando quadrano con il netto dovuto; in caso contrario genera una scadenza unica corretta, evitando residui fittizi.

Se compare `Piano dei conti incompleto`, completare prima in **Contabilità → Piano dei conti e causali** i mapping indicati. Non scegliere conti casuali e non ricaricare ripetutamente il file.

Gli XML già acquisiti come **Originali DATEV Koinos** sono archivio storico e non generano scritture: non confondere i due percorsi.

## 3. Registri IVA DATEV del mese di transizione

Se per il mese il dato fiscale autorevole proviene ancora da DATEV:

1. conservare il PDF con **Originali DATEV Koinos — acquisizione guidata**;
2. trasformarlo con il convertitore collaudato `tools/datev/extract_vat_pdf.py`;
3. importare il CSV prodotto come **Registri IVA storici — CSV analitico DATEV**;
4. controllare acquisti, vendite, reverse charge/autofatture, imponibile, IVA e detraibilità;
5. verificare l’idempotenza: il secondo caricamento deve risultare già presente, senza duplicazioni.

Non importare il PDF direttamente come movimenti IVA: il PDF viene archiviato come fonte probatoria.

## 4. Controlli contabili

1. Aprire **Contabilità → Prima nota** e verificare tutte le scritture del mese.
2. Controllare le righe articolo XML, mostrate in sola lettura, e distinguere fatture da note di credito.
3. Correggere l’eventuale conto errato con **Cambia conto**; sono selezionabili anche cespiti e conti patrimoniali compatibili.
4. Usare **Ripartisci** quando una fattura interessa più conti; per risconti e fatture da ricevere far verificare conto e quota al responsabile contabile.
5. Correggere nel **Riepilogo IVA** articoli e detraibilità; usare **Dividi** o **Aggiungi riga IVA** quando servono trattamenti diversi e salvare solo a quadratura raggiunta.
6. Per gli acquisti di cespiti impostare nel riepilogo **Classificazione acquisto → Bene ammortizzabile / cespite**: conto contabile, detraibilità e classificazione fiscale restano tre controlli distinti.
7. Ricalcolare la liquidazione non definitiva.
8. Usare **Salva e successiva** e il marcatore **Già modificata** per completare il lotto senza perdere la posizione.
9. Aprire **Situazione contabile e mastrini** e controllare conti collettivi, IVA, costi e ricavi.
10. Aprire **Mastrini clienti e fornitori** e completare eventuali associazioni rimaste; la scheda contabile mostra anche fatture e pagamenti già chiusi. Aprire almeno un cliente e un fornitore e verificare la stampa PDF con progressivi, date documento, protocolli, Dare/Avere e saldo.
11. Verificare che Dare = Avere e che i saldi iniziali comprendano l’apertura dell’esercizio.

Le scritture di un esercizio non chiuso restano provvisorie e modificabili nei limiti dei blocchi contabili/fiscali. La stampa definitiva e il blocco richiedono il workflow di chiusura previsto.

## 5. Liquidazione IVA

Prima di rendere definitiva la liquidazione confrontare:

- IVA a debito e a credito;
- IVA indetraibile e percentuali di detraibilità;
- note di credito determinate dal tipo documento TD, non da importi XML negativi;
- credito precedente, interessi e rettifiche;
- totale da versare o credito da riportare;
- dettaglio per registro, articolo e aliquota.

Nel calcolo lasciare vuoto **Credito precedente** per usare il riporto automatico dalla liquidazione immediatamente precedente. Inserire un importo manuale soltanto per una rettifica documentata e controllare nell'esito se l'origine risulta **Automatica** o **Manuale**.

Solo dopo la quadratura con il prospetto del commercialista procedere con validazione, pagamento e blocco del periodo.

## 6. Chiusura del mese

Conservare nel fascicolo mensile:

- elenco lotti importati con ID ed esito;
- originali XML/P7M/ZIP e PDF DATEV;
- CSV IVA derivato e relativo checksum;
- bilancio di verifica e mastrini campione;
- partitario e scadenzario;
- liquidazione IVA verificata;
- elenco delle rettifiche manuali e approvazione del responsabile contabile.
