package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.dao.SdiNotificaDAO;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.SdiNotifica;
import it.zensoftware.luna2.service.SdiNotificheService;
import it.zensoftware.luna2.util.HibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;

/**
 * Action per la gestione delle notifiche SDI (Sistema di Interscambio)
 * Fornisce endpoint per il polling automatico delle notifiche tramite cron job
 */
public class SdiNotificheAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(SdiNotificheAction.class);
    
    private final FatturaDAO fatturaDAO = new FatturaDAO();
    private final SdiNotificaDAO sdiNotificaDAO = new SdiNotificaDAO(HibernateUtil.getSessionFactory());
    private final SdiNotificheService sdiNotificheService = new SdiNotificheService(fatturaDAO, sdiNotificaDAO);
    
    /**
     * Metodo per il polling automatico delle notifiche SDI
     * Eseguito da cron job con frequenza configurabile
     */
    public String sincronizzaNotifiche() {
        try {
            logger.info("Inizio sincronizzazione notifiche SDI");
            sdiNotificheService.sincronizzaTutteNotifiche();
            addActionMessage("Sincronizzazione notifiche SDI completata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore durante la sincronizzazione delle notifiche SDI", e);
            addActionError("Errore durante la sincronizzazione: " + e.getMessage());
            return ERROR;
        }
    }
    
    /**
     * GET che ritorna lo stato dell'ultima notifica per una fattura
     * Utile per polling asincrono dal client
     */
    public String getStatoNotifica() {
        try {
            String fatturaIdParam = ServletActionContext.getRequest().getParameter("fatturaId");
            if (fatturaIdParam == null || fatturaIdParam.isEmpty()) {
                addActionError("ID fattura non fornito");
                return ERROR;
            }
            
            Long fatturaId = Long.parseLong(fatturaIdParam);
            
            Fattura fattura = fatturaDAO.findById(fatturaId);
            if (fattura == null) {
                addActionError("Fattura non trovata");
                return ERROR;
            }
            
            SdiNotifica notifica = sdiNotificaDAO.findByFatturaId(fatturaId);
            if (notifica == null) {
                // Nessuna notifica ancora ricevuta, proviamo a recuperarla
                if (fattura.getSdiCodice() != null && !fattura.getSdiCodice().isEmpty()) {
                    SdiNotificheService.NotificaResponse response = sdiNotificheService.recuperaNotifica(fattura.getSdiCodice());
                    if (response.trovata) {
                        notifica = new SdiNotifica();
                        notifica.setFattura(fattura);
                        notifica.setSdiCodice(fattura.getSdiCodice());
                        notifica.setStato(response.stato);
                        notifica.setXmlRisposta(response.xmlRisposta);
                        notifica.setDescrizioneErrore(response.descrizioneErrore);
                        sdiNotificaDAO.save(notifica);
                        
                        // Aggiorna lo stato della fattura
                        fattura.setSdiStato(response.stato);
                        fatturaDAO.update(fattura);
                    }
                }
            }
            
            if (notifica != null) {
                notifica.setLetta(true);
                sdiNotificaDAO.update(notifica);
                addActionMessage("Stato: " + notifica.getStato());
            } else {
                addActionMessage("Nessuna notifica ricevuta");
            }
            
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore nel recupero dello stato notifica", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }
}
