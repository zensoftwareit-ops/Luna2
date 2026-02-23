package it.zensoftware.luna2.service.notification.event;

import java.time.LocalDateTime;

/**
 * FatturaArrivataEvent - Evento notifica fattura passiva ricevuta
 */
class FatturaArrivataEvent extends NotificationEvent {
    
    public FatturaArrivataEvent(String userId, String numeroFattura, String fornitore, Double importo) {
        super("FATTURA_ARRIVATA", userId);
        addData("numeroFattura", numeroFattura);
        addData("fornitore", fornitore);
        addData("importo", importo);
    }
    
    @Override
    public String getEmailTemplate() {
        return "email_fattura_arrivata.html";
    }
    
    @Override
    public String getEmailSubject() {
        return "📩 Fattura ricevuta da " + getData().get("fornitore");
    }
    
    @Override
    public String getPushTitle() {
        return "Nuova Fattura";
    }
    
    @Override
    public String getPushMessage() {
        return "Fattura #" + getData().get("numeroFattura") + " da " + 
               getData().get("fornitore") + " - €" + getData().get("importo");
    }
    
    @Override
    public Priority getPriority() {
        return Priority.HIGH;
    }
}

/**
 * NotificaSDIEvent - Evento notifica SDI (Sistema Di Interscambio)
 */
class NotificaSDIEvent extends NotificationEvent {
    
    public NotificaSDIEvent(String userId, String tipoNotifica, String descrizione, String numeroDocumento) {
        super("NOTIFICA_SDI", userId);
        addData("tipoNotifica", tipoNotifica);  // MC, AT, ER, NS, etc
        addData("descrizione", descrizione);
        addData("numeroDocumento", numeroDocumento);
    }
    
    @Override
    public String getEmailTemplate() {
        return "email_notifica_sdi.html";
    }
    
    @Override
    public String getEmailSubject() {
        return "📧 Notifica SDI: " + getData().get("tipoNotifica");
    }
    
    @Override
    public String getPushTitle() {
        return "Notifica SDI";
    }
    
    @Override
    public String getPushMessage() {
        return "Notifica SDI " + getData().get("tipoNotifica") + " per doc. #" + 
               getData().get("numeroDocumento");
    }
    
    @Override
    public Priority getPriority() {
        return Priority.HIGH;
    }
}

/**
 * PreventivoApertoEvent - Evento quando preventivo viene aperto da cliente
 */
class PreventivoApertoEvent extends NotificationEvent {
    
    public PreventivoApertoEvent(String userId, String numeroPreventivo, String cliente, LocalDateTime dataApertura) {
        super("PREVENTIVO_APERTO", userId);
        addData("numeroPreventivo", numeroPreventivo);
        addData("cliente", cliente);
        addData("dataApertura", dataApertura);
    }
    
    @Override
    public String getEmailTemplate() {
        return "email_preventivo_aperto.html";
    }
    
    @Override
    public String getEmailSubject() {
        return "👁️ Preventivo #" + getData().get("numeroPreventivo") + " aperto da cliente";
    }
    
    @Override
    public String getPushTitle() {
        return "Preventivo Visualizzato";
    }
    
    @Override
    public String getPushMessage() {
        return "Preventivo #" + getData().get("numeroPreventivo") + " aperto da " + 
               getData().get("cliente");
    }
    
    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }
}

/**
 * PreventivoLettoEvent - Evento quando preventivo è stato effettivamente letto (>30 sec)
 */
class PreventivoLettoEvent extends NotificationEvent {
    
    public PreventivoLettoEvent(String userId, String numeroPreventivo, String cliente, Integer tempoLettura) {
        super("PREVENTIVO_LETTO", userId);
        addData("numeroPreventivo", numeroPreventivo);
        addData("cliente", cliente);
        addData("tempoLettura", tempoLettura);  // secondi
    }
    
    @Override
    public String getEmailTemplate() {
        return "email_preventivo_letto.html";
    }
    
    @Override
    public String getEmailSubject() {
        return "✅ Preventivo #" + getData().get("numeroPreventivo") + " letto";
    }
    
    @Override
    public String getPushTitle() {
        return "Preventivo Letto";
    }
    
    @Override
    public String getPushMessage() {
        return "Preventivo #" + getData().get("numeroPreventivo") + " letto da " + 
               getData().get("cliente");
    }
    
    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }
}

/**
 * OrdineConfermato Event - Evento quando ordine è confermato
 */
class OrdineConfirmatoEvent extends NotificationEvent {
    
    public OrdineConfirmatoEvent(String userId, String numeroOrdine, String cliente, Double importo) {
        super("ORDINE_CONFERMATO", userId);
        addData("numeroOrdine", numeroOrdine);
        addData("cliente", cliente);
        addData("importo", importo);
    }
    
    @Override
    public String getEmailTemplate() {
        return "email_ordine_confermato.html";
    }
    
    @Override
    public String getEmailSubject() {
        return "🎉 Ordine #" + getData().get("numeroOrdine") + " confermato!";
    }
    
    @Override
    public String getPushTitle() {
        return "Ordine Confermato";
    }
    
    @Override
    public String getPushMessage() {
        return "Ordine #" + getData().get("numeroOrdine") + " da " + 
               getData().get("cliente") + " - €" + getData().get("importo");
    }
    
    @Override
    public Priority getPriority() {
        return Priority.HIGH;
    }
}

/**
 * ScadenzaImminenteEvent - Evento reminder scadenza imminente
 */
class ScadenzaImminenteEvent extends NotificationEvent {
    
    public ScadenzaImminenteEvent(String userId, String tipoDocumento, String numeroDocumento, 
                                  String descrizione, Integer giorniRimanenti) {
        super("SCADENZA_IMMINENTE", userId);
        addData("tipoDocumento", tipoDocumento);  // FATTURA, PREVENTIVO, ORDINE
        addData("numeroDocumento", numeroDocumento);
        addData("descrizione", descrizione);
        addData("giorniRimanenti", giorniRimanenti);
    }
    
    @Override
    public String getEmailTemplate() {
        return "email_scadenza_imminente.html";
    }
    
    @Override
    public String getEmailSubject() {
        return "⏰ Scadenza imminente: " + getData().get("descrizione");
    }
    
    @Override
    public String getPushTitle() {
        return "Scadenza in arrivo";
    }
    
    @Override
    public String getPushMessage() {
        return getData().get("descrizione") + " - " + 
               getData().get("giorniRimanenti") + " giorni rimanenti";
    }
    
    @Override
    public Priority getPriority() {
        return Priority.HIGH;
    }
}

/**
 * MerceInMagazzinoEvent - Evento quando merce arriva in magazzino
 */
class MerceInMagazzinoEvent extends NotificationEvent {
    
    public MerceInMagazzinoEvent(String userId, String codiceArticolo, String descrizione, 
                                 Integer quantita, String fornitore) {
        super("MERCE_IN_MAGAZZINO", userId);
        addData("codiceArticolo", codiceArticolo);
        addData("descrizione", descrizione);
        addData("quantita", quantita);
        addData("fornitore", fornitore);
    }
    
    @Override
    public String getEmailTemplate() {
        return "email_merce_magazzino.html";
    }
    
    @Override
    public String getEmailSubject() {
        return "📦 Merce in magazzino: " + getData().get("descrizione");
    }
    
    @Override
    public String getPushTitle() {
        return "Merce arrivata";
    }
    
    @Override
    public String getPushMessage() {
        return getData().get("descrizione") + " (x" + 
               getData().get("quantita") + ") da " + getData().get("fornitore");
    }
    
    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }
}
