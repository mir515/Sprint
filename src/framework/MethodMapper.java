package framework;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

public class MethodMapper {

    /**
     * Reçoit la méthode à exécuter et la requête HTTP.
     * Construit le tableau d'objets (arguments) prêts pour method.invoke().
     */
    public static Object[] resolveParameters(Method method, HttpServletRequest req) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> targetType = param.getType();

            // FrontServlet reads multipart parts and fills these arguments.
            if (targetType == FileUpload.class || targetType == FileUpload[].class) {
                continue;
            }

            if (targetType == MySession.class) {
                args[i] = req.getAttribute("framework.session");
                continue;
            }

            if (HttpServletRequest.class.isAssignableFrom(targetType)) {
                args[i] = req;
                System.out.println("Injection directe de HttpServletRequest réussie !");
                continue;
            }

            // Nom du paramètre de la méthode Java
            String paramName = param.getName();
            System.out.println("Nom du paramètre lu par Reflection : " + paramName);

            // Gestion des Collections (ex: List)
            if (List.class.isAssignableFrom(targetType)) {
                String[] values = req.getParameterValues(paramName);
                if (values == null) {
                    args[i] = null;
                } else {
                    args[i] = Arrays.asList(values);
                }
                System.out.println("Valeur liste (Collection) extraite de la requête HTTP");
                continue;
            }

            // Gestion des Objets Imbriqués 
            if (!isSimpleType(targetType)) {
                // Instanciation de l'objet (ex: new Employe())
                Object complexObj = targetType.getDeclaredConstructor().newInstance();
                
                for (Field field : targetType.getDeclaredFields()) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    String rawValue = req.getParameter(fieldName);

                    if (rawValue != null && !rawValue.trim().isEmpty()) {
                        Object convertedValue = convertValue(rawValue, field.getType());
                        field.set(complexObj, convertedValue);
                    }
                }
                args[i] = complexObj;
                System.out.println("Objet imbriqué/complexe instancié et hydraté : " + targetType.getSimpleName());
                continue;
            }

            String rawValue = req.getParameter(paramName);
            System.out.println("Valeur extraite de la requête HTTP : " + rawValue);

            args[i] = convertValue(rawValue, targetType);
        }

        return args;
    }

    /**
     * Vérifie si le type est un type simple (Primitif, Wrapper ou String).
     */
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || type == String.class || 
               type == Integer.class || type == Double.class || 
               type == Float.class || type == Long.class || type == Boolean.class;
    }

    /**
     * Convertit une valeur String vers un type primitif / Object équivalent.
     */
    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            return getDefaultValue(targetType);
        }

        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
        } catch (NumberFormatException e) {
            return getDefaultValue(targetType);
        }

        return null;
    }

    /**
     * Retourne la valeur de repli (0 pour les nombres, false pour boolean, null pour String/Objets).
     */
    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == long.class) return 0L;
        if (type == boolean.class) return false;
        return null; // String et types Wrapper (Integer, Double...)
    }
}
