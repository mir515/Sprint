package framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import framework.annotation.Controller;
import framework.annotation.UrlMapping;
import framework.annotation.WebApi;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {

    private Map<String, VerbAction> mappings = new HashMap<>();
    private Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            String basePackage = getServletConfig().getInitParameter("basePackage");
            if (basePackage == null || basePackage.isEmpty()) {
                basePackage = "controllers";
            }

            PackageScanner scanner = new PackageScanner();
            Set<String> controllerClasses = scanner.findAnnotedClasses("Controller", basePackage);

            for (String className : controllerClasses) {
                registerController(className);
            }

            System.out.println("[FrontServlet] Initialisation réussie. Routes enregistrées : " + mappings.keySet());
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du mapping dynamique avec PackageScanner", e);
        }
    }

    private void registerController(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(UrlMapping.class)) {
                    String url = method.getAnnotation(UrlMapping.class).value();
                    mappings.put(url, new VerbAction(controllerInstance, method));
                }
            }
        } catch (Exception e) {
            System.err.println("[FrontServlet] Impossible d'enregistrer le contrôleur : " + className + " (" + e.getMessage() + ")");
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.substring(contextPath.length());

        VerbAction verbAction = mappings.get(path);
        if (verbAction == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Aucune route associée à l'URL : " + path);
            return;
        }

        Method method = verbAction.getMethod();
        Object controller = verbAction.getControllerInstance();

        if (method == null || controller == null) {
            throw new ServletException("Action ou instance introuvable pour le chemin : " + path);
        }

        try {
            Object[] args = MethodMapper.resolveParameters(method, req);
            Object result = method.invoke(controller, args);

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
                } else if (result != null) {
                    resp.sendError(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Le type de retour de la méthode " + method.getName() + " doit être un String (chemin de la vue)");
                }
            }

        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'exécution de la méthode " + method.getName(), e);
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
