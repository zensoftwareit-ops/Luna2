package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.User;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * DAO for User entity
 */
public class UserDAO extends GenericDAOImpl<User, Long> {
    
    private static final Logger logger = LogManager.getLogger(UserDAO.class);

    public UserDAO() {
        super(User.class);
    }

    /**
     * Find user by username
     */
    public User findByUsername(String username) {
        try (Session session = getSession()) {
            Query<User> query = session.createQuery("FROM User WHERE username = :username", User.class);
            query.setParameter("username", username);
            User user = query.uniqueResult();
            logger.debug("Found user by username: " + username);
            return user;
        } catch (Exception e) {
            logger.error("Error finding user by username", e);
            return null;
        }
    }

    /**
     * Find user by email
     */
    public User findByEmail(String email) {
        try (Session session = getSession()) {
            Query<User> query = session.createQuery("FROM User WHERE email = :email", User.class);
            query.setParameter("email", email);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding user by email", e);
            return null;
        }
    }

    /**
     * Authenticate user
     */
    public User authenticate(String username, String password) {
        User user = findByUsername(username);
        if (user != null && user.getAttivo() && checkPassword(password, user.getPassword())) {
            logger.info("User authenticated: " + username);
            return user;
        }
        logger.warn("Authentication failed for user: " + username);
        return null;
    }

    private boolean checkPassword(String plainPassword, String hashedPassword) {
        // Using BCrypt for password checking
        try {
            logger.debug("Checking password - Hash: " + hashedPassword);
            boolean result = org.mindrot.jbcrypt.BCrypt.checkpw(plainPassword, hashedPassword);
            logger.debug("Password check result: " + result);
            return result;
        } catch (Exception e) {
            logger.error("Error checking password: " + e.getMessage(), e);
            return false;
        }
    }

    public List<User> findActivePayrollUsers() {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.attivo = true AND u.ruolo <> :adminRole ORDER BY u.cognome ASC, u.nome ASC",
                            User.class)
                    .setParameter("adminRole", User.Ruolo.ADMIN)
                    .getResultList();
        } catch (Exception e) {
            logger.error("Error finding active payroll users", e);
            return java.util.Collections.emptyList();
        }
    }
}
