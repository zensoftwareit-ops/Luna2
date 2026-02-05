package it.zensoftware.luna2.interceptor;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import it.zensoftware.luna2.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Authentication Interceptor to check if user is logged in
 */
public class AuthenticationInterceptor extends AbstractInterceptor {
    
    private static final Logger logger = LogManager.getLogger(AuthenticationInterceptor.class);
    
    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        Map<String, Object> session = invocation.getInvocationContext().getSession();
        User user = (User) session.get("currentUser");
        
        if (user == null) {
            logger.warn("Unauthorized access attempt to: " + invocation.getAction().getClass().getName());
            return "login";
        }
        
        return invocation.invoke();
    }
}
