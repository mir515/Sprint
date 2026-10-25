package framework;

import jakarta.servlet.http.HttpSession;

/** A small framework wrapper around the container-managed HTTP session. */
public final class MySession {

    private static final String CONNECTED_USER_ATTRIBUTE = "CONNECTED_USER";

    private final HttpSession httpSession;

    public MySession(HttpSession httpSession) {
        if (httpSession == null) {
            throw new IllegalArgumentException("La session HTTP ne peut pas être null");
        }
        this.httpSession = httpSession;
    }

    public void setAttribute(String name, Object value) {
        httpSession.setAttribute(name, value);
    }

    public Object getAttribute(String name) {
        return httpSession.getAttribute(name);
    }

    public <T> T getAttribute(String name, Class<T> type) {
        Object value = getAttribute(name);
        return value == null ? null : type.cast(value);
    }

    /** Stores the application's authenticated user without depending on its User class. */
    public void setUser(Object user) {
        httpSession.setAttribute(CONNECTED_USER_ATTRIBUTE, user);
    }

    /** Reads the authenticated user using the application's own User class. */
    public <T> T getUser(Class<T> userType) {
        return getAttribute(CONNECTED_USER_ATTRIBUTE, userType);
    }

    public boolean isAuthenticated() {
        return httpSession.getAttribute(CONNECTED_USER_ATTRIBUTE) != null;
    }

    public String getId() {
        return httpSession.getId();
    }

    public void invalidate() {
        httpSession.invalidate();
    }
}
