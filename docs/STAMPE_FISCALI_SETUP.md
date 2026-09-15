# Stampe contabili e fascicolo di evidenza

## Aggiornamento

1. Effettuare il backup del database e dei file privati.
2. Eseguire il pull del branch `luna2-php` in Plesk.
3. Eseguire `bin/luna migrate` con la versione PHP del dominio: la nuova migrazione è `015_fiscal_print_evidence.sql`. Non eliminare il database. I dati esistenti rimangono disponibili.
4. Verificare ragione sociale, codice fiscale e sede in Azienda e utenti.
5. Rigenerare le stampe nel Centro professionale. I PDF già archiviati rimangono invariati; quelli precedenti a questo aggiornamento non contengono il nuovo fascicolo.

In Plesk, un'attività di tipo **Esegui uno script PHP** deve avere come percorso il solo file `httpdocs/bin/luna` e come argomenti `migrate`, con PHP 8.4 o la versione configurata sul dominio. Non inserire gli argomenti nel percorso del file.

## Funzioni disponibili

- Mastrini: saldo iniziale, totali dell'intero periodo e saldo finale; la ricerca testuale non modifica i saldi contabili. Nel Centro professionale è disponibile anche la stampa di tutti i conti.
- Prima nota: PDF, XLSX e CSV contengono le righe dei conti Dare/Avere delle registrazioni selezionate. Una selezione filtrata rimane un prospetto di controllo.
- Libro giornale: estremi registrazione/documento/controparte, dettaglio conti, progressivi dall'inizio dell'anno e quadratura del periodo.
- Registri IVA: estremi del documento distinti dalla registrazione, competenza, sezionale, origine e riepiloghi per sezionale/articolo/aliquota. Per i movimenti manuali compilare numero e data documento originale.
- Liquidazioni: dettaglio aliquote, raccordo rettifiche, credito precedente, interessi, saldo, scadenza, pagamento, riferimento F24 e riferimenti al prospetto LIPE disponibile.
- Cespiti: in Contabilità → Adempimenti, bilancio e cespiti → Schede annuali cespiti, compilare i valori verificati dell'anno. Costo originario, rivalutazioni e svalutazioni cumulate a fine anno, fondi iniziali, coefficienti effettivi, quote annuali, eliminazione, corrispettivo e riferimento documentale. La scheda alimenta il registro e non genera scritture contabili né contabilizza automaticamente le quote. Verificarne il raccordo con contabilità e ammortamenti.

## Stampa, validazione e conservazione

Le nuove elaborazioni includono l'identificazione aziendale e pagine progressive per tipo di stampa e anno, oltre al numero dell'elaborazione. Ogni nuova generazione consuma un nuovo intervallo di pagine; per una copia identica riscaricare il PDF archiviato. La numerazione parte da 1 per la nuova serie di Luna2: concordare con il professionista l'eventuale prosecuzione di libri provenienti da altri gestionali.

Il registro cespiti utilizza la scheda annuale dell'anno selezionato; scegliere 1 gennaio–31 dicembre. Esercizi non solari richiedono una verifica specifica prima dell'utilizzo di questo registro. Dati mancanti o quadrature incoerenti sono riportati nel PDF e impediscono la validazione. Correggere i dati, rigenerare, indicare il riferimento della revisione professionale e bloccare l'elaborazione.

Il pulsante **Fascicolo ZIP** scarica PDF, dati utilizzati e manifest con impronte SHA-256, pagine, autore e validazione. Le impronte vengono verificate anche al download. Il fascicolo non è un pacchetto di conservazione già firmato o marcato temporalmente: va affidato alla procedura di tenuta/conservazione concordata con il commercialista. Non è stata attivata alcuna trasmissione automatica a conservatori esterni.

La presenza di un registro reverse charge/autofatture non certifica da sola la completezza delle annotazioni: verificare il documento originario e la corretta registrazione dei due lati IVA. Il prospetto LIPE di raccordo non è una ricevuta di trasmissione.

## Collaudo

`tests/run.php`: controlli generali e sintassi PHP.

`tests/fiscal_prints.php`: MariaDB locale di test sulla porta 33317, database isolato con nome casuale. Applica tutte le migrazioni, verifica saldi filtrati/non filtrati, isolamento aziendale, schede cespiti, liquidazioni e riferimenti di pagamento, genera tutti i tipi di PDF, valida e blocca le stampe, verifica ZIP e integrità e rifiuta metadati incompleti o alterati. Non legge le credenziali `.env` dell'applicazione.
