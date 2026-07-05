package framework;

import jakarta.servlet.ServletException;
import java.util.Set;

public class UrlNotFoundException extends ServletException {
    public UrlNotFoundException(String urlDemandee, Set<String> urlsValides) {
        super("L'URL '" + urlDemandee + "' n'existe pas. " 
            + "Les seules URL disponibles sont : " + urlsValides.toString());
    }
}