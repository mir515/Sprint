package framework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public class SessionRegistryListener implements ServletContextListener,
        HttpSessionListener, HttpSessionIdListener {

    public static final String ACTIVE_SESSIONS_ATTRIBUTE = "framework.activeSessions";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().setAttribute(
                ACTIVE_SESSIONS_ATTRIBUTE,
                new ConcurrentHashMap<String, MySession>()
        );
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(ACTIVE_SESSIONS_ATTRIBUTE);
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        ServletContext context = event.getSession().getServletContext();
        MySession session = new MySession(event.getSession());
        
        sessions(context).put(session.getId(), session);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        ServletContext context = event.getSession().getServletContext();
        MySession session = new MySession(event.getSession());
        
        sessions(context).remove(session.getId());
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
        ServletContext context = event.getSession().getServletContext();
        Map<String, MySession> activeSessions = sessions(context);
        
        if (oldSessionId != null) {
            activeSessions.remove(oldSessionId);
        }
        
        MySession session = new MySession(event.getSession());
        activeSessions.put(session.getId(), session);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, MySession> getActiveSessions(ServletContext context) {
        Object value = context.getAttribute(ACTIVE_SESSIONS_ATTRIBUTE);
        if (!(value instanceof Map)) {
            throw new IllegalStateException("Le registre des sessions n'est pas initialisé");
        }
        return (Map<String, MySession>) value;
    }

   public static MySession getCurrentSession(HttpServletRequest request) {
    ServletContext context = request.getServletContext();
    Map<String, MySession> userSessions = getActiveSessions(context);

    HttpSession httpSession = request.getSession(true);
    String sessionId = httpSession.getId();

    MySession session = userSessions.get(sessionId);

    if (session == null) {
        session = new MySession(httpSession);
        userSessions.put(sessionId, session);
    }

    return session;
}

    private Map<String, MySession> sessions(ServletContext context) {
        return getActiveSessions(context);
    }
}