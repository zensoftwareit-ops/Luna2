# Luna2 — Roadmap licenze, ruoli e setup aziendale

Documento di pianificazione ricavato dalla specifica `LUNA2_GUIDA_LICENZE_RUOLI_SETUP_CODEX.md` e confrontato con l'architettura PHP attuale di Luna2.

## 1. Obiettivo

Introdurre in Luna2 un sistema completo e verificabile per:

- licenze associate all'installazione e sincronizzate con Luna Commerce;
- moduli commerciali abilitati esclusivamente dalla licenza;
- amministrazione autonoma dell'azienda da parte del cliente;
- ruoli e permessi applicati in modo uniforme a interfaccia, API, CLI, cron, importazioni ed esportazioni;
- limite utenti previsto dal piano;
- configurazione aziendale guidata e riprendibile;
- funzionamento temporaneamente offline con periodo di tolleranza;
- modalità limitata dopo scadenza, revoca o superamento della tolleranza, senza perdita dei dati.

Il superuser tecnico Zen resta separato dagli utenti aziendali e non deve concorrere al limite di licenza.

## 2. Valutazione dello stato attuale

| Area | Stato attuale | Divario da colmare |
|---|---|---|
| Superuser tecnico | Già creato dalla migrazione e in grado di selezionare un'azienda | Va separato formalmente dagli utenti licenziati e dotato di pannello tecnico licenza/audit |
| Ruoli | `SUPERUSER`, `OWNER`, `ADMIN`, ruoli di reparto e `VIEWER`; controlli distribuiti nei controller | Serve un livello centralizzato di permessi; i ruoli esistenti vanno preservati e mappati senza regressioni |
| Gestione azienda e utenti | Concentrata nel pannello del superuser | `OWNER`/`ADMIN` devono poter configurare la propria azienda e gestire i propri utenti |
| Moduli | Tabella `module_settings` e valori predefiniti di configurazione | La licenza deve essere la fonte dei diritti commerciali; le impostazioni aziendali possono solo disattivare/configurare ciò che è licenziato |
| Controllo accessi ai moduli | Presente solo in parte e soprattutto nelle pagine indice | Va applicato a ogni route e servizio, comprese azioni dirette, job, cron, API, export e integrazioni |
| Setup | Checklist di onboarding già disponibile | Va trasformata in procedura guidata persistente, riprendibile e filtrata in base ai moduli licenziati |
| Licenza | Non esiste un dominio applicativo completo | Servono identità installazione, cache locale firmata, sincronizzazione, grace period, override e log |
| Limite utenti | Non applicato | Va verificato in creazione, riattivazione, importazione, API e operazioni concorrenti |
| Modalità limitata | Non presente | Va introdotta una matrice esplicita lettura/scrittura/export/job, senza cancellare dati |
| Audit | `audit_logs` già presente | Va esteso con ruolo, origine, request ID e valori prima/dopo |
| Cron | `cron:daily` esegue attività per tutte le aziende attive | Ogni attività deve rispettare licenza, modulo e modalità operativa |

## 3. Decisioni architetturali consigliate

### 3.1 Licenza per installazione

La licenza viene legata a un'identità stabile dell'installazione e al dominio canonico. Il modello dati può restare multi-azienda, ma il piano standard deve consentire una sola azienda cliente operativa. L'azienda tecnica di piattaforma non viene conteggiata.

Per piani Enterprise, demo o assistenza sarà possibile prevedere `max_organizations` senza dover riprogettare il database.

### 3.2 Ruoli compatibili con l'esistente

Non sostituire bruscamente i ruoli specialistici già usati. Introdurre un catalogo di permessi e una mappatura compatibile:

| Ruolo attuale | Famiglia richiesta | Indicazione |
|---|---|---|
| `SUPERUSER` | `ROLE_SUPERUSER` | Tecnico Zen, fuori dal conteggio utenti |
| `OWNER`, `ADMIN` | `ROLE_COMPANY_ADMIN` | Amministrazione azienda, utenti e configurazioni |
| nuovo `MANAGER` | `ROLE_MANAGER` | Supervisione, approvazioni e report nei moduli autorizzati |
| `ACCOUNTANT`, `SALES`, `WAREHOUSE`, `HR` | `ROLE_OPERATOR` con profilo specialistico | Conservare le specializzazioni come pacchetti di permessi |
| nuovo `OPERATOR` | `ROLE_OPERATOR` | Profilo operativo generico |
| `VIEWER` | `ROLE_READONLY` | Sola lettura ed export consentiti |

I controller dovranno chiedere permessi come `customers.read`, `invoices.write` o `accounting.export`, non elenchi rigidi di ruoli.

### 3.3 Separazione fra diritto commerciale e configurazione

La regola effettiva deve essere:

`modulo disponibile = modulo base oppure (modulo presente in licenza e abilitato nella configurazione aziendale)`

Un override tecnico temporaneo e motivato può modificare questa regola, ma deve avere una scadenza e produrre un evento di audit.

`module_settings` rimane utile per configurazione e disattivazione aziendale, ma non può concedere un modulo non acquistato.

### 3.4 Controlli centralizzati e fail-safe

Introdurre:

- `LicenseService` per stato, cache, sincronizzazione e periodo di tolleranza;
- `EntitlementResolver` per calcolare i moduli effettivi;
- `PermissionGate` per ruoli e permessi;
- metadati di route: `permission`, `module`, `operation` (`READ`, `WRITE`, `EXPORT`, `TECHNICAL`);
- controlli equivalenti nel livello servizi per job, CLI e integrazioni che non attraversano il router.

Se lo stato non è determinabile, il sistema non deve concedere nuove funzioni commerciali. Durante la tolleranza conserva invece l'ultimo stato firmato valido.

## 4. Mappatura iniziale dei moduli

Questa mappatura consente di mantenere compatibilità con le chiavi esistenti e, nello stesso tempo, presentare un catalogo commerciale più chiaro.

| Modulo commerciale | Componenti Luna2 attuali | Tipo |
|---|---|---|
| Dashboard | dashboard | Base |
| Anagrafiche | `anagraphics` | Base |
| Vendite | `sales` | Base |
| Acquisti | `purchases` | Base |
| Prodotti e magazzino base | parte base di `inventory` | Base |
| Fatturazione elettronica | funzioni XML/documentali di vendita | Base, endpoint tecnici configurabili |
| Contabilità | `accounting` | Opzionale |
| Operatività | `projects`, `calendar`, logistica avanzata, trasferimenti e picking | Opzionale |
| Centro professionale | `professional` | Opzionale |
| E-commerce Hub | `ecommerce` | Opzionale |
| Noleggio e ticketing | `rental` | Opzionale |
| Comunicazioni | nuova chiave esplicita `communications` | Opzionale |
| Report direzionali | nuova chiave esplicita `management_reports` | Opzionale |
| CRM | `crm` | Opzionale |
| HR base | anagrafiche dipendenti e presenze, oggi in `hr` | Opzionale |
| Ferie e paghe | ferie, approvazioni e payroll, da separare da `hr` | Opzionale |

Le importazioni sono consigliate come capacità amministrativa/base (`imports.manage`), non come modulo commerciale, salvo diversa scelta di listino.

## 5. Modello dati proposto

Le modifiche dovranno essere introdotte con migrazioni incrementali a partire dalla successiva alla `009`, senza riscrivere quelle già distribuite.

### Migrazione 010 — Identità installazione e licenza

- `instance_identity`: UUID installazione, instance ID, dominio canonico, ambiente, versione e stato;
- `licenses`: piano, stato, limiti, binding, payload firmato, moduli derivati, date, ultimo sync valido, grace period ed errore di sync;
- conservazione della chiave come dato cifrato tramite `APP_KEY`, più hash/fingerprint e valore mascherato;
- vincolo che impedisca più licenze attive concorrenti per la stessa installazione.

### Migrazione 011 — Entitlement, sincronizzazioni e override

- `license_modules`: diritti normalizzati derivati dal payload firmato;
- `license_sync_logs`: esito, request ID, tempi ed errore, senza segreti;
- `license_overrides`: modulo/stato, motivo obbligatorio, autore, validità e revoca.

### Migrazione 012 — Ruoli e permessi

- catalogo `permissions` e assegnazioni per profilo;
- compatibilità con i valori ruolo esistenti;
- eventuale evoluzione del campo ruolo da `ENUM` a codice estensibile;
- `account_type` su utenti: `HUMAN`, `SYSTEM`, `TECHNICAL`;
- indici e vincoli necessari al conteggio atomico degli utenti attivi.

### Migrazione 013 — Setup persistente

- stato setup aziendale: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `RESET_REQUIRED`;
- passo corrente, versione del wizard, date, autore e dati intermedi validati;
- riuso di `onboarding_progress` per la cronologia dei passi completati.

### Migrazione 014 — Audit esteso

- ruolo dell'attore;
- origine (`WEB`, `API`, `CLI`, `CRON`, `WEBHOOK`);
- request/correlation ID;
- valori `before` e `after`, con mascheramento dei dati sensibili.

## 6. Roadmap di implementazione

Le durate sono stime di sviluppo e collaudo tecnico. Il lavoro sull'API Luna Commerce può procedere in parallelo.

### Fase 0 — Contratto funzionale e inventario delle superfici (1–2 giorni)

Attività:

- congelare nomi, codici e dipendenze dei moduli commerciali;
- definire il contratto JSON dell'API licenze e gli stati ammessi;
- censire tutte le route, comandi CLI, cron, export, webhook e servizi mutativi;
- assegnare a ciascuna superficie modulo, permesso e tipo di operazione;
- approvare la matrice di modalità limitata.

Criterio di uscita: nessuna route o processo applicativo resta privo di classificazione.

### Fase 1 — Fondazione autorizzazioni (4–6 giorni)

Attività:

- creare catalogo permessi e profili compatibili con i ruoli correnti;
- implementare `PermissionGate` e helper condivisi;
- estendere il router con i metadati di sicurezza;
- sostituire progressivamente gli elenchi di ruoli nei controller;
- aggiungere test automatici della matrice ruolo/permesso.

Criterio di uscita: accesso negato in modo identico da menu, URL diretto e chiamata POST/API.

### Fase 2 — Identità installazione e dominio licenza locale (2–4 giorni)

Attività:

- applicare le migrazioni 010–011;
- generare in modo idempotente l'UUID al primo setup;
- implementare repository e servizi licenza;
- cifrare la chiave e mostrare solo il fingerprint;
- implementare stati, date, limiti e cache firmata.

Criterio di uscita: un'installazione può essere identificata e può caricare/validare un payload di licenza locale senza dipendere ancora dalla rete.

### Fase 3 — Client Luna Commerce e protocollo sicuro (4–6 giorni)

Attività Luna2:

- client per attivazione, validazione, sincronizzazione e disattivazione;
- timeout brevi, retry con backoff, request ID e protezione replay;
- firma delle richieste e verifica della firma delle risposte;
- mock server e fixture per tutti gli stati licenza.

Attività Luna Commerce/WordPress, dipendenza esterna:

- endpoint corrispondenti;
- associazione chiave–piano–istanza–dominio;
- rotazione/revoca delle credenziali;
- rate limiting e audit server-side.

Scelta raccomandata: richieste autenticate con HMAC e payload licenza firmato asimmetricamente (Ed25519), verificabile in Luna2 tramite una chiave pubblica. Il solo segreto HMAC nel client è un fallback meno robusto.

Criterio di uscita: payload alterati, scaduti o riferiti a un'altra installazione vengono rifiutati.

### Fase 4 — Entitlement e protezione completa dei moduli (5–8 giorni)

Attività:

- implementare `EntitlementResolver`;
- convertire `module_settings` in configurazione subordinata alla licenza;
- applicare controlli centralizzati a tutte le route;
- proteggere servizi richiamati da CLI, cron, API, webhook, import/export e operazioni massive;
- separare magazzino base da logistica avanzata;
- separare HR base da ferie/paghe;
- introdurre le chiavi `communications` e `management_reports`;
- impedire al superuser di concedere permanentemente moduli non licenziati tramite semplici flag locali.

Criterio di uscita: un test di copertura fallisce se viene aggiunta una nuova route business senza policy.

### Fase 5 — Amministratore aziendale e limite utenti (4–6 giorni)

Attività:

- consentire a `OWNER`/`ADMIN` di gestire dati della propria azienda;
- consentire creazione, modifica, disattivazione e reset degli utenti della propria azienda;
- impedire accesso o modifica di organizzazioni diverse;
- escludere account tecnici e di sistema dal conteggio;
- applicare `max_users` a creazione, riattivazione, import, API e procedure massive;
- rendere atomico il controllo per evitare superamenti tramite richieste concorrenti;
- mostrare utenti usati/disponibili e percorso di upgrade.

Criterio di uscita: il cliente completa la gestione ordinaria senza superuser e il limite non è aggirabile.

### Fase 6 — Wizard aziendale riprendibile (5–7 giorni)

Passi consigliati:

1. verifica licenza e associazione installazione;
2. dati aziendali e sedi;
3. utenti iniziali e ruoli;
4. impostazioni fiscali e numerazioni;
5. piano dei conti e parametri IVA, se Contabilità è licenziata;
6. magazzini, causali e logistica, in base al piano;
7. email, PEC, fatturazione elettronica e relativi endpoint configurabili;
8. moduli opzionali licenziati;
9. importazione dati;
10. riepilogo, test configurazione e completamento.

Attività:

- persistere stato e dati dopo ogni passo;
- saltare i passi dei moduli non licenziati;
- validare ogni passo sia lato client sia lato server;
- reindirizzare gli amministratori verso il wizard finché i requisiti minimi non sono completati;
- consentire al superuser reset e assistenza con audit.

Criterio di uscita: il browser può essere chiuso in qualsiasi momento e il setup riprende dal punto corretto.

### Fase 7 — Sync, tolleranza e modalità limitata (3–5 giorni)

Attività:

- sincronizzazione pianificata e comando CLI manuale;
- cache dell'ultimo payload valido;
- periodo di tolleranza configurato a 7 giorni;
- avvisi progressivi in interfaccia;
- blocco coerente delle scritture e dei job in uscita dopo la tolleranza;
- revoca immediata in caso di stato `REVOKED` firmato;
- pagina di recupero licenza sempre raggiungibile.

Matrice proposta:

| Stato | Lettura | Export | Scrittura | Job/invii esterni | Pannello licenza |
|---|---:|---:|---:|---:|---:|
| `ACTIVE` | Sì | Sì | Sì | Sì | Sì |
| API non raggiungibile, entro 7 giorni | Sì | Sì | Sì | Sì, con avviso | Sì |
| `PAST_DUE`, secondo politica commerciale, entro grace | Sì | Sì | Sì | Sì, con avviso | Sì |
| Grace terminato / `SUSPENDED` / `EXPIRED` | Sì | Sì | No | No | Sì |
| `REVOKED` | Sì | Sì | No | No | Sì |

Nessuno stato deve eliminare o modificare automaticamente i dati aziendali.

### Fase 8 — Pannelli e UX (4–6 giorni)

Pannello tecnico superuser:

- identità installazione, dominio, ambiente e versione;
- chiave mascherata, piano, stato, scadenza, ultimo sync e fine grace;
- moduli effettivi e motivazione del loro stato;
- sync manuale, diagnostica e override temporaneo motivato;
- cronologia tecnica e log privi di segreti.

Pannello amministratore aziendale:

- piano, stato, utenti consumati e moduli disponibili;
- configurazione dei moduli licenziati;
- avvisi di rinnovo/upgrade;
- nessun accesso a credenziali, segreti tecnici o override.

Criterio di uscita: ogni blocco o modulo disabilitato spiega chiaramente causa e azione possibile.

### Fase 9 — Upgrade, downgrade, audit e hardening (3–5 giorni)

Attività:

- applicare upgrade senza logout o reinstallazione;
- preservare dati e relazioni dei moduli rimossi dal piano;
- consentire lettura/export dei dati storici in modalità limitata, secondo matrice approvata;
- sospendere job e webhook dei moduli non più autorizzati;
- estendere audit e correlation ID;
- mascherare chiavi e payload sensibili;
- controllare CSRF, sessioni, rate limit, tenant isolation e gestione errori.

Criterio di uscita: cambio piano reversibile, tracciato e privo di perdita dati.

### Fase 10 — Collaudo e rilascio progressivo (5–8 giorni)

Introdurre tre modalità operative:

- `shadow`: calcola e registra le decisioni, ma non blocca;
- `warn`: mostra avvisi e raccoglie anomalie;
- `enforce`: applica realmente blocchi e limiti.

Sequenza di rilascio:

1. backup e migrazioni additive;
2. deploy in `shadow` sulle installazioni esistenti;
3. associazione e verifica licenza;
4. analisi dei log e correzione delle route non classificate;
5. passaggio a `warn`;
6. collaudo cliente e superuser;
7. passaggio esplicito a `enforce`;
8. monitoraggio di sync, cron, errori autorizzativi e code ferme.

Questo evita che una licenza non ancora configurata blocchi improvvisamente installazioni già operative.

## 7. Piano di test obbligatorio

### Autorizzazione

- matrice completa ruolo/permesso/modulo;
- accesso da menu, URL diretto, POST manuale e API;
- isolamento fra aziende;
- superuser escluso dai limiti ma sempre tracciato.

### Licenza

- attivazione valida, binding errato, dominio cambiato e istanza clonata;
- firma errata, payload alterato, risposta ripetuta e clock skew;
- tutti gli stati, scadenza e passaggi temporali simulati;
- API indisponibile da 1 a oltre 7 giorni;
- revoca, rinnovo, upgrade e downgrade.

### Moduli

- route indice e azioni dirette;
- servizi, cron, CLI, webhook, esportazioni e importazioni;
- separazione fra funzioni base e avanzate di magazzino/HR;
- dati storici ancora leggibili dopo downgrade.

### Utenti

- creazione fino al limite e oltre il limite;
- riattivazione, import e API;
- due creazioni contemporanee sull'ultimo posto disponibile;
- esclusione corretta di utenti tecnici/sistema.

### Setup

- interruzione e ripresa a ogni passo;
- variazione dei moduli durante il wizard;
- reset tecnico tracciato;
- validazione dei dati fiscali e contabili;
- migrazione di un database esistente fermo alla versione `009`.

## 8. Dipendenze e rischi

| Rischio/dipendenza | Mitigazione |
|---|---|
| API WordPress non ancora disponibile | Sviluppare subito contratto, mock e client; mantenere il server come attività parallela |
| Controlli ruolo sparsi | Inventario automatico e test che richieda una policy per ogni route business |
| Installazioni esistenti senza licenza | Rollout `shadow` → `warn` → `enforce`, mai enforcement automatico al deploy |
| Ruoli specialistici già assegnati | Mappatura compatibile e migrazione graduale verso permessi |
| Un'unica licenza in installazioni multi-azienda | Piano standard con una sola azienda licenziata; limite organizzazioni esplicito per Enterprise/demo |
| Concessione moduli tramite configurazione locale | Licenza firmata come unica fonte degli entitlement; override solo temporaneo e auditato |
| Chiave rubata dal database o dai log | Cifratura con `APP_KEY`, fingerprint, mascheramento e log senza segreti |
| Cron che continua a operare dopo sospensione | Guardie nel livello servizio e test specifici per ogni job |

## 9. Decisioni da confermare prima della Fase 3

La roadmap può partire con questi valori consigliati, salvo diversa indicazione commerciale:

1. una sola azienda operativa per licenza standard; multi-azienda solo nei piani che dichiarano `max_organizations`;
2. tolleranza offline di 7 giorni;
3. lettura ed export consentiti dopo la tolleranza, scritture e invii bloccati;
4. fatturazione elettronica base inclusa, con endpoint configurabili ma protetti da permesso tecnico;
5. importazioni incluse nel setup/amministrazione e non vendute come modulo separato;
6. payload licenza firmato con Ed25519 e richieste client autenticate con HMAC;
7. account `SUPERUSER`, `SYSTEM` e `TECHNICAL` esclusi dal numero massimo utenti.

## 10. Ordine pratico dei primi interventi

Per ridurre al minimo il rischio di regressioni, il primo ciclo di sviluppo deve produrre, nell'ordine:

1. inventario route e matrice policy versionata;
2. `PermissionGate` compatibile con tutti i ruoli esistenti;
3. migrazioni additive per identità e licenza;
4. `LicenseService` con mock locale e modalità `shadow`;
5. copertura centralizzata delle route e poi dei servizi/cron;
6. pannello amministratore aziendale e limite utenti;
7. wizard;
8. collegamento all'API reale;
9. modalità limitata e progressiva attivazione dell'enforcement.

## 11. Stima complessiva

La stima ragionevole è **35–55 giornate di sviluppo e collaudo**, pari a circa **7–10 settimane di calendario** con il lavoro sull'API Luna Commerce svolto in parallelo. La parte più delicata non è l'interfaccia, ma la copertura uniforme di tutte le superfici applicative e la compatibilità con installazioni e ruoli già esistenti.

## 12. Definition of Done

Il progetto è completato quando:

- licenza e binding vengono verificati crittograficamente;
- ogni superficie business ha modulo, permesso e tipo di operazione dichiarati;
- il cliente amministra azienda e utenti senza accesso tecnico;
- il limite utenti è coerente e non aggirabile;
- il wizard è riprendibile e mostra solo passi pertinenti;
- offline, grace, sospensione, revoca, upgrade e downgrade rispettano la matrice approvata;
- dati e storico non vengono cancellati in caso di downgrade o scadenza;
- cron, API, export, importazioni e integrazioni rispettano le stesse policy della UI;
- audit e log non espongono segreti;
- il passaggio `shadow` → `warn` → `enforce` è stato collaudato su una copia realistica dei dati;
- documentazione tecnica, procedura Plesk e manuale amministratore sono aggiornati.
