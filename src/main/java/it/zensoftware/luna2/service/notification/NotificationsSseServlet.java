package it.zensoftware.luna2.service.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * SSE endpoint per notifiche real-time via HTTP.
 */
public class NotificationsSseServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(NotificationsSseServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = extractUserId(request);
        if (userId == null || userId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "userId mancante");
            return;
        }

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(0);

        SsePushConnection connection = new SsePushConnection(asyncContext);
        PushNotificationService.getInstance().registerConnection(userId, connection);

        asyncContext.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                PushNotificationService.getInstance().unregisterConnection(userId, connection);
                logger.debug("SSE completed for user: " + userId);
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                PushNotificationService.getInstance().unregisterConnection(userId, connection);
                logger.debug("SSE timeout for user: " + userId);
            }

            @Override
            public void onError(AsyncEvent event) {
                PushNotificationService.getInstance().unregisterConnection(userId, connection);
                logger.debug("SSE error for user: " + userId, event.getThrowable());
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
            }
        });

        logger.info("SSE connection registered for user: " + userId);
    }

    private String extractUserId(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            return pathInfo.substring(1);
        }
        return request.getParameter("userId");
    }

    private static class SsePushConnection implements PushConnection {
        private final AsyncContext asyncContext;

        private SsePushConnection(AsyncContext asyncContext) {
            this.asyncContext = asyncContext;
        }

        @Override
        public void send(String message) throws Exception {
            HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
            PrintWriter writer = response.getWriter();
            writer.write("data: " + message + "\n\n");
            writer.flush();
        }

        @Override
        public void close() throws Exception {
            asyncContext.complete();
        }

        @Override
        public boolean isOpen() {
            return asyncContext.getRequest().isAsyncStarted();
        }
    }
}
