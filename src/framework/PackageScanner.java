package framework;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.HashSet;
import java.net.URL;
import java.util.Enumeration;
import java.io.File;

public class PackageScanner {

    public Set<String> findAnnotedClasses(String annotation, String packageName) {
        Set<String> result = new HashSet<>();

        if (annotation == null || annotation.isEmpty()
                || packageName == null || packageName.isEmpty()) {
            System.err.println("[PackageScanner] annotation ou packageName null/vide.");
            return result;
        }

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            String packagePath = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if ("file".equals(resource.getProtocol())) {
                    File dir = new File(resource.getFile());
                    if (dir.isDirectory()) {
                        result.addAll(
                            findAnnotedClassesFromDir(annotation, packageName, dir, classLoader)
                        );
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[PackageScanner] Erreur : " + e.getMessage());
        }

        return result;
    }

    private Set<String> findAnnotedClassesFromDir(
            String annotation,
            String packageName,
            File dir,
            ClassLoader classLoader) {

        Set<String> annotedClasses = new HashSet<>();

        File[] files = dir.listFiles();
        if (files == null) return annotedClasses;

        for (File f : files) {

            if (!f.isFile() || !f.getName().endsWith(".class")) {
                continue;
            }

            String simpleClassName = f.getName().substring(0, f.getName().length() - 6);
            String fullClassName   = packageName + "." + simpleClassName;

            try {
                Class<?> clazz = Class.forName(fullClassName, true, classLoader);

                Package pkg = clazz.getPackage();
                if (pkg == null || !pkg.getName().equals(packageName)) {
                    continue;
                }

                for (Annotation ann : clazz.getAnnotations()) {
                    if (ann.annotationType().getSimpleName().equals(annotation)) {
                        annotedClasses.add(fullClassName);
                        break;
                    }
                }

            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                System.err.println("[PackageScanner] Impossible de charger : "
                        + fullClassName + " (" + e.getMessage() + ")");
            }
        }

        return annotedClasses;
    }
}