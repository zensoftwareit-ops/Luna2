package it.zensoftware.luna2.interceptor;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import it.zensoftware.luna2.dao.ModuleSettingDAO;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Interceptor to enforce module enable/disable rules
 */
public class ModuleAccessInterceptor extends AbstractInterceptor {

    private static final Logger logger = LogManager.getLogger(ModuleAccessInterceptor.class);
    private final ModuleSettingDAO moduleSettingDAO = new ModuleSettingDAO();

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        Map<String, Object> session = invocation.getInvocationContext().getSession();
        User user = (User) session.get("currentUser");

        if (user == null) {
            return invocation.invoke();
        }

        if (isSuperUser(user)) {
            session.put("enabledModules", moduleSettingDAO.getEnabledMap());
            return invocation.invoke();
        }

        Map<String, Boolean> enabled = moduleSettingDAO.getEnabledMap();
        session.put("enabledModules", enabled);

        String namespace = invocation.getProxy().getNamespace();
        String moduleCode = resolveModule(namespace);

        if (moduleCode != null && Boolean.FALSE.equals(enabled.get(moduleCode))) {
            logger.warn("Module disabled: " + moduleCode + " for user " + user.getUsername());
            return "moduleDisabled";
        }

        return invocation.invoke();
    }

    private boolean isSuperUser(User user) {
        return user != null && "admin".equalsIgnoreCase(user.getUsername());
    }

    private String resolveModule(String namespace) {
        if (namespace == null) {
            return null;
        }
        if (namespace.startsWith("/app/clienti") || namespace.startsWith("/app/fornitori")
                || namespace.startsWith("/app/prodotti") || namespace.startsWith("/app/documenti")
                || namespace.startsWith("/app/report")) {
            return "CORE";
        }
        if (namespace.startsWith("/app/magazzino")) {
            return "MAGAZZINO";
        }
        if (namespace.startsWith("/app/lead") || namespace.startsWith("/app/crm")) {
            return "CRM";
        }
        if (namespace.startsWith("/app/noleggio")) {
            return "CRM_BROKER_AUTO";
        }
        if (namespace.startsWith("/app/produzione") || namespace.startsWith("/app/commesse")) {
            return "PRODUZIONE";
        }
        if (namespace.startsWith("/app/ai")) {
            return "AI";
        }
        if (namespace.startsWith("/app/contabilita")) {
            return "CONTABILITA";
        }
        if (namespace.startsWith("/app/admin")) {
            return null;
        }
        return null;
    }
}
