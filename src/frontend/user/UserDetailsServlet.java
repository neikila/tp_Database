package frontend.user;

import helper.CommonHelper;
import helper.ErrorMessages;
import helper.LoggerHelper;
import mysql.MySqlConnect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

import static helper.ErrorMessages.ok;
import static helper.LoggerHelper.*;

public class UserDetailsServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(UserDetailsServlet.class.getName());

    private MySqlConnect mySqlServer;

    public UserDetailsServlet() {
        this.mySqlServer = new MySqlConnect();
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        mySqlServer.init();
        String email = request.getParameter("user");

        short status = ok;

        try {
            createResponse(response, status, email);
        } catch (SQLException e) {
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }

        mySqlServer.close();
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String email) throws IOException, SQLException {
        CommonHelper.setResponse(response);
        JSONObject obj = new JSONObject();
        JSONObject data = mySqlServer.getUserDetail(email);

        String message = "";

        if (data == null) {
            status = ErrorMessages.noRequestedObject;
            message = ErrorMessages.noForum();
        }

        obj.put("response", status == ErrorMessages.ok ? data: message);
        obj.put("code", status);

        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}