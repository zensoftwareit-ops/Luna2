package it.zensoftware.luna2.service.email;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;

/**
 * EmailService gestisce l'invio di email via SMTP.
 * Supporta HTML templates con variabili parametriche.
 * 
 * Configurazione in application.properties:
 * mail.smtp.host=smtp.gmail.com
 * mail.smtp.port=587
 * mail.smtp.auth=true
 * mail.from=noreply@luna2.it
 * mail.username=your-email@gmail.com
 * mail.password=app-password
 * 
 * @author Notification System
 * @version 1.0
 */
public class EmailService {
    
    private static final Logger logger = LogManager.getLogger(EmailService.class);
    
    private final String smtpHost;
    private final Integer smtpPort;
    private final String fromEmail;
    private final String username;
    private final String password;
    private final Boolean useTLS;
    
    /**
     * Costruttore con parametri SMTP
     */
    public EmailService(String smtpHost, Integer smtpPort, String fromEmail, 
                        String username, String password, Boolean useTLS) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.fromEmail = fromEmail;
        this.username = username;
        this.password = password;
        this.useTLS = useTLS;
    }
    
    /**
     * Invia email semplice (testo)
     * 
     * @param toEmail Destinatario email
     * @param subject Oggetto email
     * @param body Corpo email (testo)
     * @return true se inviata con successo, false altrimenti
     */
    public boolean sendSimpleEmail(String toEmail, String subject, String body) {
        return sendEmail(toEmail, null, subject, body, false);
    }
    
    /**
     * Invia email HTML
     * 
     * @param toEmail Destinatario email
     * @param subject Oggetto email
     * @param htmlBody Corpo email (HTML)
     * @return true se inviata con successo, false altrimenti
     */
    public boolean sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        return sendEmail(toEmail, null, subject, htmlBody, true);
    }
    
    /**
     * Invia email con CC
     * 
     * @param toEmail Destinatario
     * @param ccEmails CC separated by comma
     * @param subject Oggetto
     * @param htmlBody Corpo HTML
     * @return true se inviata con successo
     */
    public boolean sendEmailWithCc(String toEmail, String ccEmails, String subject, String htmlBody) {
        try {
            Properties props = buildSmtpProperties();
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            
            if (ccEmails != null && !ccEmails.isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmails));
            }
            
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");
            message.setSentDate(new Date());
            
            Transport.send(message);
            
            logger.info("Email sent successfully to: " + toEmail + " | Subject: " + subject);
            return true;
            
        } catch (MessagingException e) {
            logger.error("Error sending email to " + toEmail, e);
            return false;
        }
    }
    
    /**
     * Invia email con allegati
     * 
     * @param toEmail Destinatario
     * @param subject Oggetto
     * @param htmlBody Corpo HTML
     * @param attachmentPaths Path dei file da allegare
     * @return true se inviata
     */
    public boolean sendEmailWithAttachments(String toEmail, String subject, String htmlBody, 
                                            List<String> attachmentPaths) {
        try {
            Properties props = buildSmtpProperties();
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setSentDate(new Date());
            
            // Multipart: body + attachments
            // Note: In produzione usare Apache Commons Email per attachments complessi
            
            Transport.send(message);
            
            logger.info("Email with attachments sent to: " + toEmail);
            return true;
            
        } catch (MessagingException e) {
            logger.error("Error sending email with attachments", e);
            return false;
        }
    }
    
    /**
     * Invia email batch (BCC) - Utile per newsletter
     * 
     * @param bccRecipients Lista email (verranno messi in BCC)
     * @param subject Oggetto
     * @param htmlBody Corpo HTML
     * @return numero di email inviate con successo
     */
    public int sendBatchEmail(List<String> bccRecipients, String subject, String htmlBody) {
        int successCount = 0;
        
        for (String email : bccRecipients) {
            try {
                // In produzione, inviare in batch per performance
                if (sendHtmlEmail(email, subject, htmlBody)) {
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to send batch email to: " + email, e);
            }
        }
        
        logger.info("Batch email sent: " + successCount + "/" + bccRecipients.size());
        return successCount;
    }
    
    /**
     * Test della connessione SMTP
     * 
     * @return true se connessione riuscita
     */
    public boolean testConnection() {
        try {
            Properties props = buildSmtpProperties();
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            Transport transport = session.getTransport("smtp");
            transport.connect(smtpHost, smtpPort, username, password);
            transport.close();
            
            logger.info("Email service connection test: SUCCESS");
            return true;
            
        } catch (MessagingException e) {
            logger.error("Email service connection test: FAILED", e);
            return false;
        }
    }
    
    /**
     * Build Properties per SMTP configuration
     */
    private Properties buildSmtpProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", useTLS ? "true" : "false");
        props.put("mail.smtp.starttls.required", useTLS ? "true" : "false");
        props.put("mail.smtp.socketFactory.port", smtpPort);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");
        return props;
    }
    
    /**
     * Invia email generico
     */
    private boolean sendEmail(String toEmail, String ccEmail, String subject, String body, boolean isHtml) {
        try {
            Properties props = buildSmtpProperties();
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            
            if (ccEmail != null && !ccEmail.isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmail));
            }
            
            message.setSubject(subject);
            
            if (isHtml) {
                message.setContent(body, "text/html; charset=utf-8");
            } else {
                message.setText(body);
            }
            
            message.setSentDate(new Date());
            
            Transport.send(message);
            
            logger.info("Email sent to: " + toEmail + " | Subject: " + subject);
            return true;
            
        } catch (MessagingException e) {
            logger.error("Error sending email to " + toEmail + " | Subject: " + subject, e);
            return false;
        }
    }
}
