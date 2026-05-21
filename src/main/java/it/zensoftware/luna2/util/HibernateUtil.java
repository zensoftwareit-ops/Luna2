package it.zensoftware.luna2.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Hibernate Utility class with a convenient method to get Session Factory object.
 */
public class HibernateUtil {
    
    private static final Logger logger = LogManager.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static void applyDbOverride(StandardServiceRegistryBuilder registryBuilder) {
        String dbUrl = firstNonBlank(
                System.getenv("DB_URL"),
                System.getenv("SPRING_DATASOURCE_URL")
        );
        String dbUser = firstNonBlank(
                System.getenv("DB_USER"),
                System.getenv("SPRING_DATASOURCE_USERNAME")
        );
        String dbPassword = firstNonBlank(
                System.getenv("DB_PASSWORD"),
                System.getenv("SPRING_DATASOURCE_PASSWORD")
        );
        String dbDriver = firstNonBlank(
                System.getenv("DB_DRIVER"),
                System.getenv("SPRING_DATASOURCE_DRIVER_CLASS_NAME")
        );

        if (dbUrl != null) {
            registryBuilder.applySetting("hibernate.connection.url", dbUrl);
            logger.info("Hibernate DB URL override applied from environment");
        }
        if (dbUser != null) {
            registryBuilder.applySetting("hibernate.connection.username", dbUser);
            logger.info("Hibernate DB username override applied from environment");
        }
        if (dbPassword != null) {
            registryBuilder.applySetting("hibernate.connection.password", dbPassword);
            logger.info("Hibernate DB password override applied from environment");
        }
        if (dbDriver != null) {
            registryBuilder.applySetting("hibernate.connection.driver_class", dbDriver);
            logger.info("Hibernate DB driver override applied from environment");
        }
    }

    private static StandardServiceRegistryBuilder buildRegistryBuilderWithFallback() {
        StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();

        try {
            registryBuilder.configure("hibernate.cfg.xml");
            logger.info("Hibernate config loaded from hibernate.cfg.xml");
            return registryBuilder;
        } catch (Exception primaryEx) {
            logger.warn("hibernate.cfg.xml non trovato nel classpath, uso configurazione Hibernate di default con override env");
        }

        // Fallback minimo: i valori reali DB arrivano da applyDbOverride() via env.
        registryBuilder.applySetting("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
        registryBuilder.applySetting("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        registryBuilder.applySetting("hibernate.show_sql", "false");
        registryBuilder.applySetting("hibernate.format_sql", "false");
        registryBuilder.applySetting("hibernate.hbm2ddl.auto", "update");
        registryBuilder.applySetting("hibernate.current_session_context_class", "thread");
        registryBuilder.applySetting("hibernate.cache.use_second_level_cache", "false");
        registryBuilder.applySetting("hibernate.cache.use_query_cache", "false");

        return registryBuilder;
    }

    static {
        try {
            StandardServiceRegistryBuilder registryBuilder = buildRegistryBuilderWithFallback();

            // Allow per-instance DB wiring from container env variables.
            applyDbOverride(registryBuilder);

            StandardServiceRegistry registry = registryBuilder.build();
            
            logger.info("StandardServiceRegistry created");

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
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.ProdottoFornitoreListino.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Magazzino.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Contatto.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Lead.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Tag.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.ModuleSetting.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.TrackingEmail.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.SdiNotifica.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.FatturaPassiva.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Commessa.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.ComMessaRiga.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.NotificationPreference.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.NotificationHistory.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.CalendarAccount.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.CalendarEvent.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.ProdottoComponente.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.ProdottoVariante.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.MovimentoMagazzino.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Activity.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Task.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.StoriaLead.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.Reminder.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.PipelineStage.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.NoleggioLead.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.NoleggioPreventivo.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.NoleggioDocumento.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.NoleggioValutazione.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.NoleggioOrdine.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.NoleggioTicket.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.NoleggioContratto.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.NoleggioNBT.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.AccountingProfile.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.AccountingAccount.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.AccountingEntry.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.AccountingEntryLine.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.AccountingAsset.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.TaxDeadline.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.AccountingPostingConfig.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.AccountingReportPreset.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.ApprovalRequest.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.TimeRecord.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.PayrollRun.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.PayrollDetail.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.PayrollEmployeeConfig.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.AdminOperation.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.EcommercePlatform.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.EcommerceOrder.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.EcommerceProduct.class);
            metadataSources.addAnnotatedClass(it.zensoftware.luna2.model.EcommerceSyncLog.class);

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
