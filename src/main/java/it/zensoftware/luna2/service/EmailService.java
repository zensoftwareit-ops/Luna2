package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.TrackingEmailDAO;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.TrackingEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class EmailService {
    private static final Logger logger = LogManager.getLogger(EmailService.class);
    private static final Properties mailProps = new Properties();
    private TrackingEmailDAO trackingEmailDAO = new TrackingEmailDAO();

    static {
        try (FileInputStream fis = new FileInputStream("application.properties")) {
            Properties props = new Properties();
            props.load(fis);
            mailProps.put("mail.smtp.host", props.getProperty("smtp.host", "smtp.gmail.com"));
            mailProps.put("mail.smtp.port", props.getProperty("smtp.port", "587"));
            mailProps.put("mail.smtp.auth", props.getProperty("smtp.auth", "true"));
            mailProps.put("mail.smtp.starttls.enable", props.getProperty("smtp.starttls.enable", "true"));
            mailProps.put("mail.smtp.starttls.required", props.getProperty("smtp.starttls.required", "true"));
        } catch (IOException e) {
            logger.warn("application.properties not found, using defaults");
        }
    }

    public TrackingEmail sendPreventiveEmail(Preventivo preventivo, String emailDestinatario, String emailBody, 
                                              String smtpUsername, String smtpPassword, String smtpFromEmail) {
        TrackingEmail tracking = new TrackingEmail();
        String trackingId = UUID.randomUUID().toString();
        tracking.setTrackingId(trackingId);
        tracking.setPreventivo(preventivo);
        tracking.setEmailDestinatario(emailDestinatario);
        tracking.setDataInvio(new Date());

        try {
            Session session = createSession(smtpUsername, smtpPassword);
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpFromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailDestinatario));
            message.setSubject("Preventivo: " + preventivo.getNumero());

            // Crea il corpo della mail con HTML e tracciamento pixel
            String trackingPixelUrl = "http://localhost:8080/luna2/app/documenti/preventivi-trackPixel.action?tid=" + trackingId;
            String htmlContent = prepareEmailContent(emailBody, preventivo, trackingId, trackingPixelUrl);

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Email inviata a " + emailDestinatario + " con tracking ID: " + trackingId);

            // Salva il tracciamento
            trackingEmailDAO.save(tracking);
            return tracking;

        } catch (Exception e) {
            logger.error("Errore nell'invio email", e);
            throw new RuntimeException("Errore nell'invio della mail: " + e.getMessage());
        }
    }

    private Session createSession(String username, String password) {
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
        return Session.getInstance(mailProps, authenticator);
    }

    private String prepareEmailContent(String userMessage, Preventivo preventivo, String trackingId, String trackingPixelUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }" +
                ".content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }" +
                ".footer { background-color: #eee; padding: 15px; text-align: center; font-size: 12px; border-radius: 0 0 5px 5px; }" +
                ".button { background-color: #4CAF50; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 20px 0; }" +
                ".preventivo-info { background-color: white; padding: 15px; margin: 10px 0; border-left: 4px solid #4CAF50; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'><h1>Nuovo Preventivo</h1></div>" +
                "<div class='content'>" +
                "<p>" + userMessage + "</p>" +
                "<div class='preventivo-info'>" +
                "<strong>Numero Preventivo:</strong> " + preventivo.getNumero() + "<br>" +
                "<strong>Data:</strong> " + new java.text.SimpleDateFormat("dd/MM/yyyy").format(preventivo.getDataPreventivo()) + "<br>" +
                (preventivo.getCliente() != null ? "<strong>Cliente:</strong> " + preventivo.getCliente().getRagioneSociale() + "<br>" : "") +
                "<strong>Importo:</strong> € " + String.format("%.2f", preventivo.getTotale()) +
                "</div>" +
                "<p style='text-align: center;'>" +
                "<a href='http://localhost:8080/luna2/app/documenti/preventivi-downloadTracked.action?tid=" + trackingId + "&tipo=tecnico' class='button'>Scarica Preventivo (Tecnico)</a>" +
                "</p>" +
                "<p style='text-align: center;'>" +
                "<a href='http://localhost:8080/luna2/app/documenti/preventivi-downloadTracked.action?tid=" + trackingId + "&tipo=descrittivo' class='button' style='background-color: #2196F3;'>Scarica Preventivo (Descrittivo)</a>" +
                "</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>&copy; 2026 Luna2 - Gestionale Cloud. Tutti i diritti riservati.</p>" +
                "</div>" +
                "</div>" +
                "<img src='" + trackingPixelUrl + "' width='1' height='1' style='display:none;' alt='' />" +
                "</body>" +
                "</html>";
    }

    public void trackPixelOpen(String trackingId, String userAgent) {
        TrackingEmail tracking = trackingEmailDAO.findByTrackingId(trackingId);
        if (tracking != null) {
            tracking.setAperto(true);
            tracking.setDataApertura(new Date());
            tracking.setUserAgentApertura(userAgent);
            trackingEmailDAO.update(tracking);
            logger.info("Email aperta - Tracking ID: " + trackingId);
        }
    }

    public void trackDownload(String trackingId, String userAgent) {
        TrackingEmail tracking = trackingEmailDAO.findByTrackingId(trackingId);
        if (tracking != null) {
            tracking.setClickDownload(tracking.getClickDownload() + 1);
            if (tracking.getDataPrimoDownload() == null) {
                tracking.setDataPrimoDownload(new Date());
            }
            tracking.setDataUltimoDownload(new Date());
            tracking.setUserAgentDownload(userAgent);
            trackingEmailDAO.update(tracking);
            logger.info("Download tracciato - Tracking ID: " + trackingId + " - Click #" + tracking.getClickDownload());
        }
    }

    public TrackingEmail getTrackingInfo(String trackingId) {
        return trackingEmailDAO.findByTrackingId(trackingId);
    }
}
