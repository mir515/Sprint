package framework;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FrontServlet extends HttpServlet {

    private Map<String, Method> mappingUrls = new HashMap<>();

    @Override
    public void init() throws ServletException {
        String packageName = getInitParameter("packageName");
        String annotation  = getInitParameter("annotation");

        if (packageName == null || packageName.isEmpty() || annotation == null || annotation.isEmpty()) {
            throw new ServletException("[FrontController] Paramètres 'packageName' ou 'annotation' manquants.");
        }

        PackageScanner packageScanner = new PackageScanner();
        Set<String> classNames = packageScanner.findAnnotedClasses(annotation, packageName);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, true, classLoader);
                
                for (Method method : clazz.getDeclaredMethods()) {
                    for (java.lang.annotation.Annotation ann : method.getAnnotations()) {
                        if (ann.annotationType().getSimpleName().equals("Url")) {
                            Method valueMethod = ann.annotationType().getMethod("value");
                            String urlPath = (String) valueMethod.invoke(ann);
                            
                            mappingUrls.put(urlPath, method);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[FrontController] Erreur mapping classe : " + className);
            }
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        
        res.setContentType("text/html;charset=UTF-8");
        PrintWriter out = res.getWriter();

        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            out.println("<html><body><h3>Bienvenue. Veuillez spécifier une action (ex: /andran)</h3></body></html>");
            return;
        }

        String urlDemandee = pathInfo.substring(1);

        try {
            if (!mappingUrls.containsKey(urlDemandee)) {
                throw new UrlNotFoundException(urlDemandee, mappingUrls.keySet());
            }

            Method methodeAssociee = mappingUrls.get(urlDemandee);
            
            out.println("<html><body>");
            out.println("<h2>[FrontServlet] URL Valide !</h2>");
            out.println("<p>Méthode associée : <strong>" + methodeAssociee.getName() + "()</strong></p>");
            out.println("</body></html>");

        } catch (UrlNotFoundException e) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND); 
            
            out.println("<html><body style='font-family: Arial, sans-serif; margin: 40px;'>");
            out.println("<h2 style='color: #cc0000;'>Erreur 404 - URL Introuvable</h2>");
            out.println("<p style='font-size: 16px;'>" + e.getMessage() + "</p>");
            out.println("</body></html>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
}