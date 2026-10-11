package framework;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import framework.annotation.Controller;
import framework.annotation.UrlMapping;

public class FrontServlet extends HttpServlet {

    private Map<String, VerbAction> mappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // Récupère le package de base configuré dans le web.xml (ou utilise une valeur par défaut/dynamique)
            // Exemple : vous pouvez récupérer un init-param du web.xml, ou scanner un package racine si nécessaire.
            String basePackage = getServletConfig().getInitParameter("basePackage");
            
            if (basePackage == null || basePackage.isEmpty()) {
                throw new ServletException("Le paramètre d'initialisation 'basePackage' est obligatoire dans le web.xml");
            }

            PackageScanner scanner = new PackageScanner();
            // Utilise le PackageScanner pour trouver toutes les classes annotées @Controller
            Set<String> controllerClasses = scanner.findAnnotedClasses("Controller", basePackage);

            for (String className : controllerClasses) {
                registerController(className);
            }

        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du mapping dynamique avec PackageScanner", e);
        }
    }

    /**
     * Enregistre les routes d'une classe contrôleur donnée par son nom complet.
     */
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

        // Vérification de l'existence de la route
        VerbAction verbAction = mappings.get(path);
        if (verbAction == null) {
            throw new ServletException("Aucune méthode ou URL associée au chemin : " + path);
        }

        Method method = verbAction.getMethod();
        Object controller = verbAction.getControllerInstance();

        if (method == null || controller == null) {
            throw new ServletException("Action ou instance introuvable pour le chemin : " + path);
        }

        try {
            // Matching des paramètres HTTP avec les arguments de la méthode
            Object[] args = MethodMapper.resolveParameters(method, req);

            //Invocation dynamique
            System.out.println("--> Execution de la méthode : " + method.getName());

            Object result = method.invoke(controller, args);
            
            System.out.println("<-- Fin execution. Résultat retourné : " + result);

            // Redirection vers la vue
            if (result instanceof String) {
                String viewPath = (String) result;
                req.getRequestDispatcher(viewPath).forward(req, resp);
            } else if (result != null) {
                throw new ServletException("Le type de retour de la méthode " + method.getName() + " doit être un String (chemin de la vue)");
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