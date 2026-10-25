package framework;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import framework.annotation.UrlMapping;
import framework.annotation.Authenticated;
import framework.annotation.RoleRequired;

@MultipartConfig
public class FrontServlet extends HttpServlet {

    private final Map<String, VerbAction> mappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            String basePackage = getServletConfig().getInitParameter("basePackage");

            if (basePackage == null || basePackage.trim().isEmpty()) {
                throw new ServletException("Le paramètre d'initialisation 'basePackage' est obligatoire dans le web.xml");
            }

            PackageScanner scanner = new PackageScanner();
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
    private void registerController(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(UrlMapping.class)) {
                String url = method.getAnnotation(UrlMapping.class).value();
                mappings.put(url, new VerbAction(controllerInstance, method));
            }
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.substring(contextPath.length());

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
            MySession userSession = SessionRegistryListener.getCurrentSession(req);
            req.setAttribute("framework.session", userSession);

            Authenticated authentication = method.getAnnotation(Authenticated.class);
            RoleRequired roleRequirement = method.getAnnotation(RoleRequired.class);
            if (!userSession.isAuthenticated() && (authentication != null || roleRequirement != null)) {
                String loginView;
                String message;

                if (roleRequirement != null) {
                    loginView = roleRequirement.loginView();
                    message = roleRequirement.message();
                } else {
                    loginView = authentication.loginView();
                    message = authentication.message();
                }

                forwardToLogin(req, resp, loginView, message);
                return;
            }

            if (roleRequirement != null && !hasRequiredRole(userSession, roleRequirement.value())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Votre rôle ne permet pas d'accéder à cette page.");
                return;
            }

            Object[] args = MethodMapper.resolveParameters(method, req);
            resolveFileUploadArguments(method, req, args);

            Object result = method.invoke(controller, args);

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

    private boolean hasRequiredRole(MySession session, String[] requiredRoles) {
        for (String requiredRole : requiredRoles) {
            if (session.hasRole(requiredRole)) {
                return true;
            }
        }
        return false;
    }

    private void forwardToLogin(HttpServletRequest req, HttpServletResponse resp,
            String loginView, String message) throws ServletException, IOException {
        req.setAttribute("error", message);
        req.getRequestDispatcher(loginView).forward(req, resp);
    }

    /** Builds FileUpload arguments and fields from multipart parts. */
    private void resolveFileUploadArguments(Method method, HttpServletRequest req, Object[] args)
            throws Exception {
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            String parameterName = parameter.getName();

            if (parameterType == FileUpload.class) {
                Part part = findFilePart(req, parameterName);
                args[i] = part == null ? null : FileUpload.fromPart(part);
            } else if (parameterType == FileUpload[].class) {
                args[i] = toFileUploads(findFileParts(req, parameterName));
            } else if (args[i] != null && shouldInspectFields(parameterType)) {
                resolveFileUploadFields(args[i], req);
            }
        }
    }

    /** Supports FileUpload fields inside the complex objects hydrated by MethodMapper. */
    private void resolveFileUploadFields(Object object, HttpServletRequest req) throws Exception {
        Class<?> objectType = object.getClass();
        for (Field field : objectType.getDeclaredFields()) {
            String partName = objectType.getSimpleName() + "." + field.getName();
            field.setAccessible(true);

            if (field.getType() == FileUpload.class) {
                Part part = findFilePart(req, partName);
                field.set(object, part == null ? null : FileUpload.fromPart(part));
            } else if (field.getType() == FileUpload[].class) {
                field.set(object, toFileUploads(findFileParts(req, partName)));
            }
        }
    }

    private FileUpload[] toFileUploads(List<Part> parts) throws IOException {
        FileUpload[] uploads = new FileUpload[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            uploads[i] = FileUpload.fromPart(parts.get(i));
        }
        return uploads;
    }

    private Part findFilePart(HttpServletRequest req, String partName) throws Exception {
        List<Part> parts = findFileParts(req, partName);
        if (parts.size() > 1) {
            throw new IllegalArgumentException("Plusieurs fichiers reçus pour le champ " + partName);
        }
        return parts.isEmpty() ? null : parts.get(0);
    }

    private List<Part> findFileParts(HttpServletRequest req, String partName) throws Exception {
        List<Part> parts = new ArrayList<>();
        boolean hasParameterName = partName != null && !partName.matches("arg\\d+");

        for (Part part : req.getParts()) {
            String submittedFileName = part.getSubmittedFileName();
            if (submittedFileName == null || submittedFileName.trim().isEmpty()) {
                continue;
            }
            if (!hasParameterName || partName.equals(part.getName())) {
                parts.add(part);
            }
        }
        return parts;
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || type == String.class || type == Integer.class
                || type == Double.class || type == Float.class || type == Long.class
                || type == Boolean.class;
    }

    private boolean shouldInspectFields(Class<?> type) {
        return !isSimpleType(type) && !type.isArray() && !type.isInterface()
                && !Collection.class.isAssignableFrom(type)
                && !Map.class.isAssignableFrom(type)
                && !HttpServletRequest.class.isAssignableFrom(type)
                && !Part.class.isAssignableFrom(type)
                && !type.getName().startsWith("java.");
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
