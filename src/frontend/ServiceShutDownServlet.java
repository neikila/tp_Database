package frontend;

import helper.MessageStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServiceShutDownServlet extends HttpServlet {
    private final Logger logger = LogManager.getLogger(ServiceShutDownServlet.class.getName());

    public ServiceShutDownServlet() {}

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        if (request.getParameter("ShutDown").equals("true")) {
            logger.error(MessageStorage.shutDown());
            System.exit(0);
        } else {
            super.doGet(request, response);
        }
    }
}