# Partitario clienti e fornitori

## Modello contabile

Luna2 mantiene nel piano dei conti due conti collettivi configurati:

- **Crediti verso clienti** (`TRADE_RECEIVABLES`);
- **Debiti verso fornitori** (`TRADE_PAYABLES`).

Ogni anagrafica cliente o fornitore è un sottoconto analitico del rispettivo conto collettivo. Non viene creato un conto distinto nel piano dei conti per ogni soggetto: il collegamento è conservato sulle righe di prima nota tramite `customer_id` o `supplier_id`.

## Associazione automatica

Per fatture create in Luna2 o importate come **Fatture XML FatturaPA**:

1. la controparte viene cercata principalmente per Partita IVA;
2. se manca, viene creata l’anagrafica cliente o fornitore;
3. se manca il codice, Luna2 assegna un codice `CLI-…` o `FOR-…`;
4. la riga sul conto collettivo viene collegata al sottoconto;
5. lo stesso collegamento viene mantenuto su note di credito, incassi, pagamenti e relativi storni.

Le note di credito riducono il saldo della controparte e sono esposte con segno opposto nello scadenzario del partitario.

## Consultazione

Aprire **Contabilità → Partitario clienti/fornitori** e scegliere **Clienti** o **Fornitori**. Il prospetto mostra:

- tutte le anagrafiche, anche senza movimenti;
- saldo iniziale alla data `Dal`;
- Dare e Avere del periodo;
- saldo finale e saldo progressivo;
- fatture, note di credito e residuo da incassare/pagare;
- esportazione PDF, XLSX e CSV.

Il totale dei sottoconti deve essere riconciliato con il saldo del conto collettivo dello stesso periodo.

## Riconciliazione dello storico DATEV

Le vecchie scritture DATEV possono essere contabilmente corrette ma prive dell’ID tecnico della controparte. All’apertura del partitario Luna2 tenta un abbinamento, senza modificare conto o importi, usando nell’ordine:

1. collegamento al documento Luna2;
2. numero documento e anno;
3. Partita IVA;
4. codice fiscale;
5. denominazione normalizzata e univoca.

I casi senza corrispondenza univoca compaiono in **Riconciliazione storico DATEV → Movimenti ancora senza sottoconto**. Per completarli:

1. scegliere la scheda Clienti o Fornitori corretta;
2. premere **Associa** sulla riga;
3. selezionare l’anagrafica;
4. confermare;
5. ripetere finché il contatore `da associare` è zero.

L’operazione è auditata e cambia esclusivamente la dimensione analitica della riga. Non modifica Dare, Avere, protocollo, conto o quadratura.

## Controlli obbligatori

Per almeno cinque clienti e cinque fornitori significativi verificare:

- fattura e nota di credito nel sottoconto corretto;
- pagamento/incasso e saldo progressivo;
- residuo uguale allo scadenzario;
- totale sottoconti uguale al conto collettivo;
- assenza di righe ancora da associare alla data di cutover.

Se una fattura XML ha generato una nuova anagrafica invece di usare quella esistente, non associare manualmente in massa: verificare prima duplicati di Partita IVA e decidere quale anagrafica conservare.
