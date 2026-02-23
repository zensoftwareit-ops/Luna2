package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.NotificationPreferenceDAO;
import it.zensoftware.luna2.model.NotificationPreference;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NotificationSettingsAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(NotificationSettingsAction.class);

    private final NotificationPreferenceDAO preferenceDAO = new NotificationPreferenceDAO();

    private NotificationPreference preference;
    private List<String> eventTypes;

    public String view() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return LOGIN;
        }

        preference = preferenceDAO.findByUserId(String.valueOf(currentUser.getId()));
        if (preference == null) {
            preference = new NotificationPreference();
            preference.setUserId(String.valueOf(currentUser.getId()));
        }

        if (preference.getEnabledEventTypes() != null && !preference.getEnabledEventTypes().isEmpty()) {
            eventTypes = Arrays.asList(preference.getEnabledEventTypes().split(","));
        }

        return SUCCESS;
    }

    public String save() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return LOGIN;
        }

        try {
            NotificationPreference existing = preferenceDAO.findByUserId(String.valueOf(currentUser.getId()));
            if (existing == null) {
                existing = new NotificationPreference();
                existing.setUserId(String.valueOf(currentUser.getId()));
            }

            existing.setEmailEnabled(preference.getEmailEnabled());
            existing.setPushEnabled(preference.getPushEnabled());
            existing.setSmsEnabled(preference.getSmsEnabled());
            existing.setDigestFrequency(preference.getDigestFrequency());
            existing.setQuietStartTime(preference.getQuietStartTime());
            existing.setQuietEndTime(preference.getQuietEndTime());

            if (eventTypes != null && !eventTypes.isEmpty()) {
                existing.setEnabledEventTypes(eventTypes.stream().map(String::trim).collect(Collectors.joining(",")));
            } else {
                existing.setEnabledEventTypes(null);
            }

            if (existing.getId() == null) {
                preferenceDAO.save(existing);
            } else {
                preferenceDAO.update(existing);
            }

            addActionMessage("Preferenze notifiche salvate");
            preference = existing;
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore salvataggio preferenze notifiche", e);
            addActionError("Errore durante il salvataggio");
            return ERROR;
        }
    }

    private User getCurrentUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    public NotificationPreference getPreference() { return preference; }
    public void setPreference(NotificationPreference preference) { this.preference = preference; }

    public List<String> getEventTypes() { return eventTypes; }
    public void setEventTypes(List<String> eventTypes) { this.eventTypes = eventTypes; }

    public NotificationPreference.DigestFrequency[] getDigestFrequencies() {
        return NotificationPreference.DigestFrequency.values();
    }
}
