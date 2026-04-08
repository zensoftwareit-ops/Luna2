# 📅 Scadenzario Fiscale - Quick Start Guide

## Panoramica
Modulo per gestire tutte le scadenze fiscali obbligatorie di una SRL italiana.

## Accesso
**Path**: `/app/contabilita/scadenzario-list`

**Menu**: Contabilità → Scadenzario Fiscale

## Come Usare

### 1. Popolare le Scadenze Standard
- Login come **admin**
- Vai a: Contabilità → Scadenzario Fiscale
- Clicca: **"Popola Scadenzario Standard"**
- ✅ Sistema crea automaticamente le 10+ scadenze fiscali italiane dell'anno

### 2. Creare una Nuova Scadenza Manuale
- Clicca: **"Nuova Scadenza"**
- Compila:
  - **Titolo**: Es. "F24 Aprile IVA"
  - **Data Scadenza**: Es. 20/04/2026
  - **Tipo**: IVA | INPS | IRPEF | IRES | IRAP | F24 | BOLLO | ALTRO
  - **Importo** (opzionale): Es. 1500.00
  - **Note**: Descrizione/istruzioni per il team
- Clicca: **Salva Scadenza**

### 3. Filtrare per Stato
**Dropdown "Stato":**
- **Tutti** = Mostra tutte le scadenze aperte
- **Scadute** = Scadenze dove data < oggi (priorità alta!)
- **Prossimi 30 gg** = Scadenze fra 0 e 30 giorni (attenzione!)

### 4. Filtrare per Tipo
**Dropdown "Tipo":**
- IVA
- INPS  
- IRPEF
- F24
- ... e altri

### 5. Segna come Completata
- Nella tabella, clicca: **✓ (check circle)**
- Status diventa "COMPLETATA", data completion registrata
- Non sparisce dalla lista, ma mostra che è done

### 6. Modifica/Elimina
- **Matita** (edit icon): Modifica la scadenza
- **Cestino** (delete icon): Elimina (conferma richiesta)

## Dashboard Statistiche
Card in alto a sinistra mostrano:
- 🔴 **Scadute** = Quante scadenze sono passate senza completamento
- 🟡 **Prossimi 7 giorni** = Quante scadenze nei prossimi 7 gg
- 🔵 **Totale Aperte** = Tutte le scadenze non ancora completate

## Scadenze Standard Pre-Configurate

| Scadenza | Data | Frequenza | Note |
|----------|------|-----------|------|
| F24 IVA Mensile | 20 di ogni mese | Mensile | Versamento IVA |
| IVA Trimestrale | 16 apr/lug/ott/gen | Trimestrale | Dichiarazione IVA |
| E3 (Ritenute) | 30 aprile | Annuale | Comunicazione ritenute fiscali |
| CUD Dipendenti | 31 gennaio | Annuale | CUD da consegnare a dipendenti |
| INPS Mensile | 16 del mese | Mensile | Versamento contributi INPS |
| UNICO/IRES | 30 giugno | Annuale | Dichiarazione dei redditi |
| IRAP | 20 maggio | Annuale | Imposta regionale attività produttive |
| Bilancio | 15 dicembre | Annuale | Chiusura esercizio |

## API JSON Endpoints

### Statistiche Dashboard
```
GET /app/contabilita/scadenzario-dashboard
Response: { stats: {openCount, overdueCount, upcoming7Count, count_IVA, ...}, latestDeadlines: [...] }
```

### Scadenze Imminenti (7 giorni)
```
GET /app/contabilita/scadenzario-upcoming
Response: { success: true, deadlines: [...], count: N }
```

### Scadenze Scadute
```
GET /app/contabilita/scadenzario-overdue
Response: { success: true, deadlines: [...], count: N }
```

## Tips & Tricks

1. **Per team finance**: Stampare la dashboard ogni mese per review interna
2. **Settimanalmente**: Controllare "Prossimi 7 giorni" il lunedì mattina
3. **Alert**: Se scadute & count > 0, contattare commercialista
4. **Backup**: Le scadenze si sincronizzano automaticamente nel DB
5. **Recurring**: Dopo completamento annuale, scadenze ANNUAL si auto-ripetono l'anno prossimo (feature futura)

## Troubleshooting

**Q: Le scadenze standard non appaiono?**
- A: Accedi come admin → clicca "Popola Scadenzario Standard"

**Q: Come aggiungere scadenze ricorrenti (mensili/trimestrali)?**
- A: Crea manualmente, seleziona "Frequenza" = MONTHLY/QUARTERLY

**Q: Posso modificare una scadenza già completata?**
- A: Sì, clicca la matita e cambia stato o data

**Q: Ho un errore nel salvataggio?**
- A: Controlla: titolo obbligatorio, data valida, credenziali admin

## Versione
- **Modulo**: Scadenzario Fiscale v1.0
- **Release**: Q2 2026
- **Stato**: Production Ready ✅
