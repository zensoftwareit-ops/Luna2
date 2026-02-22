package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.CommessaDAO;
import it.zensoftware.luna2.dao.PreventivoDAO;
import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.model.Commessa;
import it.zensoftware.luna2.model.Commessa.StatoCommessa;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.Preventivo.Stato;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.FatturaRiga;
import it.zensoftware.luna2.model.ComMessaRiga;
import it.zensoftware.luna2.model.PreventivoRiga;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Service per la gestione delle commesse
 * Gestisce il workflow: Preventivo → Commessa → Fattura
 */
public class CommesseService {
    
    private static final Logger logger = LogManager.getLogger(CommesseService.class);
    
    private final CommessaDAO commessaDAO;
    private final PreventivoDAO preventivoDAO;
    private final FatturaDAO fatturaDAO;
    
    public CommesseService() {
        this.commessaDAO = new CommessaDAO();
        this.preventivoDAO = new PreventivoDAO();
        this.fatturaDAO = new FatturaDAO();
    }

    /**
     * Converte un preventivo in commessa
     */
    public Commessa creaCommessaDaPreventivo(Long preventivoId) throws Exception {
        logger.info("Creating commessa from preventivo: {}", preventivoId);
        
        // Carica preventivo
        Preventivo preventivo = preventivoDAO.findById(preventivoId);
        if (preventivo == null) {
            throw new IllegalArgumentException("Preventivo non trovato");
        }
        
        // Verifica che il preventivo sia ACCETTATO
        if (preventivo.getStato() != Stato.ACCETTATO) {
            throw new IllegalStateException("Il preventivo deve essere ACCETTATO per creare una commessa");
        }
        
        // Verifica che non esista già una commessa per questo preventivo
        Commessa esistente = commessaDAO.findByPreventivo(preventivoId);
        if (esistente != null) {
            throw new IllegalStateException("Esiste già una commessa per questo preventivo: " + esistente.getNumero());
        }
        
        // Crea commessa
        Calendar cal = Calendar.getInstance();
        int anno = cal.get(Calendar.YEAR);
        
        Commessa commessa = new Commessa();
        commessa.setNumero(commessaDAO.generaNuovoNumero(anno));
        commessa.setAnno(anno);
        commessa.setPreventivo(preventivo);
        commessa.setDescrizione(preventivo.getOggetto());
        commessa.setDataApertura(new Date());
        commessa.setStato(StatoCommessa.APERTA);
        
        // Copia importi dal preventivo
        commessa.setImponibile(preventivo.getImponibile());
        commessa.setIva(preventivo.getIva());
        commessa.setTotale(preventivo.getTotale());
        
        // Data prevista fine (es. 30 giorni dall'apertura)
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, 30);
        commessa.setDataPrevistFine(cal.getTime());
        
        // Salva commessa
        commessa = commessaDAO.save(commessa);
        
        // Copia righe dal preventivo
        if (preventivo.getRighe() != null && !preventivo.getRighe().isEmpty()) {
            List<ComMessaRiga> righeCommessa = new ArrayList<>();
            for (PreventivoRiga rigaPrev : preventivo.getRighe()) {
                ComMessaRiga riga = new ComMessaRiga();
                riga.setCommessa(commessa);
                riga.setNumeroRiga(rigaPrev.getRigaNumero());
                riga.setDescrizione(rigaPrev.getDescrizione());
                riga.setQuantita(rigaPrev.getQuantita());
                riga.setImportoUnitario(rigaPrev.getPrezzoUnitario());
                // ImportoTotale viene calcolato automaticamente in setQuantita/setImportoUnitario
                righeCommessa.add(riga);
            }
            commessa.setRighe(righeCommessa);
            commessa = commessaDAO.save(commessa);
        }
        
        // Aggiorna stato preventivo
        preventivo.setStato(Stato.CONVERTITO);
        preventivoDAO.save(preventivo);
        
        logger.info("Commessa created: {}", commessa.getNumero());
        return commessa;
    }

    /**
     * Converte una commessa in fattura (al completamento)
     */
    public Fattura creaFatturaDaCommessa(Long commessaId) throws Exception {
        logger.info("Creating fattura from commessa: {}", commessaId);
        
        // Carica commessa
        Commessa commessa = commessaDAO.findWithRighe(commessaId);
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non trovata");
        }
        
        // Verifica che la commessa sia COMPLETATA
        if (commessa.getStato() != StatoCommessa.COMPLETATA) {
            throw new IllegalStateException("La commessa deve essere COMPLETATA per creare una fattura");
        }
        
        // Verifica che non esista già una fattura
        if (commessa.getFattura() != null) {
            throw new IllegalStateException("Esiste già una fattura per questa commessa: " + 
                                          commessa.getFattura().getNumero());
        }
        
        // Crea fattura
        Calendar cal = Calendar.getInstance();
        int anno = cal.get(Calendar.YEAR);
        
        Fattura fattura = new Fattura();
        fattura.setNumero(fatturaDAO.generaNuovoNumero(anno));
        fattura.setAnno(anno);
        fattura.setDataFattura(new Date());
        fattura.setDataScadenza(getDataScadenza(30)); // 30 giorni
        fattura.setCliente(commessa.getPreventivo().getCliente());
        
        // Copia importi dalla commessa
        fattura.setImponibile(commessa.getImponibile());
        fattura.setIva(commessa.getIva());
        fattura.setTotale(commessa.getTotale());
        fattura.setPagato(BigDecimal.ZERO);
        
        fattura.setStato(Fattura.StatoFattura.EMESSA);
        fattura.setNote("Fattura generata da commessa " + commessa.getNumero());
        
        // Salva fattura
        fattura = fatturaDAO.save(fattura);
        
        // Copia righe dalla commessa
        if (commessa.getRighe() != null && !commessa.getRighe().isEmpty()) {
            List<FatturaRiga> righeFattura = new ArrayList<>();
            for (ComMessaRiga rigaCommessa : commessa.getRighe()) {
                FatturaRiga riga = new FatturaRiga();
                riga.setFattura(fattura);
                riga.setRigaNumero(rigaCommessa.getNumeroRiga());
                riga.setDescrizione(rigaCommessa.getDescrizione());
                riga.setQuantita(rigaCommessa.getQuantita());
                riga.setPrezzoUnitario(rigaCommessa.getImportoUnitario());
                riga.setImponibileRiga(rigaCommessa.getImportoTotale());
                riga.setIvaPercentuale(new BigDecimal("22.00")); // Default IVA 22%
                riga.setTotaleRiga(rigaCommessa.getImportoTotale().multiply(new BigDecimal("1.22")));
                righeFattura.add(riga);
            }
            fattura.setRighe(righeFattura);
            fattura = fatturaDAO.save(fattura);
        }
        
        // Collega fattura a commessa e chiudi commessa
        commessa.setFattura(fattura);
        commessa.setStato(StatoCommessa.CHIUSA);
        commessa.setDataChiusura(new Date());
        commessaDAO.save(commessa);
        
        logger.info("Fattura created: {}", fattura.getNumero());
        return fattura;
    }

    /**
     * Avvia lavorazione commessa
     */
    public Commessa avviaLavorazione(Long commessaId) throws Exception {
        Commessa commessa = commessaDAO.findById(commessaId);
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non trovata");
        }
        
        if (commessa.getStato() != StatoCommessa.APERTA) {
            throw new IllegalStateException("Solo le commesse APERTE possono essere avviate");
        }
        
        commessa.setStato(StatoCommessa.IN_LAVORAZIONE);
        commessa = commessaDAO.save(commessa);
        
        logger.info("Commessa {} avviata", commessa.getNumero());
        return commessa;
    }

    /**
     * Sospendi commessa
     */
    public Commessa sospendiCommessa(Long commessaId, String motivazione) throws Exception {
        Commessa commessa = commessaDAO.findById(commessaId);
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non trovata");
        }
        
        if (commessa.getStato() != StatoCommessa.IN_LAVORAZIONE) {
            throw new IllegalStateException("Solo le commesse IN_LAVORAZIONE possono essere sospese");
        }
        
        commessa.setStato(StatoCommessa.SOSPESA);
        commessa.setNote((commessa.getNote() != null ? commessa.getNote() + "\n" : "") + 
                        "SOSPESA: " + motivazione);
        commessa = commessaDAO.save(commessa);
        
        logger.info("Commessa {} sospesa", commessa.getNumero());
        return commessa;
    }

    /**
     * Riprendi commessa sospesa
     */
    public Commessa riprendiCommessa(Long commessaId) throws Exception {
        Commessa commessa = commessaDAO.findById(commessaId);
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non trovata");
        }
        
        if (commessa.getStato() != StatoCommessa.SOSPESA) {
            throw new IllegalStateException("Solo le commesse SOSPESE possono essere riprese");
        }
        
        commessa.setStato(StatoCommessa.IN_LAVORAZIONE);
        commessa = commessaDAO.save(commessa);
        
        logger.info("Commessa {} ripresa", commessa.getNumero());
        return commessa;
    }

    /**
     * Completa commessa
     */
    public Commessa completaCommessa(Long commessaId) throws Exception {
        Commessa commessa = commessaDAO.findById(commessaId);
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non trovata");
        }
        
        if (commessa.getStato() != StatoCommessa.IN_LAVORAZIONE) {
            throw new IllegalStateException("Solo le commesse IN_LAVORAZIONE possono essere completate");
        }
        
        commessa.setStato(StatoCommessa.COMPLETATA);
        commessa.setDataFineEffettiva(new Date());
        commessa.setPercentualeCompletamento(100);
        commessa = commessaDAO.save(commessa);
        
        logger.info("Commessa {} completata", commessa.getNumero());
        return commessa;
    }

    /**
     * Annulla commessa
     */
    public Commessa annullaCommessa(Long commessaId, String motivazione) throws Exception {
        Commessa commessa = commessaDAO.findById(commessaId);
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non trovata");
        }
        
        if (commessa.getStato() == StatoCommessa.CHIUSA) {
            throw new IllegalStateException("Non è possibile annullare una commessa CHIUSA");
        }
        
        commessa.setStato(StatoCommessa.ANNULLATA);
        commessa.setNote((commessa.getNote() != null ? commessa.getNote() + "\n" : "") + 
                        "ANNULLATA: " + motivazione);
        commessa = commessaDAO.save(commessa);
        
        logger.info("Commessa {} annullata", commessa.getNumero());
        return commessa;
    }

    /**
     * Aggiorna percentuale completamento
     */
    public Commessa aggiornaPercentuale(Long commessaId, Integer percentuale) throws Exception {
        if (percentuale < 0 || percentuale > 100) {
            throw new IllegalArgumentException("Percentuale deve essere tra 0 e 100");
        }
        
        Commessa commessa = commessaDAO.findById(commessaId);
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non trovata");
        }
        
        commessa.setPercentualeCompletamento(percentuale);
        commessa = commessaDAO.save(commessa);
        
        return commessa;
    }

    /**
     * Trova tutte le commesse
     */
    public List<Commessa> findAll() {
        return commessaDAO.findAll();
    }

    /**
     * Trova commesse per stato
     */
    public List<Commessa> findByStato(StatoCommessa stato) {
        return commessaDAO.findByStato(stato);
    }

    /**
     * Trova commesse aperte
     */
    public List<Commessa> findAperte() {
        return commessaDAO.findAperte();
    }

    /**
     * Trova commesse scadute
     */
    public List<Commessa> findScadute() {
        return commessaDAO.findScadute();
    }

    /**
     * Cerca commesse
     */
    public List<Commessa> search(String searchTerm) {
        return commessaDAO.search(searchTerm);
    }

    /**
     * Calcola valore totale per stato
     */
    public BigDecimal calcolaValoreTotale(StatoCommessa stato) {
        return commessaDAO.calcolaValoreTotaleByStato(stato);
    }

    /**
     * Carica commessa per ID
     */
    public Commessa findById(Long id) {
        return commessaDAO.findById(id);
    }

    /**
     * Carica commessa con righe
     */
    public Commessa findWithRighe(Long id) {
        return commessaDAO.findWithRighe(id);
    }

    /**
     * Calcola data scadenza
     */
    private Date getDataScadenza(int giorni) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, giorni);
        return cal.getTime();
    }
}
