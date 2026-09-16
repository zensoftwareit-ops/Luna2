# Ripristino completo di Luna2

La funzione è disponibile esclusivamente al superuser in **Stato del sistema → Ripristino completo del sistema**.

## Dati sempre conservati

- superuser che esegue l'operazione;
- organizzazione tecnica `Luna2 Platform` del superuser;
- migrazioni applicate;
- identità dell'installazione;
- licenza, moduli licenziati, storico e override della licenza;
- log tecnici su filesystem e sessione corrente.

## Modalità disponibili

### Mantieni aziende e utenti

Conserva le anagrafiche delle aziende e tutte le credenziali utente. Elimina ogni altro dato, incluse configurazioni moduli, preferenze, documenti, contabilità, anagrafiche clienti/fornitori, magazzino, HR, integrazioni, importazioni e file generati.

### Conserva soltanto il superuser

Elimina anche tutte le aziende operative e tutti gli utenti. Rimangono soltanto il superuser e la sua organizzazione tecnica. L'eventuale collegamento della licenza a un'azienda eliminata viene azzerato; la licenza di installazione non viene cancellata.

## Protezioni richieste

Per avviare il ripristino servono tutte le seguenti conferme:

1. schema database completamente aggiornato;
2. accesso come superuser;
3. conferma di avere un backup verificato di database e `storage`;
4. password corrente del superuser;
5. digitazione esatta della frase `RESET LUNA2`;
6. ulteriore conferma del browser.

## File eliminati

Vengono svuotati i contenuti di:

- `storage/cache`;
- `storage/imports`;
- `storage/private`;
- `storage/exports`.

Non vengono eliminati `storage/logs` e `storage/sessions`.

## Dopo il ripristino

- con utenti conservati, selezionare nuovamente l'azienda e riconfigurare i moduli;
- con solo superuser, aprire **Aziende e utenti** e creare la prima azienda;
- verificare la licenza dalla pagina **Licenza**;
- non eseguire nuovamente `migrate`: lo schema e lo storico migrazioni sono conservati.
