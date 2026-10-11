package framework.annotation;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UrlMapping {
    String value();
    String method() default "GET"; // Ajout de l'attribut de méthode HTTP
}