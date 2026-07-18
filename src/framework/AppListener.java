package framework;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppListener implements ServletContextListener {

    public static Map<UrlMethod, MethodInfo> urlMethodMappings = new HashMap<>();
    public static String viewPrefix = "";
    public static String viewSuffix = "";
    public static RuntimeException initError = null;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[Framework] Demarrage de l'application...");

        ServletContext context = sce.getServletContext();

        viewPrefix = context.getInitParameter("viewPrefix");
        if (viewPrefix == null)
            viewPrefix = "/WEB-INF/views/";

        viewSuffix = context.getInitParameter("viewSuffix");
        if (viewSuffix == null)
            viewSuffix = ".jsp";

        System.out.println("[Framework] View prefix: " + viewPrefix);
        System.out.println("[Framework] View suffix: " + viewSuffix);

        String packageToScan = context.getInitParameter("controllerPackage");
        if (packageToScan == null || packageToScan.isEmpty()) {
            packageToScan = "controlleur"; 
        }

        PackageScanner scanner = new PackageScanner();
        Set<String> allClasses = scanner.findAnnotedClasses("Controller", packageToScan);

        try {
            for (String className : allClasses) {
                try {
                    Class<?> clazz = Class.forName(className);

                    if (clazz.isAnnotationPresent(Controller.class)) {
                        System.out.println("[Framework] Controller: " + className);

                        for (Method method : clazz.getMethods()) {
                            // CORRECTION : Remplacement de UrlMapping par Url
                            if (method.isAnnotationPresent(Url.class)) {
                                Url rm = method.getAnnotation(Url.class);
                                String url = rm.value();
                                String httpMethod = rm.method();

                                if (httpMethod == null || httpMethod.isEmpty()) {
                                    httpMethod = "GET";
                                }

                                UrlMethod key = new UrlMethod(url, httpMethod);

                                if (urlMethodMappings.containsKey(key)) {
                                    MethodInfo existing = urlMethodMappings.get(key);
                                    throw new RuntimeException(
                                            "URL dupliquee: " + httpMethod + " " + url + " - " +
                                                    existing.className + "." + existing.methodName + " et " +
                                                    className + "." + method.getName());
                                }

                                MethodInfo info = new MethodInfo(className, method.getName());
                                urlMethodMappings.put(key, info);

                                System.out.println("[Framework] Mapping: " + httpMethod + " " + url + " -> " + className
                                        + "." + method.getName());
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (RuntimeException e) {
            initError = e;
            e.printStackTrace();
        }

        System.out.println("[Framework] Total mappings: " + urlMethodMappings.size());
        System.out.println("[Framework] Application demarree avec succes !");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[Framework] Arret de l'application...");
    }
}