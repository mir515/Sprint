package framework;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppListener implements ServletContextListener {

    public static Map<UrlMethod, MethodInfo> urlMethodMappings = new HashMap<>();
    public static RuntimeException initError = null;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[Framework] Demarrage de l'application...");

        String packageToScan = sce.getServletContext().getInitParameter("controllerPackage");
        if (packageToScan == null || packageToScan.isEmpty()) {
            packageToScan = "controlleur";
        }

        // Instanciation de notre scanner de package
        PackageScanner scanner = new PackageScanner();
        
        // On cherche toutes les classes qui ont l'annotation simple "Controller"
        Set<String> controllerClasses = scanner.findAnnotedClasses("Controller", packageToScan);

        try {
            for (String className : controllerClasses) {
                try {
                    Class<?> clazz = Class.forName(className);
                    System.out.println("[Framework] Controller trouve: " + className);

                    for (Method method : clazz.getMethods()) {
                        // On utilise bien l'annotation @Url que vous avez définie
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
                } catch (ClassNotFoundException e) {
                    System.err.println("[Framework] Impossible de charger la classe: " + className);
                    e.printStackTrace();
                }
            }
        } catch (RuntimeException e) {
            initError = e;
            System.err.println("[Framework] ERREUR d'initialisation: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[Framework] Total mappings charges: " + urlMethodMappings.size());
        System.out.println("[Framework] Application demarree avec succes !");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[Framework] Arret de l'application...");
    }
}