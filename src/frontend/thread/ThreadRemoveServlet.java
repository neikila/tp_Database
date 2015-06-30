package frontend.thread;

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

public class ThreadRemoveServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ThreadRemoveServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ThreadRemoveServlet(MySqlConnect mySqlServer) {
        // this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        mySqlServer = new MySqlConnect(true);

        JSONObject req = getJSONFromRequest(request, "ThreadRemoveServlet");

        short status = ok;
        String message = "";

        long threadId = (long) req.get("thread");
        if (threadId == 0) {
            status = wrongData;
            message = wrongJSONData();
        }


        if (status == ok) {
            String query = "update thread set isDeleted = 1, amountOfPost = 0 where id = " + threadId + ";";
            int result = mySqlServer.executeUpdate(query);
            logger.info(resultUpdate(), result);

            if (result != 0) {
                query = "update post set isDeleted = 1 where thread = " + threadId + ";";
                result = mySqlServer.executeUpdate(query);
                logger.info(resultUpdate(), result);
            } else {
                status = noRequestedObject;
                message = noPost();
            }
        }
        try {
            createResponse(response, status, message, threadId);
        } catch (SQLException e) {
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        mySqlServer.close();
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, long threadId) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);

        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        if (status == ErrorMessages.ok) {
            data.put("thread", threadId);
        }
        obj.put("response", status == ErrorMessages.ok? data: message);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}