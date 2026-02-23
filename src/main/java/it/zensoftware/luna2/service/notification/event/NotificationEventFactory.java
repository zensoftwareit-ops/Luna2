package it.zensoftware.luna2.service.notification.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Factory per creare eventi di notifica senza esporre le classi package-private.
 */
public final class NotificationEventFactory {

    private NotificationEventFactory() {
    }

    public static NotificationEvent fatturaArrivata(String userId, String numeroFattura, String fornitore, BigDecimal importo) {
        return new FatturaArrivataEvent(userId, numeroFattura, fornitore, toDouble(importo));
    }

    public static NotificationEvent notificaSdi(String userId, String tipoNotifica, String descrizione, String numeroDocumento) {
        return new NotificaSDIEvent(userId, tipoNotifica, descrizione, numeroDocumento);
    }

    public static NotificationEvent preventivoAperto(String userId, String numeroPreventivo, String cliente, LocalDateTime dataApertura) {
        return new PreventivoApertoEvent(userId, numeroPreventivo, cliente, dataApertura);
    }

    public static NotificationEvent preventivoLetto(String userId, String numeroPreventivo, String cliente, int tempoLettura) {
        return new PreventivoLettoEvent(userId, numeroPreventivo, cliente, tempoLettura);
    }

    public static NotificationEvent ordineConfermato(String userId, String numeroOrdine, String cliente, BigDecimal importo) {
        return new OrdineConfirmatoEvent(userId, numeroOrdine, cliente, toDouble(importo));
    }

    public static NotificationEvent scadenzaImminente(String userId, String tipoDocumento, String numeroDocumento,
                                                      String descrizione, int giorniRimanenti) {
        return new ScadenzaImminenteEvent(userId, tipoDocumento, numeroDocumento, descrizione, giorniRimanenti);
    }

    public static NotificationEvent merceInMagazzino(String userId, String codiceArticolo, String descrizione,
                                                     int quantita, String fornitore) {
        return new MerceInMagazzinoEvent(userId, codiceArticolo, descrizione, quantita, fornitore);
    }

    private static Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}
