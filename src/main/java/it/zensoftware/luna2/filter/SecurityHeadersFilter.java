package it.zensoftware.luna2.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SecurityHeadersFilter adds HTTP security headers to all responses.
 * Implements OWASP security best practices and CSP policies.
 * 
 * Headers added:
 * - X-Frame-Options: DENY (prevents clickjacking)
 * - X-Content-Type-Options: nosniff (prevents MIME type sniffing)
 * - Strict-Transport-Security (HSTS): Enforces HTTPS for 1 year
 * - Content-Security-Policy: Restricts resource loading
 * - X-XSS-Protection: Legacy XSS filter (deprecated but still useful)
 * - Referrer-Policy: Controls referrer information
 * - Permissions-Policy: Controls browser features
 * 
 * @author AI Security Hardening
 * @version 1.0
 */
public class SecurityHeadersFilter implements Filter {
    
    private static final int HSTS_MAX_AGE = 31536000; // 1 year in seconds
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Filter initialization - no special setup needed
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // 1. Clickjacking Protection: Prevent embedding in iframes
            httpResponse.setHeader("X-Frame-Options", "DENY");
            
            // 2. MIME Type Sniffing Prevention: Enforce declared content type
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            
            // 3. HSTS (HTTP Strict-Transport-Security): Force HTTPS for all future requests
            // max-age: 1 year (31536000 seconds)
            // includeSubDomains: Apply to all subdomains
            // preload: Allow inclusion in HSTS preload lists
            httpResponse.setHeader("Strict-Transport-Security", 
                "max-age=" + HSTS_MAX_AGE + "; includeSubDomains; preload");
            
            // 4. Content Security Policy: Restrict resource loading
            // default-src 'self': Load resources from same origin only
            // script-src 'self': Load scripts from same origin only (no inline scripts)
            // style-src 'self' 'unsafe-inline': Allow inline styles (required for some frameworks)
            // img-src 'self' data:: Allow images from same origin and data URIs
            // font-src 'self': Load fonts from same origin only
            httpResponse.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' https://cdn.jsdelivr.net; " +
                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                "img-src 'self' data:; " +
                "font-src 'self' https://cdn.jsdelivr.net; " +
                "form-action 'self'; " +
                "frame-ancestors 'none'");
            
            // 5. Legacy XSS Protection Header (deprecated, but still useful for older browsers)
            // 1; mode=block: Enable XSS filter and block the page if attack detected
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            
            // 6. Referrer Policy: No referrer information sent to external sites
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // 7. Permissions Policy (formerly Feature Policy): Disable unnecessary browser features
            httpResponse.setHeader("Permissions-Policy", 
                "accelerometer=(), camera=(), geolocation=(), gyroscope=(), " +
                "magnetometer=(), microphone=(), payment=(), usb=()");
            
            // 8. Disable browser caching for sensitive pages
            // Control: no-cache, no-store - Don't cache
            // Must-revalidate: Must revalidate with server before using cached copy
            // Pragma: no-cache - Legacy header for older HTTP/1.0 caches
            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");
        }
        
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        // Filter cleanup - no special cleanup needed
    }
}
