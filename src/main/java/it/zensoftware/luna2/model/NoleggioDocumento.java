package it.zensoftware.luna2.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Entity NoleggioDocumento - Gestione documenti istruttoria noleggio
 * Checklist documenti reddituali con tracking stato
 * 
 * @author Luna2 Team
 * @version 1.0
 */
@Entity
@Table(name = "noleggio_documenti")
public class NoleggioDocumento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "noleggio_lead_id", nullable = false)
    private Long noleggioLeadId;

    /**
     * Tipo Documento
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 30)
    private TipoDocumento tipoDocumento;

    @Column(name = "descrizione", length = 500)
    private String descrizione;

    @Column(name = "obbligatorio", nullable = false)
    private Boolean obbligatorio = true;

    /**
     * File information
     */
    @Column(name = "nome_file", length = 255)
    private String nomeFile;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * Status tracking
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "note_validazione", columnDefinition = "TEXT")
    private String noteValidazione;

    /**
     * Dates
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_richiesta")
    private Date dataRichiesta;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_ricezione")
    private Date dataRicezione;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "data_validazione")
    private Date dataValidazione;

    /**
     * Scadenza documento (per patente, CI, etc.)
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "data_scadenza_documento")
    private Date dataScadenzaDocumento;

    // =========================================================================
    // ENUMS
    // =========================================================================

    public enum TipoDocumento {
        DOCUMENTO_IDENTITA("Documento di Identità"),
        PATENTE_GUIDA("Patente di Guida"),
        CODICE_FISCALE("Codice Fiscale"),
        BUSTA_PAGA("Busta Paga (ultime 3)"),
        CU("Certificazione Unica (CU)"),
        UNICO("Dichiarazione dei Redditi (Unico)"),
        BILANCIO_AZIENDALE("Bilancio Aziendale"),
        VISURA_CAMERALE("Visura Camerale"),
        ALTRO("Altro Documento");

        private final String descrizione;

        TipoDocumento(String descrizione) {
            this.descrizione = descrizione;
        }

        public String getDescrizione() {
            return descrizione;
        }
    }

    public enum Status {
        DA_RICHIEDERE,   // Documento da richiedere al cliente
        RICHIESTO,       // Richiesta inviata (trigger: reminder dopo X giorni)
        RICEVUTO,        // Documento ricevuto, da validare
        VALIDATO,        // Documento validato e OK
        RIFIUTATO        // Documento non valido (da richiedere di nuovo)
    }

    // =========================================================================
    // BUSINESS METHODS
    // =========================================================================

    /**
     * Verifica se il documento è in scadenza (entro 30 giorni)
     */
    public boolean isInScadenza() {
        if (dataScadenzaDocumento == null) {
            return false;
        }
        
        long millisecondi30Giorni = 30L * 24 * 60 * 60 * 1000;
        Date tra30Giorni = new Date(System.currentTimeMillis() + millisecondi30Giorni);
        
        return dataScadenzaDocumento.before(tra30Giorni);
    }

    /**
     * Verifica se il documento è scaduto
     */
    public boolean isScaduto() {
        if (dataScadenzaDocumento == null) {
            return false;
        }
        return dataScadenzaDocumento.before(new Date());
    }

    /**
     * Calcola i giorni dall'ultima richiesta
     */
    public long giorniDaRichiesta() {
        if (dataRichiesta == null) {
            return 0;
        }
        
        long diffMillis = new Date().getTime() - dataRichiesta.getTime();
        return diffMillis / (24 * 60 * 60 * 1000);
    }

    /**
     * Verifica se serve reminder (> X giorni dalla richiesta senza ricezione)
     */
    public boolean richiedeReminder(int giorniSoglia) {
        return status == Status.RICHIESTO && 
               dataRicezione == null && 
               giorniDaRichiesta() >= giorniSoglia;
    }

    /**
     * Marca come richiesto
     */
    public void marcaRichiesto() {
        this.status = Status.RICHIESTO;
        this.dataRichiesta = new Date();
    }

    /**
     * Marca come ricevuto
     */
    public void marcaRicevuto() {
        this.status = Status.RICEVUTO;
        this.dataRicezione = new Date();
    }

    /**
     * Valida il documento
     */
    public void valida(String note) {
        this.status = Status.VALIDATO;
        this.dataValidazione = new Date();
        this.noteValidazione = note;
    }

    /**
     * Rifiuta il documento
     */
    public void rifiuta(String motivo) {
        this.status = Status.RIFIUTATO;
        this.dataValidazione = new Date();
        this.noteValidazione = motivo;
    }

    /**
     * Verifica se il file è caricato
     */
    public boolean hasFile() {
        return filePath != null && !filePath.isEmpty();
    }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoleggioLeadId() {
        return noleggioLeadId;
    }

    public void setNoleggioLeadId(Long noleggioLeadId) {
        this.noleggioLeadId = noleggioLeadId;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Boolean getObbligatorio() {
        return obbligatorio;
    }

    public void setObbligatorio(Boolean obbligatorio) {
        this.obbligatorio = obbligatorio;
    }

    public String getNomeFile() {
        return nomeFile;
    }

    public void setNomeFile(String nomeFile) {
        this.nomeFile = nomeFile;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getNoteValidazione() {
        return noteValidazione;
    }

    public void setNoteValidazione(String noteValidazione) {
        this.noteValidazione = noteValidazione;
    }

    public Date getDataRichiesta() {
        return dataRichiesta;
    }

    public void setDataRichiesta(Date dataRichiesta) {
        this.dataRichiesta = dataRichiesta;
    }

    public Date getDataRicezione() {
        return dataRicezione;
    }

    public void setDataRicezione(Date dataRicezione) {
        this.dataRicezione = dataRicezione;
    }

    public Date getDataValidazione() {
        return dataValidazione;
    }

    public void setDataValidazione(Date dataValidazione) {
        this.dataValidazione = dataValidazione;
    }

    public Date getDataScadenzaDocumento() {
        return dataScadenzaDocumento;
    }

    public void setDataScadenzaDocumento(Date dataScadenzaDocumento) {
        this.dataScadenzaDocumento = dataScadenzaDocumento;
    }

    @Override
    public String toString() {
        return "NoleggioDocumento{" +
                "id=" + id +
                ", tipoDocumento=" + tipoDocumento +
                ", obbligatorio=" + obbligatorio +
                ", status=" + status +
                ", nomeFile='" + nomeFile + '\'' +
                '}';
    }
}
