package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * NoleggioContratto - Contratto  attivo e scadenzario (Fase 5)
 * 
 * Gestisce il contratto di noleggio attivo con integrazione calendario Google/iCloud
 * per gestione scadenzario automatico: rinnovi, revisioni, tagliandi, km, patenti, etc.
 * 
 * LOGICA AUTOMAZIONE (Fase 5) con CALENDARIO GOOGLE/ICLOUD:
 * - Alert rinnovo contratto 4-6 mesi prima scadenza → CalendarEvent NOLEGGIO_RINNOVO
 * - Verifica chilometrica ogni 6 mesi → CalendarEvent NOLEGGIO_VERIFICA_KM
 * - Scadenze revisione auto → CalendarEvent NOLEGGIO_REVISIONE
 * - Scadenze tagliandi manutenzione → CalendarEvent NOLEGGIO_TAGLIANDO
 * - Scadenze patente conducente → CalendarEvent NOLEGGIO_PATENTE
 * - Scadenze assicurazione → CalendarEvent NOLEGGIO_ASSICURAZIONE
 * 
 * Tutti gli eventi vengono SINCRONIZZATI automaticamente con Google Calendar / iCloud
 * tramite il CalendarSyncService esistente.
 * 
 * DB: noleggio_contratti
 * 
 * @author Luna2 CRM - Modulo Broker Noleggio
 */
@Entity
@Table(name = "noleggio_contratti")
public class NoleggioContratto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ==================== ENUMERATIONS ====================
    
    /**
     * Status - Stati del contratto
     */
    public enum Status {
        ATTIVO,                 // Contratto attivo
        IN_SCADENZA,            // Mancano 4-6 mesi alla scadenza
        PROPOSTA_RINNOVO,       // Proposta rinnovo inviata
        RINNOVATO,              // Contratto rinnovato
        SCADUTO,                // Contratto scaduto
        RISOLTO_ANTICIPATAMENTE,// Cliente ha risolto prima scadenza
        ANNULLATO               // Contratto annullato
    }
    
    /**
     * TipoContratto - Durata/tipo contratto
     */
    public enum TipoContratto {
        NOLEGGIO_12_MESI,       // 12 mesi
        NOLEGGIO_24_MESI,       // 24 mesi
        NOLEGGIO_36_MESI,       // 36 mesi (più comune)
        NOLEGGIO_48_MESI,       // 48 mesi
        NOLEGGIO_60_MESI,       // 60 mesi
        NOLEGGIO_OPERATIVO,     // Noleggio operativo aziendale
        NOLEGGIO_FINANZIARIO    // Leasing finanziario
    }
    
    /**
     * PeriodicoPagamento - Frequenza pagamento canone
     */
    public enum PeriodicoPagamento {
        MENSILE,                // Canone mensile
        TRIMESTRALE,            // Canone trimestrale
        SEMESTRALE,             // Canone semestrale
        ANNUALE                 // Canone annuale
    }
    
    // ==================== PRIMARY KEY ====================
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    // ==================== FOREIGN KEYS ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private NoleggioLead lead;
    
    @Column(name = "ordine_id", nullable = false)
    private Long ordineId;  // FK a noleggio_ordini (ordine consegnato)
    
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;  // FK a users/crm_leads
    
    @Column(name = "utente_assegnato_id")
    private Long utenteAssegnatoId;  // FK a users (account manager)
    
    // ==================== CORE FIELDS ====================
    
    @Column(name = "numero_contratto", unique = true, length = 50)
    private String numeroContratto;  // Es: CNT-2025-00078
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contratto", nullable = false, length = 50)
    private TipoContratto tipoContratto;
    
    @Column(name = "data_inizio", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dataInizio;
    
    @Column(name = "data_fine", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dataFine;
    
    @Column(name = "durata_mesi", nullable = false)
    private Integer durataMesi;
    
    // ==================== DATI CLIENTE ====================
    
    @Column(name = "nome_cliente", nullable = false, length = 200)
    private String nomeCliente;
    
    @Column(name = "codice_fiscale", length = 50)
    private String codiceFiscale;
    
    @Column(name = "partita_iva", length = 50)
    private String partitaIva;
    
    @Column(name = "indirizzo_cliente", columnDefinition = "TEXT")
    private String indirizzoCliente;
    
    @Column(name = "telefono", length = 50)
    private String telefono;
    
    @Column(name = "email", length = 200)
    private String email;
    
    // ==================== DATI VEICOLO ====================
    
    @Column(name = "marca", nullable = false, length = 100)
    private String marca;
    
    @Column(name = "modello", nullable = false, length = 100)
    private String modello;
    
    @Column(name = "targa", nullable = false, unique = true, length = 20)
    private String targa;
    
    @Column(name = "vin", length = 50)
    private String vin;  // Vehicle Identification Number
    
    @Column(name = "colore", length = 100)
    private String colore;
    
    @Column(name = "cilindrata")
    private Integer cilindrata;
    
    @Column(name = "anno_immatricolazione")
    private Integer annoImmatricolazione;
    
    // ==================== CONDIZIONI ECONOMICHE ====================
    
    @Column(name = "canone_mensile", nullable = false, precision = 10, scale = 2)
    private BigDecimal canoneMensile;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "periodicita_pagamento", length = 50)
    private PeriodicoPagamento periodicitaPagamento;
    
    @Column(name = "anticipo_versato", precision = 10, scale = 2)
    private BigDecimal anticipoVersato;
    
    @Column(name = "deposito_cauzionale", precision = 10, scale = 2)
    private BigDecimal depositoCauzionale;
    
    @Column(name = "valore_riscatto", precision = 10, scale = 2)
    private BigDecimal valoreRiscatto;  // Se previsto riscatto finale
    
    // ==================== CONDIZIONI CHILOMETRICHE ====================
    
    @Column(name = "km_totali_previsti")
    private Integer kmTotaliPrevisti;  // Km totali contratto (es. 45000 per 36 mesi)
    
    @Column(name = "km_annuali")
    private Integer kmAnnuali;  // Km anno (es. 15000)
    
    @Column(name = "km_iniziali")
    private Integer kmIniziali;  // Km alla consegna
    
    @Column(name = "km_attuali")
    private Integer kmAttuali;  // Km attuali (aggiornati a ultimo check)
    
    @Column(name = "data_ultima_verifica_km")
    @Temporal(TemporalType.DATE)
    private Date dataUltimaVerificaKm;
    
    @Column(name = "prossima_verifica_km")
    @Temporal(TemporalType.DATE)
    private Date prossimaVerificaKm;  // Ogni 6 mesi
    
    @Column(name = "costo_km_extra", precision = 10, scale = 4)
    private BigDecimal costoKmExtra;  // Costo per ogni km oltre soglia
    
    // ==================== SCADENZARIO MANUTENZIONE ====================
    
    @Column(name = "data_prossima_revisione")
    @Temporal(TemporalType.DATE)
    private Date dataProssimaRevisione;
    
    @Column(name = "data_ultimo_tagliando")
    @Temporal(TemporalType.DATE)
    private Date dataUltimoTagliando;
    
    @Column(name = "data_prossimo_tagliando")
    @Temporal(TemporalType.DATE)
    private Date dataProssimoTagliando;
    
    @Column(name = "km_prossimo_tagliando")
    private Integer kmProssimoTagliando;
    
    // ==================== SCADENZARIO DOCUMENTI ====================
    
    @Column(name = "data_scadenza_assicurazione")
    @Temporal(TemporalType.DATE)
    private Date dataScadenzaAssicurazione;
    
    @Column(name = "numero_polizza", length = 100)
    private String numeroPolizza;
    
    @Column(name = "compagnia_assicurativa", length = 200)
    private String compagniaAssicurativa;
    
    @Column(name = "data_scadenza_patente_conducente")
    @Temporal(TemporalType.DATE)
    private Date dataScadenzaPatenteConducente;
    
    @Column(name = "numero_patente", length = 50)
    private String numeroPatente;
    
    // ==================== RINNOVO CONTRATTO ====================
    
    @Column(name = "allarme_rinnovo_attivato")
    private Boolean allarmeRinnovoAttivato;  // TRUE se alert rinnovo inviato
    
    @Column(name = "data_allarme_rinnovo")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataAllarmeRinnovo;
    
    @Column(name = "proposta_rinnovo_inviata")
    private Boolean propostaRinnovoInviata;
    
    @Column(name = "data_proposta_rinnovo")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataPropostaRinnovo;
    
    @Column(name = "nuovo_contratto_id")
    private Long nuovoContrattoId;  // FK a noleggio_contratti (se rinnovato)
    
    // ==================== DOCUMENTI ====================
    
    @Column(name = "file_contratto_path", length = 500)
    private String fileContrattoPath;  // PDF contratto firmato
    
    @Column(name = "file_libretto_path", length = 500)
    private String fileLibrettoPath;  // PDF libretto circolazione
    
    @Column(name = "allegati", columnDefinition = "TEXT")
    private String allegati;  // JSON array altri documenti
    
    // ==================== NOTE ====================
    
    @Column(name = "note_contratto", columnDefinition = "TEXT")
    private String noteContratto;
    
    @Column(name = "note_interne", columnDefinition = "TEXT")
    private String noteInterne;
    
    // ==================== TIMESTAMPS ====================
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    // ==================== LIFECYCLE CALLBACKS ====================
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
        if (status == null) {
            status = Status.ATTIVO;
        }
        if (allarmeRinnovoAttivato == null) {
            allarmeRinnovoAttivato = false;
        }
        if (propostaRinnovoInviata == null) {
            propostaRinnovoInviata = false;
        }
        
        // Genera numero contratto automatico
        if (numeroContratto == null) {
            numeroContratto = generaNumeroContratto();
        }
        
        // Calcola prima verifica km (6 mesi da inizio)
        if (prossimaVerificaKm == null && dataInizio != null) {
            prossimaVerificaKm = aggiungiMesi(dataInizio, 6);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Genera numero contratto univoco formato: CNT-YYYY-NNNNN
     */
    private String generaNumeroContratto() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        long timestamp = System.currentTimeMillis() % 100000;
        return String.format("CNT-%d-%05d", year, timestamp);
    }
    
    /**
     * Verifica se mancano 4-6 mesi alla scadenza (per alert rinnovo).
     * AUTOMAZIONE: Chiamato da NoleggioScadenzarioService per creare CalendarEvent.
     * 
     * @param giorniAnticipo Giorni anticipo (es. 120 = 4 mesi)
     * @return true se in scadenza
     */
    public boolean isRinnovoInScadenza(int giorniAnticipo) {
        if (status != Status.ATTIVO) {
            return false;
        }
        
        long giorniAScadenza = calcolaGiorniFino(dataFine);
        return giorniAScadenza <= giorniAnticipo && giorniAScadenza > 0;
    }
    
    /**
     * Verifica se richiede verifica chilometrica (ogni 6 mesi).
     * AUTOMAZIONE: Crea CalendarEvent NOLEGGIO_VERIFICA_KM.
     * 
     * @return true se richiede verifica km
     */
    public boolean richiedeVerificaKm() {
        if (prossimaVerificaKm == null) {
            return false;
        }
        
        long giorniAProssimaVerifica = calcolaGiorniFino(prossimaVerificaKm);
        return giorniAProssimaVerifica <= 7 && giorniAProssimaVerifica >= 0;
    }
    
    /**
     * Registra verifica chilometrica effettuata.
     * Pianifica prossima verifica fra 6 mesi.
     * 
     * @param kmRilevati Km attuali rilevati
     */
    public void registraVerificaKm(int kmRilevati) {
        this.kmAttuali = kmRilevati;
        this.dataUltimaVerificaKm = new Date();
        this.prossimaVerificaKm = aggiungiMesi(new Date(), 6);
    }
    
    /**
     * Calcola km mancanti prima di raggiungere soglia contratto.
     * 
     * @return km disponibili (negativo se superato)
     */
    public Integer calcolaKmDisponibili() {
        if (kmAttuali == null || kmTotaliPrevisti == null || kmIniziali == null) {
            return null;
        }
        
        int kmPercorsi = kmAttuali - kmIniziali;
        return kmTotaliPrevisti - kmPercorsi;
    }
    
    /**
     * Verifica se km attuali superano soglia contratto.
     * 
     * @return true se km superati
     */
    public boolean isKmSuperati() {
        Integer disponibili = calcolaKmDisponibili();
        return disponibili != null && disponibili < 0;
    }
    
    /**
     * Calcola costo extra per km oltre soglia.
     * 
     * @return importo extra da pagare
     */
    public BigDecimal calcolaCostoKmExtra() {
        if (!isKmSuperati() || costoKmExtra == null) {
            return BigDecimal.ZERO;
        }
        
        int kmExtra = Math.abs(calcolaKmDisponibili());
        return costoKmExtra.multiply(new BigDecimal(kmExtra));
    }
    
    /**
     * Attiva allarme rinnovo (4-6 mesi prima scadenza).
     */
    public void attivaAllarmeRinnovo() {
        this.allarmeRinnovoAttivato = true;
        this.dataAllarmeRinnovo = new Date();
        this.status = Status.IN_SCADENZA;
    }
    
    /**
     * Registra invio proposta rinnovo.
     */
    public void registraPropostaRinnovo() {
        this.propostaRinnovoInviata = true;
        this.dataPropostaRinnovo = new Date();
        this.status = Status.PROPOSTA_RINNOVO;
    }
    
    /**
     * Rinnova contratto.
     * 
     * @param nuovoContrattoId ID nuovo contratto generato
     */
    public void rinnova(Long nuovoContrattoId) {
        this.status = Status.RINNOVATO;
        this.nuovoContrattoId = nuovoContrattoId;
    }
    
    /**
     * Calcola giorni mancanti fino a una data.
     */
    private long calcolaGiorniFino(Date data) {
        if (data == null) return Long.MAX_VALUE;
        long diff = data.getTime() - new Date().getTime();
        return diff / (24 * 60 * 60 * 1000);
    }
    
    /**
     * Aggiunge mesi a una data.
     */
    private Date aggiungiMesi(Date data, int mesi) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(data);
        cal.add(java.util.Calendar.MONTH, mesi);
        return cal.getTime();
    }
    
    // ==================== GETTERS & SETTERS ====================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public NoleggioLead getLead() {
        return lead;
    }
    
    public void setLead(NoleggioLead lead) {
        this.lead = lead;
    }
    
    public Long getOrdineId() {
        return ordineId;
    }
    
    public void setOrdineId(Long ordineId) {
        this.ordineId = ordineId;
    }
    
    public Long getClienteId() {
        return clienteId;
    }
    
    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }
    
    public Long getUtenteAssegnatoId() {
        return utenteAssegnatoId;
    }
    
    public void setUtenteAssegnatoId(Long utenteAssegnatoId) {
        this.utenteAssegnatoId = utenteAssegnatoId;
    }
    
    public String getNumeroContratto() {
        return numeroContratto;
    }
    
    public void setNumeroContratto(String numeroContratto) {
        this.numeroContratto = numeroContratto;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public TipoContratto getTipoContratto() {
        return tipoContratto;
    }
    
    public void setTipoContratto(TipoContratto tipoContratto) {
        this.tipoContratto = tipoContratto;
    }
    
    public Date getDataInizio() {
        return dataInizio;
    }
    
    public void setDataInizio(Date dataInizio) {
        this.dataInizio = dataInizio;
    }
    
    public Date getDataFine() {
        return dataFine;
    }
    
    public void setDataFine(Date dataFine) {
        this.dataFine = dataFine;
    }
    
    public Integer getDurataMesi() {
        return durataMesi;
    }
    
    public void setDurataMesi(Integer durataMesi) {
        this.durataMesi = durataMesi;
    }
    
    public String getNomeCliente() {
        return nomeCliente;
    }
    
    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }
    
    public String getCodiceFiscale() {
        return codiceFiscale;
    }
    
    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }
    
    public String getPartitaIva() {
        return partitaIva;
    }
    
    public void setPartitaIva(String partitaIva) {
        this.partitaIva = partitaIva;
    }
    
    public String getIndirizzoCliente() {
        return indirizzoCliente;
    }
    
    public void setIndirizzoCliente(String indirizzoCliente) {
        this.indirizzoCliente = indirizzoCliente;
    }
    
    public String getTelefono() {
        return telefono;
    }
    
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getMarca() {
        return marca;
    }
    
    public void setMarca(String marca) {
        this.marca = marca;
    }
    
    public String getModello() {
        return modello;
    }
    
    public void setModello(String modello) {
        this.modello = modello;
    }
    
    public String getTarga() {
        return targa;
    }
    
    public void setTarga(String targa) {
        this.targa = targa;
    }
    
    public String getVin() {
        return vin;
    }
    
    public void setVin(String vin) {
        this.vin = vin;
    }
    
    public String getColore() {
        return colore;
    }
    
    public void setColore(String colore) {
        this.colore = colore;
    }
    
    public Integer getCilindrata() {
        return cilindrata;
    }
    
    public void setCilindrata(Integer cilindrata) {
        this.cilindrata = cilindrata;
    }
    
    public Integer getAnnoImmatricolazione() {
        return annoImmatricolazione;
    }
    
    public void setAnnoImmatricolazione(Integer annoImmatricolazione) {
        this.annoImmatricolazione = annoImmatricolazione;
    }
    
    public BigDecimal getCanoneMensile() {
        return canoneMensile;
    }
    
    public void setCanoneMensile(BigDecimal canoneMensile) {
        this.canoneMensile = canoneMensile;
    }
    
    public PeriodicoPagamento getPeriodicitaPagamento() {
        return periodicitaPagamento;
    }
    
    public void setPeriodicitaPagamento(PeriodicoPagamento periodicitaPagamento) {
        this.periodicitaPagamento = periodicitaPagamento;
    }
    
    public BigDecimal getAnticipoVersato() {
        return anticipoVersato;
    }
    
    public void setAnticipoVersato(BigDecimal anticipoVersato) {
        this.anticipoVersato = anticipoVersato;
    }
    
    public BigDecimal getDepositoCauzionale() {
        return depositoCauzionale;
    }
    
    public void setDepositoCauzionale(BigDecimal depositoCauzionale) {
        this.depositoCauzionale = depositoCauzionale;
    }
    
    public BigDecimal getValoreRiscatto() {
        return valoreRiscatto;
    }
    
    public void setValoreRiscatto(BigDecimal valoreRiscatto) {
        this.valoreRiscatto = valoreRiscatto;
    }
    
    public Integer getKmTotaliPrevisti() {
        return kmTotaliPrevisti;
    }
    
    public void setKmTotaliPrevisti(Integer kmTotaliPrevisti) {
        this.kmTotaliPrevisti = kmTotaliPrevisti;
    }
    
    public Integer getKmAnnuali() {
        return kmAnnuali;
    }
    
    public void setKmAnnuali(Integer kmAnnuali) {
        this.kmAnnuali = kmAnnuali;
    }
    
    public Integer getKmIniziali() {
        return kmIniziali;
    }
    
    public void setKmIniziali(Integer kmIniziali) {
        this.kmIniziali = kmIniziali;
    }
    
    public Integer getKmAttuali() {
        return kmAttuali;
    }
    
    public void setKmAttuali(Integer kmAttuali) {
        this.kmAttuali = kmAttuali;
    }
    
    public Date getDataUltimaVerificaKm() {
        return dataUltimaVerificaKm;
    }
    
    public void setDataUltimaVerificaKm(Date dataUltimaVerificaKm) {
        this.dataUltimaVerificaKm = dataUltimaVerificaKm;
    }
    
    public Date getProssimaVerificaKm() {
        return prossimaVerificaKm;
    }
    
    public void setProssimaVerificaKm(Date prossimaVerificaKm) {
        this.prossimaVerificaKm = prossimaVerificaKm;
    }
    
    public BigDecimal getCostoKmExtra() {
        return costoKmExtra;
    }
    
    public void setCostoKmExtra(BigDecimal costoKmExtra) {
        this.costoKmExtra = costoKmExtra;
    }
    
    public Date getDataProssimaRevisione() {
        return dataProssimaRevisione;
    }
    
    public void setDataProssimaRevisione(Date dataProssimaRevisione) {
        this.dataProssimaRevisione = dataProssimaRevisione;
    }
    
    public Date getDataUltimoTagliando() {
        return dataUltimoTagliando;
    }
    
    public void setDataUltimoTagliando(Date dataUltimoTagliando) {
        this.dataUltimoTagliando = dataUltimoTagliando;
    }
    
    public Date getDataProssimoTagliando() {
        return dataProssimoTagliando;
    }
    
    public void setDataProssimoTagliando(Date dataProssimoTagliando) {
        this.dataProssimoTagliando = dataProssimoTagliando;
    }
    
    public Integer getKmProssimoTagliando() {
        return kmProssimoTagliando;
    }
    
    public void setKmProssimoTagliando(Integer kmProssimoTagliando) {
        this.kmProssimoTagliando = kmProssimoTagliando;
    }
    
    public Date getDataScadenzaAssicurazione() {
        return dataScadenzaAssicurazione;
    }
    
    public void setDataScadenzaAssicurazione(Date dataScadenzaAssicurazione) {
        this.dataScadenzaAssicurazione = dataScadenzaAssicurazione;
    }
    
    public String getNumeroPolizza() {
        return numeroPolizza;
    }
    
    public void setNumeroPolizza(String numeroPolizza) {
        this.numeroPolizza = numeroPolizza;
    }
    
    public String getCompagniaAssicurativa() {
        return compagniaAssicurativa;
    }
    
    public void setCompagniaAssicurativa(String compagniaAssicurativa) {
        this.compagniaAssicurativa = compagniaAssicurativa;
    }
    
    public Date getDataScadenzaPatenteConducente() {
        return dataScadenzaPatenteConducente;
    }
    
    public void setDataScadenzaPatenteConducente(Date dataScadenzaPatenteConducente) {
        this.dataScadenzaPatenteConducente = dataScadenzaPatenteConducente;
    }
    
    public String getNumeroPatente() {
        return numeroPatente;
    }
    
    public void setNumeroPatente(String numeroPatente) {
        this.numeroPatente = numeroPatente;
    }
    
    public Boolean getAllarmeRinnovoAttivato() {
        return allarmeRinnovoAttivato;
    }
    
    public void setAllarmeRinnovoAttivato(Boolean allarmeRinnovoAttivato) {
        this.allarmeRinnovoAttivato = allarmeRinnovoAttivato;
    }
    
    public Date getDataAllarmeRinnovo() {
        return dataAllarmeRinnovo;
    }
    
    public void setDataAllarmeRinnovo(Date dataAllarmeRinnovo) {
        this.dataAllarmeRinnovo = dataAllarmeRinnovo;
    }
    
    public Boolean getPropostaRinnovoInviata() {
        return propostaRinnovoInviata;
    }
    
    public void setPropostaRinnovoInviata(Boolean propostaRinnovoInviata) {
        this.propostaRinnovoInviata = propostaRinnovoInviata;
    }
    
    public Date getDataPropostaRinnovo() {
        return dataPropostaRinnovo;
    }
    
    public void setDataPropostaRinnovo(Date dataPropostaRinnovo) {
        this.dataPropostaRinnovo = dataPropostaRinnovo;
    }
    
    public Long getNuovoContrattoId() {
        return nuovoContrattoId;
    }
    
    public void setNuovoContrattoId(Long nuovoContrattoId) {
        this.nuovoContrattoId = nuovoContrattoId;
    }
    
    public String getFileContrattoPath() {
        return fileContrattoPath;
    }
    
    public void setFileContrattoPath(String fileContrattoPath) {
        this.fileContrattoPath = fileContrattoPath;
    }
    
    public String getFileLibrettoPath() {
        return fileLibrettoPath;
    }
    
    public void setFileLibrettoPath(String fileLibrettoPath) {
        this.fileLibrettoPath = fileLibrettoPath;
    }
    
    public String getAllegati() {
        return allegati;
    }
    
    public void setAllegati(String allegati) {
        this.allegati = allegati;
    }
    
    public String getNoteContratto() {
        return noteContratto;
    }
    
    public void setNoteContratto(String noteContratto) {
        this.noteContratto = noteContratto;
    }
    
    public String getNoteInterne() {
        return noteInterne;
    }
    
    public void setNoteInterne(String noteInterne) {
        this.noteInterne = noteInterne;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
