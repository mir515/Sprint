package framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import framework.annotation.Controller;
import framework.annotation.UrlMapping;
import framework.annotation.WebApi;

public class FrontServlet extends HttpServlet {

    private List<String> controllers = new ArrayList<>();
    private Map<String, VerbAction> mappings = new HashMap<>();
    private Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            PackageScanner scanner = new PackageScanner();
            String basePackage = "controllers"; 

            Set<String> controllerClasses = scanner.findAnnotedClasses("Controller", basePackage);

            for (String className : controllerClasses) {
                Class<?> clazz = Class.forName(className);
                controllers.add(clazz.getSimpleName());
                Object instance = clazz.getDeclaredConstructor().newInstance();

                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(UrlMapping.class)) {
                        String url = m.getAnnotation(UrlMapping.class).value();
                        mappings.put(url, new VerbAction(instance, m));
                    }
                }
            }
            
            System.out.println("[FrontServlet] Initialisation réussie. Routes enregistrées : " + mappings.keySet());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        String contexte = req.getContextPath();
        String path = uri.substring(contexte.length());

        if (mappings.containsKey(path)) {
            VerbAction verbAction = mappings.get(path);
            Method method = verbAction.getMethod();
            Object controller = verbAction.getControllerInstance();

            try {
                Object result = method.invoke(controller);

                if (method.isAnnotationPresent(WebApi.class)) {
                    resp.setContentType("application/json;charset=UTF-8");
                    String json = gson.toJson(result);
                    PrintWriter out = resp.getWriter();
                    out.print(json);
                    out.flush();
                } else {
                    if (result instanceof String) {
                        String viewPath = (String) result;
                        req.getRequestDispatcher(viewPath).forward(req, resp);
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                "Type de retour non valide pour le rendu de vue HTML/JSP");
                    }
                }

            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'exécution de la méthode " + method.getName(), e);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Aucune route associée à l'URL : " + path);
        }
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
}