package it.zensoftware.luna2.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Hibernate Utility class with a convenient method to get Session Factory object.
 */
public class HibernateUtil {
    
    private static final Logger logger = LogManager.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;

    static {
        try {
            // Load configuration from hibernate.cfg.xml
            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .configure("hibernate.cfg.xml")
                    .build();
            
            logger.info("StandardServiceRegistry created from hibernate.cfg.xml");

            MetadataSources metadataSources = new MetadataSources(registry);
            
            // Register annotated classes
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.User.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Cliente.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Fornitore.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Ordine.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.OrdineRiga.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Fattura.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.FatturaRiga.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Preventivo.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.PreventivoRiga.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Prodotto.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Magazzino.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Contatto.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Lead.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Tag.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.ModuleSetting.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.TrackingEmail.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.SdiNotifica.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.FatturaPassiva.class);
            
            logger.info("Annotated classes registered");

            Metadata metadata = metadataSources.buildMetadata();
            
            logger.info("Metadata created");
            
            sessionFactory = metadata.getSessionFactoryBuilder().build();
            
            logger.info("SessionFactory created successfully");
            
        } catch (Throwable ex) {
            logger.error("Initial SessionFactory creation failed.", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            logger.info("SessionFactory closed");
        }
    }
}
