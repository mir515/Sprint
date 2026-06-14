import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class FrontServlet extends HttpServlet {

    // Centralisation du traitement pour GET et POST
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        
        // Récupération de l'URI
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        // Affichage en console (print)
        System.out.println("[Framework] URI Appelée : " + requestURI);

        // Affichage à l'écran pour le client
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<body>");
            out.println("<p><strong>URI capturée :</strong> " + requestURI + "</p>");
            out.println("<p><strong>Context Path :</strong> " + contextPath + "</p>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
