# Acquisizione degli originali DATEV Koinos

## Prima di iniziare

1. Eseguire un backup di database e cartella `storage/imports`.
2. Collaudare su una copia separata, non sul database in uso.
3. Applicare le migrazioni, inclusa `016_datev_source_archive.sql`, con il normale comando `bin/luna migrate`. Non eliminare il database.
   In Plesk, per gli XLS voluminosi, impostare almeno `memory_limit=512M`, `max_execution_time=300` e limiti upload/post adeguati (ad esempio 64M). Verificare anche il timeout PHP-FPM/proxy: un caricamento interrotto non va confermato. Non cambiare questi valori per risolvere errori di tracciato.
4. Selezionare l’azienda destinataria, poi Importazioni → **Originali DATEV Koinos — acquisizione guidata**.
5. Caricare un file alla volta (oppure uno ZIP), verificare anteprima, azienda e segnalazioni, quindi confermare. Per comodità iniziare da piano dei conti e anagrafiche.

L’acquisizione è distinta dalla migrazione contabile completa: caricare un file non significa averne contabilizzato il contenuto.

## Gestione dei tracciati

| Fonte | Risultato |
|---|---|
| Piano dei conti CSV DK | Conti nuovi con gerarchia; nessuna sovrascrittura. Conti d’ordine conservati da classificare. Verificare il raccordo con gli automatismi Luna2. |
| Lista clienti/fornitori CSV DK | Anagrafiche nuove con codici originali; controllo doppioni per codice, P. IVA e CF. Indirizzi/paesi non determinabili restano da completare nell’archivio. |
| Causali CSV DK | Causali nuove disattivate, da configurare prima dell’uso; configurazioni esistenti mantenute. |
| Articoli IVA, aliquote IVA, conti per tributo | Cataloghi di riferimento separati, senza abbinamenti fiscali presunti. |
| Categorie, aliquote e conti cespiti | Le categorie vengono create; aliquote e conti originali restano tracciati per il raccordo. I conti non vengono associati se la fonte non è univoca. |
| XLS BACespiti (anagrafica completa) | Crea i cespiti operativi con codice, categoria, date, costo, aliquote e stato DATEV. Non sovrascrive codici già presenti con dati diversi. |
| XLS BAMovimenti (movimenti completi) | Collega acquisizioni, ammortamenti, incrementi, decrementi, alienazioni ed eliminazioni ai cespiti. Conserva i riferimenti anche quando la fonte non espone gli importi. |
| XLS CGPrimaNota | Testate storiche consultabili. Senza le righe conto/Dare/Avere non vengono create scritture o saldi. |
| XLS BAProgressivi | Importa tutti i progressivi annuali e aggiorna il saldo civile del cespite alla data più recente, dopo avere acquisito BACespiti. |
| PDF libri giornale | Originali scaricabili e riconosciuti come evidenza. Per le registrazioni operative usare i CSV quadrati prodotti da `tools/datev/extract_journal_pdf.py`. |
| PDF registri IVA | Originali scaricabili per controllo e riconciliazione dei movimenti IVA. |
| PDF registri cespiti | Evidenza annuale civile/fiscale, distinta dagli altri PDF. Per il 2026 aperto si usano saldo 2025, anagrafica e movimenti 2026. |
| ZIP fatture inviate/ricevute | XML storici e indici XLS consultabili. Nessuna nuova contabilizzazione, scadenza, invio SDI o duplicazione dei registri. |

**Le fatture archiviate in questa modalità non diventano documenti operativi nelle sezioni Vendite/Acquisti.** Il percorso distinto “Fatture XML FatturaPA” è operativo e non deve essere usato indiscriminatamente per lo storico già contabilizzato.

## Consultazione e controlli

Aprire il lotto dalla pagina Importazioni: ricerca nel contenuto, filtro per tipo, pagine da 50 righe, riepilogo esiti e download originali con verifica SHA-256. Tutte le consultazioni sono limitate all’azienda corrente e ai ruoli titolare/amministratore/contabile.

- **Applicato:** dato operativo inserito oppure già presente e compatibile.
- **Riferimento:** dato archiviato, non utilizzato dai calcoli operativi.
- **Da completare:** mancano informazioni o configurazioni per l’utilizzo operativo.
- **Da riconciliare:** chiave già presente con contenuto diverso; nessuna sovrascrittura.

Ricaricare dati identici non crea duplicati. Una versione diversa rimane separata e segnalata. Un’analisi interrotta non è confermabile: correggere e ricaricare il file. Non è previsto un annullamento massivo di questa modalità; ripristinare il backup di collaudo se necessario.

## Ordine operativo per il passaggio 2025-2026

1. Acquisire piano dei conti, clienti/fornitori, causali, IVA e tabelle di raccordo.
2. Acquisire categorie cespiti, `Anagrafica completa cespiti.xls`, `Cespiti progressivi.xls` e `Movimenti completi cespiti.xls`.
3. Archiviare i registri cespiti 2016-2025. Il 2015 è necessario soltanto come evidenza per i beni storici; non si importa la contabilità generale 2015.
4. Acquisire gli XLS di prima nota e i due libri giornale PDF come originali DATEV.
5. Generare i CSV contabili 2025 e 2026 dal libro giornale analitico. Il convertitore interrompe il lavoro se anche una sola registrazione o il totale del libro non quadrano.
6. Caricare i CSV da Importazioni → Movimenti contabili, dopo il piano dei conti. I periodi 2025 e 2026 devono essere aperti.
7. Acquisire registri IVA e XML FatturaPA; riconciliare totali contabili, registri IVA, clienti/fornitori e documenti senza duplicare le fatture già comprese nelle scritture.
8. Concludere soltanto dopo verbale di quadratura firmato dal responsabile contabile/commercialista.

L’interfaccia distingue sempre dati applicati, fonti di controllo e anomalie. Nessun valore mancante viene inventato.

## Collaudo tecnico

`tests/datev_sources.php <cartella-export>` verifica i 16 export di collaudo originali usando esclusivamente MariaDB locale sulla porta 33317 e un database casuale eliminato a fine test. Nessun dato cliente è incluso nel repository. Le copie di lavoro rimangono nello storage privato del collaudo.
