package framework;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FrontServlet extends HttpServlet {

    private Map<UrlMethod, MethodInfo> mappingUrls = new HashMap<>();

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
                            
                            String httpMethod = "GET"; 
                            try {
                                Method methodAttr = ann.annotationType().getMethod("method");
                                httpMethod = ((String) methodAttr.invoke(ann)).toUpperCase();
                            } catch (NoSuchMethodException e) {
                            }

                            UrlMethod key = new UrlMethod(urlPath, httpMethod);

                            if (mappingUrls.containsKey(key)) {
                                throw new RuntimeException("URL dupliquée détectée : [" + httpMethod + "] /" + urlPath);
                            }

                            mappingUrls.put(key, new MethodInfo(className, method.getName()));
                        }
                    }
                }
            } catch (RuntimeException e) {
                throw new ServletException(e.getMessage(), e);
            } catch (Exception e) {
                System.err.println("[FrontController] Erreur mapping classe " + className + " : " + e.getMessage());
            }
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        
        res.setContentType("text/html;charset=UTF-8");
        PrintWriter out = res.getWriter();

        String pathInfo = req.getPathInfo();
        String httpMethod = req.getMethod().toUpperCase();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            out.println("<html><body><h3>Bienvenue. Veuillez spécifier une action (ex: /andran)</h3></body></html>");
            return;
        }

        String urlDemandee = pathInfo.substring(1);
        UrlMethod key = new UrlMethod(urlDemandee, httpMethod);

        try {
            if (!mappingUrls.containsKey(key)) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.println("<html><body>");
                out.println("<h2>Erreur 404 - URL Introuvable</h2>");
                out.println("<p>Aucun mapping trouvé pour la méthode <strong>" + httpMethod + "</strong> sur l'URL <strong>/" + urlDemandee + "</strong></p>");
                out.println("<h3>Mappings disponibles :</h3><ul>");
                for (UrlMethod mapped : mappingUrls.keySet()) {
                    out.println("<li>[" + mapped.getMethod() + "] /" + mapped.getUrl() + "</li>");
                }
                out.println("</ul></body></html>");
                return;
            }

            MethodInfo info = mappingUrls.get(key);
            Class<?> clazz = Class.forName(info.className, true, Thread.currentThread().getContextClassLoader());
            Object instance = clazz.getDeclaredConstructor().newInstance();

            Method methodToInvoke = null;
            try {
                methodToInvoke = clazz.getMethod(info.methodName, HttpServletRequest.class, HttpServletResponse.class);
                methodToInvoke.invoke(instance, req, res);
            } catch (NoSuchMethodException e) {
                methodToInvoke = clazz.getMethod(info.methodName);
                methodToInvoke.invoke(instance);
                
                out.println("<html><body>");
                out.println("<h2>Exécution réussie !</h2>");
                out.println("<p><strong>URL appelée :</strong> /" + urlDemandee + "</p>");
                out.println("<p><strong>Méthode HTTP :</strong>" + httpMethod + "</span></p>");
                out.println("<p><strong>Fonction invoquée :</strong> <code>" + info.methodName + "()</code></p>");
                out.println("<p><strong>Classe :</strong> <code>" + info.className + "</code></p>");
                out.println("</body></html>");
            }

        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("<html><body><h2>Erreur 500 - Erreur d'exécution</h2>");
            out.println("<p>" + e.getCause() + "</p></body></html>");
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

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }
}