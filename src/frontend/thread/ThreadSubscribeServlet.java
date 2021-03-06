package frontend.thread;

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

import static helper.ErrorMessages.*;
import static helper.LoggerHelper.*;
import static main.JsonInterpreterFromRequest.getJSONFromRequest;

public class ThreadSubscribeServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ThreadSubscribeServlet.class.getName());
    private MySqlConnect mySqlServer;

    public ThreadSubscribeServlet() {
        this.mySqlServer = new MySqlConnect();
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        mySqlServer.init();

        JSONObject req = getJSONFromRequest(request, "PostCreate");

        short status = ok;
        String message = "";

        long threadId = 0;
        String email = (String) req.get("user");

        if (req.containsKey("thread") && email != null) {
            threadId = (long) req.get("thread");
        } else {
            status = notValidRequest;
            message = wrongParamsOfRequest();
        }


        int result = 0;
        String query;

        int userId = mySqlServer.getUserIdByEmail(email);
        if (userId < 1) {
            status = noRequestedObject;
            message = noUser();
        }

        if (status == ok) {
            query = "insert into subscribtion set user_id = " + userId + " , thread_id = " + threadId + ";";
            result = mySqlServer.executeUpdate(query);
            logger.info(resultUpdate(), result);
            if (result == 0) {
                status = noRequestedObject;
                message = noThread();
            }
        }
        try {
            createResponse(response, status, message, threadId, email);
        } catch (SQLException e) {
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        mySqlServer.close();
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, long threadId, String email) throws IOException, SQLException {
        CommonHelper.setResponse(response);
        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        if (status != ErrorMessages.ok) {
            data.put("error", message);
        } else {
            data.put("thread", threadId);
            data.put("user", email);
        }
        obj.put("response", data);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}