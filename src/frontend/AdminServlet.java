package frontend;


import templater.PageGenerator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AdminServlet extends HttpServlet {
    public AdminServlet() {

    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {

        response.setStatus(HttpServletResponse.SC_OK);

        String pageToReturn;

        Map<String, Object> pageVariables = new HashMap<>();
        pageToReturn = "adminPage.html";
        pageVariables.put("titleMessage", "Admin page");

        response.getWriter().println(PageGenerator.getPage(pageToReturn, pageVariables));
    }


    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
            if(action != null)
            {
                switch (action) {
                    case "Stop server":
                        System.exit(0);
                        break;
                }
            }
    }
}