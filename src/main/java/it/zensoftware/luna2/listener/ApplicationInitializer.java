package it.zensoftware.luna2.listener;

import it.zensoftware.luna2.model.ModuleSetting;
import it.zensoftware.luna2.model.User;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Date;

/**
 * Application listener that initializes default data
 */
@WebListener
public class ApplicationInitializer implements ServletContextListener {
    
    private static final Logger logger = LogManager.getLogger(ApplicationInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Application initializing...");
        
        try {
            // Initialize default users if the table is empty
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            Session session = sessionFactory.openSession();
            
            try {
                session.beginTransaction();
                
                // Check if admin user exists
                Long adminCount = (Long) session.createQuery(
                    "SELECT COUNT(*) FROM User WHERE username = 'admin'"
                ).uniqueResult();
                
                if (adminCount == 0) {
                    logger.info("Creating default admin user...");
                    
                    // Create admin user with hashed password
                    User adminUser = new User();
                    adminUser.setUsername("admin");
                    adminUser.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw("admin", org.mindrot.jbcrypt.BCrypt.gensalt()));  // Hashed password
                    adminUser.setEmail("admin@luna2.com");
                    adminUser.setNome("Admin");
                    adminUser.setCognome("System");
                    adminUser.setRuolo(User.Ruolo.ADMIN);
                    adminUser.setAttivo(true);
                    adminUser.setDataCreazione(new Date());
                    adminUser.setDataModifica(new Date());
                    
                    session.save(adminUser);
                    
                    logger.info("Admin user created: username=admin, password=admin");
                }

                    Long moduleCount = (Long) session.createQuery(
                        "SELECT COUNT(*) FROM ModuleSetting"
                    ).uniqueResult();

                    if (moduleCount == 0) {
                        logger.info("Creating default module settings...");

                        session.save(new ModuleSetting("CORE", "Core", true,
                            "Anagrafiche, documenti, prodotti, report"));
                        session.save(new ModuleSetting("MAGAZZINO", "Magazzino", true,
                            "Giacenze e movimentazioni"));
                        session.save(new ModuleSetting("CRM", "CRM", true,
                            "Lead e pipeline"));
                        session.save(new ModuleSetting("PRODUZIONE", "Produzione/Commesse", false,
                            "Commesse e avanzamento"));
                        session.save(new ModuleSetting("AI", "Modulo AI", false,
                            "Funzioni AI e automazioni"));
                    }
                
                session.getTransaction().commit();
                logger.info("Application initialization completed successfully");
                
            } catch (Exception e) {
                session.getTransaction().rollback();
                logger.error("Error during initialization", e);
            } finally {
                session.close();
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize application", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Application shutting down...");
        HibernateUtil.getSessionFactory().close();
    }
}
