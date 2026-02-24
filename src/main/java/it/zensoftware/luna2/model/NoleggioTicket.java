package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * NoleggioTicket - Sistema ticketing post-vendita (Fase 4)
 * 
 * Gestisce le richieste di assistenza post-vendita del cliente con sistema
 * di anti-rimbalzo: se il ticket viene riaperto entro 48-72h dalla chiusura,
 * viene generato alert per escalation manageriale.
 * 
 * LOGICA AUTOMAZIONE (Fase 4):
 * - Alert anti-rimbalzo se riaperto entro 48-72h
 * - Push notification operatore per nuovi ticket
 * - SLA tracking: avviso se ticket aperto oltre X giorni
 * - Notifica cliente automatica per cambio status
 * 
 * DB: noleggio_tickets
 * 
 * @author Luna2 CRM - Modulo Broker Noleggio
 */
@Entity
@Table(name = "noleggio_tickets")
public class NoleggioTicket implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ==================== ENUMERATIONS ====================
    
    /**
     * Status - Stati del ticket
     */
    public enum Status {
        APERTO,                 // Appena aperto, in attesa assegnazione
        ASSEGNATO,              // Assegnato a operatore
        IN_LAVORAZIONE,         // Operatore sta lavorando su soluzione
        IN_ATTESA_CLIENTE,      // In attesa risposta/info dal cliente
        RISOLTO,                // Risolto, in attesa conferma cliente
        CHIUSO,                 // Chiuso definitivamente
        RIAPERTO,               // Riaperto dopo chiusura (ANTI-RIMBALZO!)
        ESCALATO                // Escalato a manager/supporto avanzato
    }
    
    /**
     * Priorita - Livello di urgenza
     */
    public enum Priorita {
        BASSA,                  // Richiesta informazioni generiche
        MEDIA,                  // Problema non bloccante
        ALTA,                   // Problema che impatta uso veicolo
        CRITICA                 // Veicolo inutilizzabile o danno grave
    }
    
    /**
     * Categoria - Tipo di problema
     */
    public enum Categoria {
        GUASTO_VEICOLO,         // Guasto tecnico auto
        DANNO_CARROZZERIA,      // Danno estetico
        SINISTRO,               // Incidente stradale
        MANUTENZIONE,           // Richiesta manutenzione
        DOCUMENTI,              // Problemi documentali
        FATTURAZIONE,           // Problemi fatture/pagamenti
        CAMBIO_VEICOLO,         // Richiesta cambio veicolo
        RECLAMO_SERVIZIO,       // Reclamo servizio broker
        INFORMAZIONI,           // Richiesta informazioni
        ALTRO                   // Altro
    }
    
    /**
     * Canale - Come è arrivata la segnalazione
     */
    public enum Canale {
        TELEFONO,               // Telefonata
        EMAIL,                  // Email cliente
        AREA_RISERVATA,         // Ticket creato da area riservata
        WHATSAPP,               // WhatsApp Business
        CHATBOT,                // Chat online
        SPORTELLO               // Presentato di persona
    }
    
    // ==================== PRIMARY KEY ====================
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    // ==================== FOREIGN KEYS ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratto_id", nullable = false)
    private NoleggioContratto contratto;
    
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;  // FK a users/crm_leads
    
    @Column(name = "operatore_assegnato_id")
    private Long operatoreAssegnatoId;  // FK a users (operatore che gestisce)
    
    @Column(name = "manager_escalation_id")
    private Long managerEscalationId;  // FK a users (manager in caso escalation)
    
    // ==================== CORE FIELDS ====================
    
    @Column(name = "numero_ticket", unique = true, length = 50)
    private String numeroTicket;  // Es: TKT-2025-00123
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priorita", nullable = false, length = 20)
    private Priorita priorita;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 50)
    private Categoria categoria;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "canale", length = 50)
    private Canale canale;
    
    @Column(name = "oggetto", nullable = false, length = 200)
    private String oggetto;
    
    @Column(name = "descrizione", nullable = false, columnDefinition = "TEXT")
    private String descrizione;
    
    // ==================== TRACKING TEMPORALE ====================
    
    @Column(name = "data_apertura", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataApertura;
    
    @Column(name = "data_assegnazione")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataAssegnazione;
    
    @Column(name = "data_presa_in_carico")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataPresaInCarico;
    
    @Column(name = "data_risoluzione")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataRisoluzione;
    
    @Column(name = "data_chiusura")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataChiusura;
    
    @Column(name = "data_riapertura")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataRiapertura;
    
    // ==================== ANTI-RIMBALZO SYSTEM ====================
    
    @Column(name = "numero_riaperture")
    private Integer numeroRiaperture;
    
    @Column(name = "flag_anti_rimbalzo")
    private Boolean flagAntiRimbalzo;  // TRUE se riaperto entro 48-72h
    
    @Column(name = "ore_tra_chiusura_e_riapertura")
    private Integer oreTraChiusuraERiapertura;
    
    @Column(name = "motivazione_riapertura", columnDefinition = "TEXT")
    private String motivazioneRiapertura;
    
    // ==================== SLA (Service Level Agreement) ====================
    
    @Column(name = "sla_risposta_ore")
    private Integer slaRispostaOre;  // Ore entro cui rispondere (es. 4h per CRITICA)
    
    @Column(name = "sla_risoluzione_ore")
    private Integer slaRisoluzioneOre;  // Ore entro cui risolvere (es. 24h per CRITICA)
    
    @Column(name = "sla_risposta_scaduto")
    private Boolean slaRispostaScaduto;
    
    @Column(name = "sla_risoluzione_scaduto")
    private Boolean slaRisoluzioneScaduto;
    
    // ==================== DATI VEICOLO ====================
    
    @Column(name = "targa_veicolo", length = 20)
    private String targaVeicolo;
    
    @Column(name = "km_attuali")
    private Integer kmAttuali;
    
    @Column(name = "luogo_veicolo", length = 200)
    private String luogoVeicolo;  // Dove si trova il veicolo
    
    // ==================== ALLEGATI & NOTE ====================
    
    @Column(name = "allegati", columnDefinition = "TEXT")
    private String allegati;  // JSON array con file paths/URLs
    
    @Column(name = "soluzione_applicata", columnDefinition = "TEXT")
    private String soluzioneApplicata;
    
    @Column(name = "note_interne", columnDefinition = "TEXT")
    private String noteInterne;
    
    @Column(name = "valutazione_cliente")
    private Integer valutazioneCliente;  // 1-5 stelle dopo chiusura
    
    @Column(name = "feedback_cliente", columnDefinition = "TEXT")
    private String feedbackCliente;
    
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
            status = Status.APERTO;
        }
        if (dataApertura == null) {
            dataApertura = new Date();
        }
        if (numeroRiaperture == null) {
            numeroRiaperture = 0;
        }
        if (flagAntiRimbalzo == null) {
            flagAntiRimbalzo = false;
        }
        if (slaRispostaScaduto == null) {
            slaRispostaScaduto = false;
        }
        if (slaRisoluzioneScaduto == null) {
            slaRisoluzioneScaduto = false;
        }
        
        // Genera numero ticket automatico
        if (numeroTicket == null) {
            numeroTicket = generaNumeroTicket();
        }
        
        // Imposta SLA in base a priorità
        impostaSLA();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
    
    // ==================== BUSINESS METHODS ====================
    
    /**
     * Genera numero ticket univoco formato: TKT-YYYY-NNNNN
     */
    private String generaNumeroTicket() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        long timestamp = System.currentTimeMillis() % 100000;
        return String.format("TKT-%d-%05d", year, timestamp);
    }
    
    /**
     * Imposta SLA (tempi di risposta/risoluzione) in base a priorità.
     */
    private void impostaSLA() {
        switch (priorita) {
            case CRITICA:
                slaRispostaOre = 2;   // Risposta entro 2 ore
                slaRisoluzioneOre = 24;  // Risoluzione entro 24 ore
                break;
            case ALTA:
                slaRispostaOre = 4;   // Risposta entro 4 ore
                slaRisoluzioneOre = 48;  // Risoluzione entro 48 ore
                break;
            case MEDIA:
                slaRispostaOre = 8;   // Risposta entro 8 ore
                slaRisoluzioneOre = 72;  // Risoluzione entro 72 ore
                break;
            case BASSA:
                slaRispostaOre = 24;  // Risposta entro 24 ore
                slaRisoluzioneOre = 120; // Risoluzione entro 5 giorni
                break;
        }
    }
    
    /**
     * Assegna ticket a operatore.
     */
    public void assegna(Long operatoreId) {
        this.operatoreAssegnatoId = operatoreId;
        this.status = Status.ASSEGNATO;
        this.dataAssegnazione = new Date();
    }
    
    /**
     * Operatore prende in carico il ticket.
     */
    public void prendiInCarico() {
        this.status = Status.IN_LAVORAZIONE;
        this.dataPresaInCarico = new Date();
    }
    
    /**
     * Marca ticket come risolto.
     */
    public void risolvi(String soluzione) {
        this.status = Status.RISOLTO;
        this.dataRisoluzione = new Date();
        this.soluzioneApplicata = soluzione;
    }
    
    /**
     * Chiudi definitivamente il ticket.
     */
    public void chiudi() {
        this.status = Status.CHIUSO;
        this.dataChiusura = new Date();
    }
    
    /**
     * Riapri ticket (CONTROLLA ANTI-RIMBALZO).
     * 
     * @param motivazione Motivazione riapertura
     * @return true se è stato attivato flag anti-rimbalzo
     */
    public boolean riapri(String motivazione) {
        this.numeroRiaperture++;
        this.dataRiapertura = new Date();
        this.motivazioneRiapertura = motivazione;
        this.status = Status.RIAPERTO;
        
        // VERIFICA ANTI-RIMBALZO: Se riaperto entro 48-72 ore dalla chiusura
        if (dataChiusura != null) {
            long oreTraChiusuraERiapertura = calcolaOreTra(dataChiusura, dataRiapertura);
            this.oreTraChiusuraERiapertura = (int) oreTraChiusuraERiapertura;
            
            // Anti-rimbalzo: tra 48 e 72 ore
            if (oreTraChiusuraERiapertura >= 48 && oreTraChiusuraERiapertura <= 72) {
                this.flagAntiRimbalzo = true;
                this.status = Status.ESCALATO;  // Auto-escalation
                return true;  // ATTIVA ALERT
            }
        }
        
        return false;
    }
    
    /**
     * Scala ticket a manager.
     */
    public void escala(Long managerId) {
        this.status = Status.ESCALATO;
        this.managerEscalationId = managerId;
    }
    
    /**
     * Verifica se SLA risposta è scaduto.
     * AUTOMAZIONE: Chiamato da NoleggioAutomationService ogni ora.
     * 
     * @return true se SLA risposta superato
     */
    public boolean isSlaRispostaScaduto() {
        if (slaRispostaScaduto || status == Status.CHIUSO) {
            return false;
        }
        
        long oreApertura = calcolaOreTrascorseDa(dataApertura);
        return oreApertura > slaRispostaOre;
    }
    
    /**
     * Verifica se SLA risoluzione è scaduto.
     * AUTOMAZIONE: Chiamato da NoleggioAutomationService ogni 4 ore.
     * 
     * @return true se SLA risoluzione superato
     */
    public boolean isSlaRisoluzioneScaduto() {
        if (slaRisoluzioneScaduto || status == Status.CHIUSO) {
            return false;
        }
        
        long oreApertura = calcolaOreTrascorseDa(dataApertura);
        return oreApertura > slaRisoluzioneOre;
    }
    
    /**
     * Calcola ore trascorse da una data.
     */
    private long calcolaOreTrascorseDa(Date data) {
        if (data == null) return 0;
        long diff = new Date().getTime() - data.getTime();
        return diff / (60 * 60 * 1000);  // ms to hours
    }
    
    /**
     * Calcola ore tra due date.
     */
    private long calcolaOreTra(Date data1, Date data2) {
        if (data1 == null || data2 == null) return 0;
        long diff = data2.getTime() - data1.getTime();
        return diff / (60 * 60 * 1000);  // ms to hours
    }
    
    /**
     * Calcola tempo di risoluzione effettivo in ore.
     */
    public Long calcolaTempoRisoluzione() {
        if (dataRisoluzione == null) return null;
        return calcolaOreTra(dataApertura, dataRisoluzione);
    }
    
    // ==================== GETTERS & SETTERS ====================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public NoleggioContratto getContratto() {
        return contratto;
    }
    
    public void setContratto(NoleggioContratto contratto) {
        this.contratto = contratto;
    }
    
    public Long getClienteId() {
        return clienteId;
    }
    
    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }
    
    public Long getOperatoreAssegnatoId() {
        return operatoreAssegnatoId;
    }
    
    public void setOperatoreAssegnatoId(Long operatoreAssegnatoId) {
        this.operatoreAssegnatoId = operatoreAssegnatoId;
    }
    
    public Long getManagerEscalationId() {
        return managerEscalationId;
    }
    
    public void setManagerEscalationId(Long managerEscalationId) {
        this.managerEscalationId = managerEscalationId;
    }
    
    public String getNumeroTicket() {
        return numeroTicket;
    }
    
    public void setNumeroTicket(String numeroTicket) {
        this.numeroTicket = numeroTicket;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public Priorita getPriorita() {
        return priorita;
    }
    
    public void setPriorita(Priorita priorita) {
        this.priorita = priorita;
    }
    
    public Categoria getCategoria() {
        return categoria;
    }
    
    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
    
    public Canale getCanale() {
        return canale;
    }
    
    public void setCanale(Canale canale) {
        this.canale = canale;
    }
    
    public String getOggetto() {
        return oggetto;
    }
    
    public void setOggetto(String oggetto) {
        this.oggetto = oggetto;
    }
    
    public String getDescrizione() {
        return descrizione;
    }
    
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
    
    public Date getDataApertura() {
        return dataApertura;
    }
    
    public void setDataApertura(Date dataApertura) {
        this.dataApertura = dataApertura;
    }
    
    public Date getDataAssegnazione() {
        return dataAssegnazione;
    }
    
    public void setDataAssegnazione(Date dataAssegnazione) {
        this.dataAssegnazione = dataAssegnazione;
    }
    
    public Date getDataPresaInCarico() {
        return dataPresaInCarico;
    }
    
    public void setDataPresaInCarico(Date dataPresaInCarico) {
        this.dataPresaInCarico = dataPresaInCarico;
    }
    
    public Date getDataRisoluzione() {
        return dataRisoluzione;
    }
    
    public void setDataRisoluzione(Date dataRisoluzione) {
        this.dataRisoluzione = dataRisoluzione;
    }
    
    public Date getDataChiusura() {
        return dataChiusura;
    }
    
    public void setDataChiusura(Date dataChiusura) {
        this.dataChiusura = dataChiusura;
    }
    
    public Date getDataRiapertura() {
        return dataRiapertura;
    }
    
    public void setDataRiapertura(Date dataRiapertura) {
        this.dataRiapertura = dataRiapertura;
    }
    
    public Integer getNumeroRiaperture() {
        return numeroRiaperture;
    }
    
    public void setNumeroRiaperture(Integer numeroRiaperture) {
        this.numeroRiaperture = numeroRiaperture;
    }
    
    public Boolean getFlagAntiRimbalzo() {
        return flagAntiRimbalzo;
    }
    
    public void setFlagAntiRimbalzo(Boolean flagAntiRimbalzo) {
        this.flagAntiRimbalzo = flagAntiRimbalzo;
    }
    
    public Integer getOreTraChiusuraERiapertura() {
        return oreTraChiusuraERiapertura;
    }
    
    public void setOreTraChiusuraERiapertura(Integer oreTraChiusuraERiapertura) {
        this.oreTraChiusuraERiapertura = oreTraChiusuraERiapertura;
    }
    
    public String getMotivazioneRiapertura() {
        return motivazioneRiapertura;
    }
    
    public void setMotivazioneRiapertura(String motivazioneRiapertura) {
        this.motivazioneRiapertura = motivazioneRiapertura;
    }
    
    public Integer getSlaRispostaOre() {
        return slaRispostaOre;
    }
    
    public void setSlaRispostaOre(Integer slaRispostaOre) {
        this.slaRispostaOre = slaRispostaOre;
    }
    
    public Integer getSlaRisoluzioneOre() {
        return slaRisoluzioneOre;
    }
    
    public void setSlaRisoluzioneOre(Integer slaRisoluzioneOre) {
        this.slaRisoluzioneOre = slaRisoluzioneOre;
    }
    
    public Boolean getSlaRispostaScaduto() {
        return slaRispostaScaduto;
    }
    
    public void setSlaRispostaScaduto(Boolean slaRispostaScaduto) {
        this.slaRispostaScaduto = slaRispostaScaduto;
    }
    
    public Boolean getSlaRisoluzioneScaduto() {
        return slaRisoluzioneScaduto;
    }
    
    public void setSlaRisoluzioneScaduto(Boolean slaRisoluzioneScaduto) {
        this.slaRisoluzioneScaduto = slaRisoluzioneScaduto;
    }
    
    public String getTargaVeicolo() {
        return targaVeicolo;
    }
    
    public void setTargaVeicolo(String targaVeicolo) {
        this.targaVeicolo = targaVeicolo;
    }
    
    public Integer getKmAttuali() {
        return kmAttuali;
    }
    
    public void setKmAttuali(Integer kmAttuali) {
        this.kmAttuali = kmAttuali;
    }
    
    public String getLuogoVeicolo() {
        return luogoVeicolo;
    }
    
    public void setLuogoVeicolo(String luogoVeicolo) {
        this.luogoVeicolo = luogoVeicolo;
    }
    
    public String getAllegati() {
        return allegati;
    }
    
    public void setAllegati(String allegati) {
        this.allegati = allegati;
    }
    
    public String getSoluzioneApplicata() {
        return soluzioneApplicata;
    }
    
    public void setSoluzioneApplicata(String soluzioneApplicata) {
        this.soluzioneApplicata = soluzioneApplicata;
    }
    
    public String getNoteInterne() {
        return noteInterne;
    }
    
    public void setNoteInterne(String noteInterne) {
        this.noteInterne = noteInterne;
    }
    
    public Integer getValutazioneCliente() {
        return valutazioneCliente;
    }
    
    public void setValutazioneCliente(Integer valutazioneCliente) {
        this.valutazioneCliente = valutazioneCliente;
    }
    
    public String getFeedbackCliente() {
        return feedbackCliente;
    }
    
    public void setFeedbackCliente(String feedbackCliente) {
        this.feedbackCliente = feedbackCliente;
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
