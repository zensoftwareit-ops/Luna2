# Procedura operativa di importazione DATEV Koinos → Luna2

## Perimetro della migrazione

Questa procedura importa nell'ordine corretto i dati disponibili di **BASIC S.R.L.S.**, P. IVA **02572350185**, per gli esercizi:

- 2025 completo;
- 2026 dal 1° gennaio al 31 luglio 2026;
- cespiti storici necessari a ricostruire valori iniziali, progressivi e movimenti fino al 2026;
- fatture elettroniche disponibili come archivio storico.

La procedura deve essere eseguita prima su una copia dell'azienda. Solo dopo la riconciliazione contabile e fiscale si ripete sul database definitivo.

## Regole tassative

1. Non importare più file contemporaneamente.
2. Non cambiare l'ordine indicato.
3. Prima di ogni conferma controllare che l'azienda destinataria sia **BASIC S.R.L.S. – P. IVA 02572350185**.
4. Se un'importazione produce anche un solo errore tecnico, non passare al file seguente.
5. Non importare i due ZIP delle fatture storiche con il tipo `Fatture XML FatturaPA`: creerebbe documenti o registrazioni duplicati rispetto alla prima nota storica.
6. Non caricare direttamente il pacchetto `Luna2_tracciati_operativi_DATEV_2025_2026.zip`: estrarlo sul computer e usare separatamente i tre CSV contenuti.
7. Non eliminare il database esistente. Le migrazioni strutturali si applicano sopra il database.
8. Non chiudere gli esercizi 2025 e 2026 prima della fine dell'importazione e della riconciliazione.
9. Conservare una copia immutabile di tutti i file DATEV originali e del pacchetto Luna2.

## 0. Preparazione obbligatoria

### 0.1 Distribuire la versione aggiornata

Eseguire il pull del branch `luna2-php` nell'installazione di collaudo.

Da Plesk eseguire la migrazione:

```text
/opt/plesk/php/8.4/bin/php /var/www/vhosts/<dominio>/httpdocs/bin/luna migrate
```

Risultato richiesto: tutte le migrazioni devono risultare applicate e il comando deve terminare senza errori.

### 0.2 Controllare PHP in Plesk

Impostare almeno:

- `memory_limit = 512M`
- `max_execution_time = 300`
- `upload_max_filesize = 64M`
- `post_max_size = 64M`

### 0.3 Preparare l'azienda di collaudo

1. Creare o usare una copia dell'azienda reale.
2. Accedere come superuser o utente con permesso di importazione.
3. Selezionare l'azienda corretta.
4. Verificare in **Azienda e utenti**:
   - ragione sociale `BASIC S.R.L.S.`;
   - P. IVA `02572350185`;
   - esercizi contabili 2025 e 2026 presenti;
   - esercizi e periodi non bloccati/non chiusi.
5. Fare un backup del database.
6. Fare un backup della cartella `storage/imports`.
7. Annotare data, ora e nome dei due backup.

### 0.4 Preparare i file Luna2

Estrarre sul computer:

`Luna2_tracciati_operativi_DATEV_2025_2026.zip`

Devono comparire esattamente:

1. `Luna2_integrazione_piano_dei_conti.csv`
2. `Luna2_movimenti_contabili_2025.csv`
3. `Luna2_movimenti_contabili_2026_al_3107.csv`

Non rinominare questi tre CSV.

## Procedura standard per ogni file DATEV originale

Quando nella tabella seguente il tipo è **Originali DATEV Koinos — acquisizione guidata**, eseguire sempre questi passaggi:

1. Aprire **Importazioni**.
2. Nel riquadro **Importa dati da DATEV Koinos**, selezionare `Originali DATEV Koinos — acquisizione guidata`.
3. Premere **Scegli file** e selezionare esclusivamente il file indicato nella riga corrente.
4. Premere **Carica e analizza**.
5. Nella pagina di anteprima verificare:
   - nome esatto del file;
   - azienda `BASIC S.R.L.S.`;
   - P. IVA `02572350185`;
   - numero di righe analizzate maggiore di zero;
   - nessun errore tecnico.
6. Se l'azienda o la P. IVA non corrispondono, interrompere senza confermare.
7. Spuntare la dichiarazione `Ho verificato l’azienda destinataria, eseguito un backup e letto i limiti dell’acquisizione.`
8. Premere **Conferma acquisizione** una sola volta.
9. Attendere il completamento senza ricaricare la pagina.
10. Controllare che **Errori = 0**.
11. Annotare ID del lotto, file, righe analizzate, righe acquisite ed eventuali righe `Da completare` o `Da riconciliare`.
12. Solo dopo questo controllo passare al file successivo.

`Riferimento`, `Da completare` e `Da riconciliare` non equivalgono automaticamente a un errore tecnico: identificano dati conservati o mapping da verificare. Devono però essere inclusi nel verbale di riconciliazione.

## Procedura standard per i CSV operativi Luna2

Quando il tipo indicato è **Piano dei conti** oppure **Prima nota / movimenti**:

1. Aprire **Importazioni**.
2. Selezionare il tipo esatto indicato nella tabella.
3. Scegliere il singolo CSV indicato.
4. Premere **Carica e analizza**.
5. Controllare l'anteprima e l'azienda destinataria.
6. Premere **Conferma importazione** una sola volta.
7. Attendere il completamento.
8. Verificare che il lotto sia completato e che gli errori siano zero.
9. Eseguire subito il controllo contabile indicato prima di proseguire.

## 1. Tabelle di base e anagrafiche

Caricare i file uno alla volta in questo ordine.

| N. | Nome esatto del file | Tipo da selezionare | Risultato/controllo prima di proseguire |
|---:|---|---|---|
| 1 | `Stampa piano dei conti_150926095325.csv` | Originali DATEV Koinos — acquisizione guidata | Conti acquisiti senza errori; conservare l'elenco dei conti incompleti. |
| 2 | `Stampa lista clienti fornitori_150926095616.csv` | Originali DATEV Koinos — acquisizione guidata | Clienti e fornitori acquisiti; verificare duplicati di P. IVA/C.F. e soggetti da riconciliare. |
| 3 | `Stampa Aliquote IVA_150926100840.csv` | Originali DATEV Koinos — acquisizione guidata | Tabella aliquote conservata e disponibile per la riconciliazione fiscale. |
| 4 | `Stampa Articoli IVA_150926095834.csv` | Originali DATEV Koinos — acquisizione guidata | Articoli IVA conservati; annotare quelli senza mapping Luna2. |
| 5 | `Stampa elenco conti per tributo_150926100153.csv` | Originali DATEV Koinos — acquisizione guidata | Associazioni tributi/conti conservate senza errori. |
| 6 | `ELENCO CAUSALI CONTABILI_150926100023.csv` | Originali DATEV Koinos — acquisizione guidata | Causali acquisite come dati da verificare; non abilitarle in massa senza controllo del commercialista. |
| 7 | `Stampa elenco categorie cespiti_150926100420.csv` | Originali DATEV Koinos — acquisizione guidata | Categorie acquisite; le collisioni di codice vanno riconciliate, non ignorate. |
| 8 | `Stampa conti per categoria cespite_150926100636.csv` | Originali DATEV Koinos — acquisizione guidata | Collegamenti categoria/conti conservati. |
| 9 | `Stampa elenco aliquote ammortamento per cespite_150926100306.csv` | Originali DATEV Koinos — acquisizione guidata | Aliquote conservate per il confronto con le anagrafiche cespiti. |

### Controllo di blocco n. 1

Prima di proseguire:

- nessun lotto deve avere errori tecnici;
- i clienti/fornitori duplicati devono essere elencati;
- le collisioni delle categorie cespiti devono essere elencate;
- i conti DATEV incompleti devono essere noti.

## 2. Integrazione dei conti mancanti

| N. | Nome esatto del file | Tipo da selezionare | Risultato/controllo |
|---:|---|---|---|
| 10 | `Luna2_integrazione_piano_dei_conti.csv` | Piano dei conti | I 17 conti usati nei movimenti ma assenti dalla stampa del piano dei conti devono essere creati senza errori. |

Dopo l'importazione aprire **Contabilità → Piano dei conti** e cercare a campione almeno cinque codici presenti nel CSV. Non importare ancora la prima nota se uno dei 17 conti manca.

## 3. Cespiti operativi

| N. | Nome esatto del file | Tipo da selezionare | Risultato atteso |
|---:|---|---|---|
| 11 | `Anagrafica completa cespiti.xls` | Originali DATEV Koinos — acquisizione guidata | **131 cespiti** operativi. |
| 12 | `Cespiti progressivi.xls` | Originali DATEV Koinos — acquisizione guidata | **787 progressivi** collegati ai cespiti. |
| 13 | `Movimenti completi cespiti.xls` | Originali DATEV Koinos — acquisizione guidata | **610 movimenti cespiti** collegati alle anagrafiche. |

### Controllo di blocco n. 2

Aprire la sezione cespiti e verificare almeno:

1. numero complessivo cespiti = 131;
2. presenza di cespiti acquistati in anni diversi;
3. data acquisto e data entrata in funzione;
4. costo storico;
5. categoria e aliquota;
6. fondo ammortamento e residuo;
7. almeno un cespite dismesso/alienato;
8. almeno un cespite con movimenti 2025;
9. quadratura dei progressivi 2025 con il registro DATEV 2025.

Se un movimento fa riferimento a un cespite inesistente, fermare la procedura.

## 4. Registri storici dei cespiti

Questi PDF sono fonti probatorie e di riconciliazione. Non sostituiscono i tre file operativi del punto precedente.

Caricarli cronologicamente:

| N. | Nome esatto del file | Tipo da selezionare |
|---:|---|---|
| 14 | `Registro cespiti analitico 2016.pdf` | Originali DATEV Koinos — acquisizione guidata |
| 15 | `Registro cespiti analitico 2017.pdf` | Originali DATEV Koinos — acquisizione guidata |
| 16 | `Registro cespiti analitico 2018.pdf` | Originali DATEV Koinos — acquisizione guidata |
| 17 | `Registro cespiti analitico 2019.pdf` | Originali DATEV Koinos — acquisizione guidata |
| 18 | `Registro cespiti analitico 2020.pdf` | Originali DATEV Koinos — acquisizione guidata |
| 19 | `Registro cespiti analitico 2021.pdf` | Originali DATEV Koinos — acquisizione guidata |
| 20 | `Registro cespiti analitico 2022.pdf` | Originali DATEV Koinos — acquisizione guidata |
| 21 | `Registro cespiti analitico 2023.pdf` | Originali DATEV Koinos — acquisizione guidata |
| 22 | `Registro cespiti analitico 2024.pdf` | Originali DATEV Koinos — acquisizione guidata |
| 23 | `Registro cespiti analitico 2025.pdf` | Originali DATEV Koinos — acquisizione guidata |

Non esiste un registro definitivo 2026 perché l'esercizio è ancora in corso: per il 2026 fanno fede l'anagrafica completa e i movimenti esportati fino al 31 luglio.

### File 2015 con nome errato

Il file ricevuto come `Registro cespiti analitico 2015.pdf` contiene in realtà pagine di libro giornale, non il registro cespiti 2015. Non deve essere usato come registro cespiti né come prima nota operativa.

Se si desidera conservarlo nel fascicolo delle fonti:

1. farne una copia;
2. rinominare la copia `Libro giornale analitico 2015 - solo supporto cespiti.pdf`;
3. caricarla come `Originali DATEV Koinos — acquisizione guidata`;
4. classificarla esclusivamente come allegato probatorio per i cespiti storici.

## 5. Fonti contabili originali 2025 e 2026

Questi quattro file documentano le testate e il dettaglio originario. Vanno acquisiti prima dei CSV operativi, ma non generano da soli la prima nota operativa.

| N. | Nome esatto del file | Tipo da selezionare | Uso |
|---:|---|---|---|
| 24 | `prima nota 2025.xls` | Originali DATEV Koinos — acquisizione guidata | Fonte/testate prima nota 2025. |
| 25 | `Libro giornale analitico al 311225.pdf` | Originali DATEV Koinos — acquisizione guidata | Fonte analitica da cui è stato derivato il CSV operativo 2025. |
| 26 | `prima nota al 310726.xls` | Originali DATEV Koinos — acquisizione guidata | Fonte/testate prima nota 2026 al 31/07. |
| 27 | `Libro giornale analitico al 310726.pdf` | Originali DATEV Koinos — acquisizione guidata | Fonte analitica da cui è stato derivato il CSV operativo 2026. |

Il vecchio `Report.pdf` non serve se sono presenti i due PDF con nome esplicito indicati sopra. Conservarlo fuori dall'import come copia ridondante.

## 6. Prima nota e movimenti Dare/Avere operativi

Questa è la fase che crea i movimenti contabili utilizzabili da mastrini, libro giornale e bilancio di verifica.

### 6.1 Esercizio 2025

| N. | Nome esatto del file | Tipo da selezionare |
|---:|---|---|
| 28 | `Luna2_movimenti_contabili_2025.csv` | Prima nota / movimenti |

Risultato atteso:

- 17.901 righe contabili importate;
- 6.846 registrazioni di giornale;
- totale Dare: € 7.999.087,15;
- totale Avere: € 7.999.087,15;
- differenza: € 0,00.

Prima di caricare il 2026:

1. aprire **Contabilità → Prima nota/Libro giornale**;
2. filtrare dal 01/01/2025 al 31/12/2025;
3. verificare la prima e l'ultima registrazione;
4. aprire il bilancio di verifica al 31/12/2025;
5. controllare Dare = Avere e differenza zero;
6. aprire almeno cinque mastrini, inclusi banca, cliente, fornitore, costo e IVA;
7. confrontare i saldi con DATEV.

### 6.2 Esercizio 2026 fino al 31 luglio

| N. | Nome esatto del file | Tipo da selezionare |
|---:|---|---|
| 29 | `Luna2_movimenti_contabili_2026_al_3107.csv` | Prima nota / movimenti |

Risultato atteso:

- 3.646 righe contabili importate;
- 1.469 registrazioni di giornale;
- totale Dare: € 1.041.549,67;
- totale Avere: € 1.041.549,67;
- differenza: € 0,00.

Controllare:

1. periodo dal 01/01/2026 al 31/07/2026;
2. nessuna registrazione successiva al 31/07/2026 proveniente dal lotto;
3. Dare = Avere;
4. differenza zero;
5. progressivi iniziali coerenti con la chiusura 2025;
6. nessun movimento duplicato rispetto a eventuali dati già presenti.

### Controllo complessivo della prima nota

Dopo entrambi i CSV:

- registrazioni totali: **8.315**;
- righe contabili totali: **21.547**;
- Dare complessivo: **€ 9.040.636,82**;
- Avere complessivo: **€ 9.040.636,82**;
- differenza: **€ 0,00**.

Se i valori non coincidono, non importare IVA e fatture storiche e non ripetere subito il caricamento: una seconda importazione potrebbe duplicare i movimenti. Esaminare prima il lotto e, se previsto per quel lotto standard, usare il rollback.

## 7. Registri IVA

| N. | Nome esatto del file | Tipo da selezionare |
|---:|---|---|
| 30 | `Registri IVA 2025.pdf` | Originali DATEV Koinos — acquisizione guidata |
| 31 | `Registri IVA al 310726.pdf` | Originali DATEV Koinos — acquisizione guidata |

Controllare per ciascun periodo e ciascun registro:

- imponibile per articolo/codice IVA;
- aliquota;
- imposta;
- acquisti;
- vendite;
- reverse charge;
- autofatture;
- totali del periodo;
- coerenza con le liquidazioni IVA.

### Limite operativo da non ignorare

Con i file attualmente disponibili, i PDF IVA vengono archiviati come fonte probatoria e usati per la riconciliazione. Non alimentano automaticamente tutte le righe analitiche della tabella operativa `vat_movements`.

Di conseguenza, una migrazione può essere considerata completa sul piano documentale e contabile Dare/Avere, ma **non ancora completa al 100% sul piano dello storico IVA navigabile** finché non viene eseguita una delle seguenti attività:

1. ottenere da DATEV un export analitico IVA in CSV/XLS e importarlo; oppure
2. completare e collaudare un importatore analitico dedicato per i due PDF IVA.

Questo è un controllo bloccante prima del go-live fiscale definitivo.

## 8. Fatture elettroniche storiche

Importare per ultime:

| N. | Nome esatto del file | Tipo da selezionare | Uso |
|---:|---|---|---|
| 32 | `20260915_ExportFattureRicevute.zip` | Originali DATEV Koinos — acquisizione guidata | Archivio storico fatture passive XML. |
| 33 | `20260915_ExportFattureInviate.zip` | Originali DATEV Koinos — acquisizione guidata | Archivio storico fatture attive XML. |

Non selezionare `Fatture XML FatturaPA`. I movimenti contabili 2025/2026 sono già stati importati dai CSV: l'importazione operativa degli XML potrebbe duplicare documenti, IVA, scadenze o prima nota.

Controllo atteso: **58 XML complessivi** tra ricevuti e inviati. Il numero è inferiore allo storico reale ma corrisponde ai file effettivamente consegnati.

## 9. File che non devono essere importati

- screenshot dell'applicativo DATEV;
- `Report.pdf`, se duplicato dei libri giornale con nome esplicito;
- il pacchetto `Luna2_tracciati_operativi_DATEV_2025_2026.zip` intero;
- `Registro cespiti analitico 2015.pdf` come registro cespiti;
- qualunque file temporaneo, rinominato o modificato senza conservarne l'originale;
- i due ZIP fatture con il tipo `Fatture XML FatturaPA`.

## 10. Riconciliazione finale obbligatoria

Compilare e firmare un verbale con almeno questi controlli:

### Anagrafiche

- numero clienti;
- numero fornitori;
- controllo a campione di ragione sociale, P. IVA, C.F., indirizzo e condizioni di pagamento;
- elenco duplicati e decisione di fusione/mantenimento.

### Contabilità

- 6.846 registrazioni e 17.901 righe per il 2025;
- 1.469 registrazioni e 3.646 righe per il 2026;
- quadratura Dare/Avere per ciascun esercizio;
- bilancio di verifica 31/12/2025;
- bilancio di verifica 31/07/2026;
- controllo mastrini banca, cassa, clienti, fornitori, IVA, costi e ricavi;
- controllo saldi di apertura 2026.

### IVA

- totali per registro;
- totali per articolo e aliquota;
- liquidazioni periodiche;
- eventuale credito/debito riportato;
- reverse charge e autofatture;
- elenco degli elementi non ancora presenti nello storico IVA operativo.

### Cespiti

- 131 anagrafiche;
- 787 progressivi;
- 610 movimenti;
- costo storico;
- fondi ammortamento;
- residui;
- acquisizioni e dismissioni;
- quadratura con registro analitico 2025.

### Fatture elettroniche

- 58 XML conservati;
- distinzione ricevute/inviate;
- nessuna duplicazione di documenti o registrazioni contabili.

### Approvazione

La migrazione definitiva è autorizzabile solo quando:

1. tutti i lotti tecnici hanno zero errori;
2. Dare e Avere quadrano;
3. i saldi campione coincidono con DATEV;
4. cespiti e progressivi coincidono con il registro 2025;
5. lo storico IVA è stato riconciliato e l'eventuale limite operativo è stato risolto o formalmente accettato;
6. il commercialista del cliente firma il verbale di quadratura.

## 11. Ripetizione sul database definitivo

Solo dopo il collaudo:

1. bloccare le registrazioni su DATEV alla data concordata;
2. eseguire un nuovo backup del database Luna2 definitivo e di `storage/imports`;
3. annotare data e ora di cut-off;
4. ripetere esattamente i punti da 1 a 8 nello stesso ordine;
5. ripetere tutti i controlli del punto 10;
6. far firmare il verbale definitivo;
7. conservare backup, file originali, CSV Luna2, hash dei pacchetti e verbale nello stesso fascicolo di migrazione.

## Registro minimo da compilare durante il lavoro

Per ogni file annotare:

| Ordine | File | Tipo import | ID lotto | Data/ora | Analizzate | Acquisite | Errori | Da completare/riconciliare | Operatore | Esito |
|---:|---|---|---:|---|---:|---:|---:|---:|---|---|
| 1 |  |  |  |  |  |  |  |  |  |  |

Non lasciare campi vuoti: questo registro dimostra la sequenza seguita e rende possibile individuare il punto esatto di un'eventuale anomalia.
