package framework;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.HashSet;
import java.net.URL;
import java.util.Enumeration;
import java.io.File;

public class PackageScanner {

    /**
     * Trouve toutes les classes du package EXACT `packageName`
     * qui portent l'annotation de nom simple `annotation` (ex: "Controller").
     */
    public Set<String> findAnnotedClasses(String annotation, String packageName) {
        Set<String> result = new HashSet<>();

        if (annotation == null || annotation.isEmpty()
                || packageName == null || packageName.isEmpty()) {
            System.err.println("[PackageScanner] annotation ou packageName null/vide.");
            return result;
        }

        try {
            // Le ClassLoader du thread courant voit les classes de l'app web (WEB-INF/classes)
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            String packagePath = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                // On ne traite que les URLs "file://" (WEB-INF/classes/)
                // Les URLs "jar://" correspondent aux classes du JAR lui-même → on les ignore
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

    /**
     * Parcourt UNIQUEMENT les .class directement dans `dir` (pas de récursion),
     * car un sous-répertoire = un sous-package différent du packageName demandé.
     *
     * Pour chaque .class trouvé :
     *   1. On reconstruit le nom complet : packageName + "." + NomSimple
     *   2. On charge la classe avec le ClassLoader de l'app web
     *   3. On vérifie que le package de la classe est EXACTEMENT packageName
     *   4. On vérifie que la classe porte l'annotation cherchée (niveau classe uniquement)
     */
    private Set<String> findAnnotedClassesFromDir(
            String annotation,
            String packageName,
            File dir,
            ClassLoader classLoader) {

        Set<String> annotedClasses = new HashSet<>();

        File[] files = dir.listFiles();
        if (files == null) return annotedClasses;

        for (File f : files) {

            // On ignore les sous-répertoires : ils correspondent à des sous-packages
            // (ex: com/example/sub/) dont le nom ne correspond pas au packageName demandé
            if (!f.isFile() || !f.getName().endsWith(".class")) {
                continue;
            }

            // Reconstruction du nom complet : "com.example.MyClass"
            String simpleClassName = f.getName().substring(0, f.getName().length() - 6);
            String fullClassName   = packageName + "." + simpleClassName;

            try {
                // On utilise le ClassLoader du thread (= classloader de l'app web)
                // pour trouver les classes dans WEB-INF/classes, pas seulement dans le JAR
                Class<?> clazz = Class.forName(fullClassName, true, classLoader);

                // Double-vérification : le package déclaré de la classe doit être EXACTEMENT
                // celui demandé (protège contre des cas limites de chargement croisé)
                Package pkg = clazz.getPackage();
                if (pkg == null || !pkg.getName().equals(packageName)) {
                    continue;
                }

                // Vérification de l'annotation au niveau CLASSE uniquement
                // On compare par nom simple (ex: "Controller") pour éviter de forcer
                // l'import complet dans web.xml
                for (Annotation ann : clazz.getAnnotations()) {
                    if (ann.annotationType().getSimpleName().equals(annotation)) {
                        annotedClasses.add(fullClassName);
                        break; // inutile de tester les autres annotations de cette classe
                    }
                }

            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // Classe impossible à charger (dépendance manquante, etc.) → on ignore
                System.err.println("[PackageScanner] Impossible de charger : "
                        + fullClassName + " (" + e.getMessage() + ")");
            }
        }

        return annotedClasses;
    }
}
