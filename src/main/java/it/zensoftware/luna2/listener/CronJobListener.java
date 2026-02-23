package it.zensoftware.luna2.listener;

import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.dao.ReminderDAO;
import it.zensoftware.luna2.dao.SdiNotificaDAO;
import it.zensoftware.luna2.model.Reminder;
import it.zensoftware.luna2.model.User;
import it.zensoftware.luna2.service.SdiNotificheService;
import it.zensoftware.luna2.service.calendar.CalendarSyncService;
import it.zensoftware.luna2.service.notification.EventPublisher;
import it.zensoftware.luna2.service.notification.event.NotificationEventFactory;
import it.zensoftware.luna2.util.HibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Date;
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
    private Timer timerReminder;
    private Timer timerCalendar;
    
    // Frequenza di polling in millisecondi (default: 5 minuti)
    private static final long POLLING_INTERVAL = 5 * 60 * 1000;
    private static final long REMINDER_INTERVAL = 10 * 60 * 1000;
    private static final long CALENDAR_SYNC_INTERVAL = 15 * 60 * 1000;
    
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

                // Avvia timer notifiche reminder
                timerReminder = new Timer("ReminderNotifications", true);
                timerReminder.scheduleAtFixedRate(new ReminderNotificationTask(),
                    REMINDER_INTERVAL,
                    REMINDER_INTERVAL
                );

                logger.info("Reminder notifications avviate con frequenza " + (REMINDER_INTERVAL / 1000 / 60) + " minuti");

                // Avvia timer sincronizzazione calendario
                timerCalendar = new Timer("CalendarSync", true);
                timerCalendar.scheduleAtFixedRate(new CalendarSyncTask(),
                    CALENDAR_SYNC_INTERVAL,
                    CALENDAR_SYNC_INTERVAL
                );

                logger.info("Calendar sync avviato con frequenza " + (CALENDAR_SYNC_INTERVAL / 1000 / 60) + " minuti");
            
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
            if (timerReminder != null) {
                timerReminder.cancel();
                logger.info("Reminder notifications fermate");
            }
            if (timerCalendar != null) {
                timerCalendar.cancel();
                logger.info("Calendar sync fermato");
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

    /**
     * Task che invia notifiche su reminder in scadenza.
     */
    private static class ReminderNotificationTask extends TimerTask {
        private static final Logger logger = LogManager.getLogger(ReminderNotificationTask.class);

        @Override
        public void run() {
            try {
                ReminderDAO reminderDAO = new ReminderDAO();
                Date now = new Date();
                Date future = new Date(now.getTime() + (1000L * 60 * 60 * 24 * 3));

                for (Reminder reminder : reminderDAO.findBetweenDates(now, future)) {
                    if (reminder.getStato() != Reminder.ReminderStatus.PENDING) {
                        continue;
                    }

                    User utente = reminder.getUtente();
                    if (utente == null) {
                        continue;
                    }

                    long diff = reminder.getDataNotifica().getTime() - now.getTime();
                    int giorni = (int) Math.max(0, diff / (1000 * 60 * 60 * 24));

                    EventPublisher.getInstance().publishEvent(
                            NotificationEventFactory.scadenzaImminente(
                                    String.valueOf(utente.getId()),
                                    "REMINDER",
                                    String.valueOf(reminder.getId()),
                                    reminder.getTitolo() != null ? reminder.getTitolo() : reminder.getReferenza(),
                                    giorni
                            )
                    );

                    reminder.setStato(Reminder.ReminderStatus.SENT);
                    reminder.setDataInvio(new Date());
                    reminderDAO.update(reminder);
                }
            } catch (Exception e) {
                logger.error("Errore durante reminder notification task", e);
            }
        }
    }

    /**
     * Task che sincronizza i calendari esterni.
     */
    private static class CalendarSyncTask extends TimerTask {
        private static final Logger logger = LogManager.getLogger(CalendarSyncTask.class);

        @Override
        public void run() {
            try {
                new CalendarSyncService().syncAllAccounts();
            } catch (Exception e) {
                logger.error("Errore durante calendar sync", e);
            }
        }
    }
}
