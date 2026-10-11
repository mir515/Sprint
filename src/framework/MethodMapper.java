package framework;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import jakarta.servlet.http.HttpServletRequest;

public class MethodMapper {

    /**
     * Reçoit la méthode à exécuter et la requête HTTP.
     * Construit le tableau d'objets (arguments) prêts pour method.invoke().
     */
    public static Object[] resolveParameters(Method method, HttpServletRequest req) throws Exception {
        Parameter[] parameters = method.getParameters();

        for (Parameter param : method.getParameters()) {
            System.out.println("Nom du paramètre lu par Reflection : " + param.getName());
            System.out.println("Valeur extraite de la requête HTTP : " + req.getParameter(param.getName()));
        }
        
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            
            // Nom du paramètre de la méthode Java (ex: "id", "nom")
            String paramName = param.getName();
            
            // Valeur envoyée par la vue (GET ou POST)
            String rawValue = req.getParameter(paramName);

            // Conversion de la valeur String vers le type exact de la méthode
            args[i] = convertValue(rawValue, param.getType());
        }

        return args;
    }

    /**
     * Convertit une valeur String vers un type primitif / Object équivalent.
     * Si la valeur est absente ou vide, renvoie la valeur par défaut (0, null, false...).
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
            // Si la conversion échoue (ex: "abc" vers int), on retourne la valeur par défaut
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