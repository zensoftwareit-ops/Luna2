package it.zensoftware.luna2.listener;

import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.dao.SdiNotificaDAO;
import it.zensoftware.luna2.service.SdiNotificheService;
import it.zensoftware.luna2.util.HibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Listener per l'avvio di background task cron
 * Sincronizza automaticamente le notifiche SDI con frequenza periodica
 */
@WebListener
public class CronJobListener implements ServletContextListener {
    
    private static final Logger logger = LogManager.getLogger(CronJobListener.class);
    private Timer timerNotifiche;
    private Timer timerFatturePassive;
    
    // Frequenza di polling in millisecondi (default: 5 minuti)
    private static final long POLLING_INTERVAL = 5 * 60 * 1000;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            logger.info("Avvio del context listener per i cron job SDI");
            
            // Avvia il timer per la sincronizzazione delle notifiche SDI
            timerNotifiche = new Timer("SdiNotifichePoller", true); // daemon thread
            timerNotifiche.scheduleAtFixedRate(new SdiNotifichePollerTask(), 
                    POLLING_INTERVAL, // ritardo iniziale
                    POLLING_INTERVAL  // frequenza di ripetizione
            );
            
            logger.info("Poller SDI Notifiche avviato con frequenza " + (POLLING_INTERVAL / 1000 / 60) + " minuti");
            
            // Avvia il timer per la sincronizzazione delle fatture passive
            timerFatturePassive = new Timer("FatturePassivePoller", true); // daemon thread
            timerFatturePassive.scheduleAtFixedRate(new FatturePassivePollerTask(), 
                    POLLING_INTERVAL, // ritardo iniziale
                    POLLING_INTERVAL  // frequenza di ripetizione
            );
            
            logger.info("Poller Fatture Passive avviato con frequenza " + (POLLING_INTERVAL / 1000 / 60) + " minuti");
            
        } catch (Exception e) {
            logger.error("Errore nell'inizializzazione del cron job listener", e);
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            if (timerNotifiche != null) {
                timerNotifiche.cancel();
                logger.info("Poller SDI Notifiche fermato");
            }
            if (timerFatturePassive != null) {
                timerFatturePassive.cancel();
                logger.info("Poller Fatture Passive fermato");
            }
        } catch (Exception e) {
            logger.error("Errore nella chiusura del cron job listener", e);
        }
    }
    
    /**
     * Task che sincronizza le notifiche SDI
     */
    private static class SdiNotifichePollerTask extends TimerTask {
        private static final Logger logger = LogManager.getLogger(SdiNotifichePollerTask.class);
        
        @Override
        public void run() {
            try {
                logger.debug("Inizio polling notifiche SDI");
                
                FatturaDAO fatturaDAO = new FatturaDAO();
                SdiNotificaDAO sdiNotificaDAO = new SdiNotificaDAO(HibernateUtil.getSessionFactory());
                SdiNotificheService service = new SdiNotificheService(fatturaDAO, sdiNotificaDAO);
                
                service.sincronizzaTutteNotifiche();
                
                logger.debug("Polling notifiche SDI completato");
            } catch (Exception e) {
                logger.error("Errore durante il polling delle notifiche SDI", e);
            }
        }
    }
}
