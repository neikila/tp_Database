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
import static main.JsonInterpreterFromRequest.getJSONFromRequest;

public class UserUnfollowServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(UserUnfollowServlet.class.getName());

    private MySqlConnect mySqlServer;

    public UserUnfollowServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());

        JSONObject req = getJSONFromRequest(request, "UserFollow");

        String email = (String) req.get("follower");

        long followee_id = mySqlServer.getUserIdByEmail((String)req.get("followee"));
        long follower_id = mySqlServer.getUserIdByEmail(email);

        String query = "delete from follow where " +
                "followee_id = " + followee_id + " and " +
                "follower_id = " + follower_id + ";";
        int result = mySqlServer.executeUpdate(query);
        logger.info(resultUpdate(), result);
        short status = ok;
        String message = "";

        try {
            createResponse(response, status, message, email);
        } catch (SQLException e) {
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, String email) throws IOException, SQLException {
        CommonHelper.setResponse(response);
        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();

        if (status == ErrorMessages.ok) {
            data = mySqlServer.getUserDetail(email);
        }
        if (data == null) {
            status = ErrorMessages.noRequestedObject;
            message = ErrorMessages.noUser();
        }
        obj.put("response", status == ErrorMessages.ok? data: message);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}