package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.ModuleSettingDAO;
import it.zensoftware.luna2.model.ModuleSetting;
import it.zensoftware.luna2.model.User;

import java.util.Map;

/**
 * Superuser module settings
 */
public class ModuleSettingsAction extends ActionSupport {

    private static final long serialVersionUID = 1L;

    private final ModuleSettingDAO moduleSettingDAO = new ModuleSettingDAO();

    private boolean coreEnabled;
    private boolean magazzinoEnabled;
    private boolean crmEnabled;
    private boolean produzioneEnabled;
    private boolean aiEnabled;

    public String execute() {
        if (!isSuperUser()) {
            return "forbidden";
        }
        loadCurrentSettings();
        return SUCCESS;
    }

    public String save() {
        if (!isSuperUser()) {
            return "forbidden";
        }
        updateModule("CORE", "Core", coreEnabled, "Anagrafiche, documenti, prodotti, report");
        updateModule("MAGAZZINO", "Magazzino", magazzinoEnabled, "Giacenze e movimentazioni");
        updateModule("CRM", "CRM", crmEnabled, "Lead e pipeline");
        updateModule("PRODUZIONE", "Produzione/Commesse", produzioneEnabled, "Commesse e avanzamento");
        updateModule("AI", "Modulo AI", aiEnabled, "Funzioni AI e automazioni");

        com.opensymphony.xwork2.ActionContext.getContext().getSession()
            .put("enabledModules", moduleSettingDAO.getEnabledMap());

        addActionMessage("Impostazioni moduli aggiornate");
        loadCurrentSettings();
        return SUCCESS;
    }

    private void updateModule(String code, String name, boolean enabled, String description) {
        ModuleSetting setting = moduleSettingDAO.findByCode(code);
        if (setting == null) {
            setting = new ModuleSetting(code, name, enabled, description);
        } else {
            setting.setName(name);
            setting.setEnabled(enabled);
            setting.setDescription(description);
        }
        moduleSettingDAO.saveOrUpdate(setting);
    }

    private void loadCurrentSettings() {
        coreEnabled = isEnabled("CORE", true);
        magazzinoEnabled = isEnabled("MAGAZZINO", true);
        crmEnabled = isEnabled("CRM", true);
        produzioneEnabled = isEnabled("PRODUZIONE", false);
        aiEnabled = isEnabled("AI", false);
    }

    private boolean isEnabled(String code, boolean defaultValue) {
        ModuleSetting setting = moduleSettingDAO.findByCode(code);
        if (setting == null) {
            return defaultValue;
        }
        return Boolean.TRUE.equals(setting.getEnabled());
    }

    private boolean isSuperUser() {
        Map<String, Object> session = com.opensymphony.xwork2.ActionContext.getContext().getSession();
        User user = (User) session.get("currentUser");
        return user != null && "admin".equalsIgnoreCase(user.getUsername());
    }

    public boolean isCoreEnabled() {
        return coreEnabled;
    }

    public void setCoreEnabled(boolean coreEnabled) {
        this.coreEnabled = coreEnabled;
    }

    public boolean isMagazzinoEnabled() {
        return magazzinoEnabled;
    }

    public void setMagazzinoEnabled(boolean magazzinoEnabled) {
        this.magazzinoEnabled = magazzinoEnabled;
    }

    public boolean isCrmEnabled() {
        return crmEnabled;
    }

    public void setCrmEnabled(boolean crmEnabled) {
        this.crmEnabled = crmEnabled;
    }

    public boolean isProduzioneEnabled() {
        return produzioneEnabled;
    }

    public void setProduzioneEnabled(boolean produzioneEnabled) {
        this.produzioneEnabled = produzioneEnabled;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }
}
