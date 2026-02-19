package it.zensoftware.luna2.listener;

import it.zensoftware.luna2.dao.FatturaPassivaDAO;
import it.zensoftware.luna2.dao.FornitoreDAO;
import it.zensoftware.luna2.service.FatturePassiveService;
import it.zensoftware.luna2.util.HibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Aggancia il polling automatico delle fatture passive ricevute da SDI
 * Esecuzione periodica come cron job
 */
public class FatturePassivePollerTask extends TimerTask {
    private static final Logger logger = LogManager.getLogger(FatturePassivePollerTask.class);
    
    @Override
    public void run() {
        try {
            logger.debug("Inizio polling fatture passive da SDI");
            
            // Leggi la PIVA aziendale
            String partitaIva = getCompanyPartitaIva();
            if (partitaIva == null || partitaIva.isEmpty()) {
                logger.warn("PIVA aziendale non configurata, polling fatture passive saltato");
                return;
            }
            
            FatturaPassivaDAO fatturaPassivaDAO = new FatturaPassivaDAO();
            FornitoreDAO fornitoreDAO = new FornitoreDAO();
            FatturePassiveService service = new FatturePassiveService(fatturaPassivaDAO, fornitoreDAO);
            
            service.sincronizzaFatturePassive(partitaIva);
            
            logger.debug("Polling fatture passive completato");
        } catch (Exception e) {
            logger.error("Errore durante il polling delle fatture passive", e);
        }
    }
    
    /**
     * Estrae la partita IVA aziendale da application.properties
     */
    private String getCompanyPartitaIva() {
        try {
            Properties props = new Properties();
            props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
            return props.getProperty("company.vat", "");
        } catch (Exception e) {
            logger.warn("Errore nella lettura della PIVA aziendale: " + e.getMessage());
            return null;
        }
    }
}
