package framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RoleRequired {
    String[] value();

    String loginView() default "/login";

    String message() default "Veuillez vous connecter pour accéder à cette page.";
}
