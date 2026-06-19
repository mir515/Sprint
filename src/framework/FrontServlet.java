package framework;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.Set;
import java.util.HashSet;

public class FrontServlet extends HttpServlet {

    // Champ d'INSTANCE (pas static) : chaque servlet a son propre état
    // static partagerait les classes entre toutes les instances → bug si plusieurs configs
    private Set<String> classes = new HashSet<>();

    @Override
    public void init() throws ServletException {
        String packageName = getInitParameter("packageName");
        String annotation  = getInitParameter("annotation");

        if (packageName == null || packageName.isEmpty()) {
            throw new ServletException("[FrontController] Le paramètre 'packageName' est manquant dans web.xml");
        }
        if (annotation == null || annotation.isEmpty()) {
            throw new ServletException("[FrontController] Le paramètre 'annotation' est manquant dans web.xml");
        }

        PackageScanner packageScanner = new PackageScanner();
        this.classes = packageScanner.findAnnotedClasses(annotation, packageName);

        System.out.println("[FrontController] " + this.classes.size()
                + " classe(s) trouvée(s) dans '" + packageName
                + "' avec @" + annotation);
    }

    public void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {

        res.setContentType("text/html;charset=UTF-8");
        PrintWriter out = res.getWriter();

        String packageName = getInitParameter("packageName");
        String annotation  = getInitParameter("annotation");

        out.println("<!DOCTYPE html><html><body>");
        out.println("<h2>Classes dans le package <code>" + packageName + "</code>"
                  + " annotées <code>@" + annotation + "</code></h2>");

        if (this.classes.isEmpty()) {
            out.println("<p><em>Aucune classe trouvée.</em></p>");
        } else {
            out.println("<ul>");
            for (String className : this.classes) {
                out.println("  <li>" + className + "</li>");
            }
            out.println("</ul>");
        }

        out.println("</body></html>");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        processRequest(req, res);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        processRequest(req, res);
    }
}
