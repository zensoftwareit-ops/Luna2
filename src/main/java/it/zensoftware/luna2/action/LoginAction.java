package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.UserDAO;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Map;

/**
 * Login Action for user authentication
 */
public class LoginAction extends ActionSupport {
    
    private static final Logger logger = LogManager.getLogger(LoginAction.class);
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private UserDAO userDAO = new UserDAO();

    public String execute() {
        return SUCCESS;
    }

    public String authenticate() {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            addFieldError("username", "Username e password sono obbligatori");
            return INPUT;
        }

        try {
            User user = userDAO.authenticate(username, password);
            
            if (user != null) {
                // Update last access
                user.setUltimoAccesso(new Date());
                userDAO.update(user);

                // Store user in session
                Map<String, Object> session = getSession();
                session.put("currentUser", user);
                
                logger.info("User logged in: " + username);
                addActionMessage("Benvenuto, " + user.getNomeCompleto() + "!");
                return SUCCESS;
            } else {
                addFieldError("username", "Username o password non corretti");
                return INPUT;
            }
        } catch (Exception e) {
            logger.error("Login error", e);
            addActionError("Errore durante il login. Riprova.");
            return INPUT;
        }
    }

    public String logout() {
        Map<String, Object> session = getSession();
        User user = (User) session.get("currentUser");
        
        if (user != null) {
            logger.info("User logged out: " + user.getUsername());
        }
        
        session.clear();
        addActionMessage("Logout effettuato con successo");
        return SUCCESS;
    }

    // Helper method to get session
    private Map<String, Object> getSession() {
        return com.opensymphony.xwork2.ActionContext.getContext().getSession();
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
