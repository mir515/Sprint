package framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    private static Set<String> uris = new HashSet<>();

    @Override
    public void init() throws ServletException {
        System.out.println("[Framework] FrontControllerServlet initialise");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Si une erreur est survenue durant le scan à l'initialisation, on bloque tout ici
        if (AppListener.initError != null) {
            throw new ServletException(AppListener.initError);
        }

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String chemin = uri.substring(contextPath.length());
        String httpMethod = req.getMethod();

        uris.add(chemin);

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        out.println("<html><body>");
        out.println("<h1>URL: " + chemin + "</h1>");
        out.println("<p>Methode HTTP: " + httpMethod + "</p>");

        UrlMethod key = new UrlMethod(chemin, httpMethod);

        if (AppListener.urlMethodMappings.containsKey(key)) {
            MethodInfo info = AppListener.urlMethodMappings.get(key);
            String simpleName = info.className.substring(info.className.lastIndexOf('.') + 1);
            out.println("<p>Classe: " + simpleName + "</p>");
            out.println("<p>Methode: " + info.methodName + "</p>");

            try {
                Class<?> clazz = Class.forName(info.className);
                Object instance = clazz.getDeclaredConstructor().newInstance();

                Method method = null;
                try {
                    method = clazz.getMethod(info.methodName, HttpServletRequest.class, HttpServletResponse.class);
                    method.invoke(instance, req, resp);
                } catch (NoSuchMethodException e) {
                    method = clazz.getMethod(info.methodName);
                    method.invoke(instance);
                }

            } catch (Exception e) {
                throw new ServletException(e);
            }
        } else {
            out.println("<p style='color:red;'>404 - Aucun mapping pour: " + httpMethod + " " + chemin + "</p>");
            out.println("<h2>Mappings disponibles</h2><ul>");
            for (Map.Entry<UrlMethod, MethodInfo> entry : AppListener.urlMethodMappings.entrySet()) {
                String simpleName = entry.getValue().className
                        .substring(entry.getValue().className.lastIndexOf('.') + 1);
                out.println(
                        "<li><strong>" + entry.getKey().getMethod() + "</strong> " + entry.getKey().getUrl() + " -> " +
                                simpleName + "." + entry.getValue().methodName + "</li>");
            }
            out.println("</ul>");
        }

        out.println("<hr>");
        out.println("<h2>URLs visitees (" + uris.size() + ")</h2><ul>");
        for (String lien : uris) {
            out.println("<li>" + lien + "</li>");
        }
        out.println("</ul>");

        out.println("</body></html>");
    }
}