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

import static main.JsonInterpreterFromRequest.getJSONFromRequest;

public class ThreadRestoreServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ThreadRestoreServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ThreadRestoreServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());

        JSONObject req = getJSONFromRequest(request, "PostCreate");

        short status = ErrorMessages.ok;
        String message = "";

        long threadId= 0;
        if (req.containsKey("thread")) {
            threadId = (long)req.get("thread");
        } else {
            status = 2;
            message = "Wrong json";
        }

        int result = 0;
        String query;

        if (status == 0) {
            query = "update thread set isDeleted = 0 where id = " + threadId + ";";
            logger.info(LoggerHelper.query(), query);
            result = mySqlServer.executeUpdate(query);
            logger.info(LoggerHelper.resultUpdate(), result);
            if (result == 0) {
                status = ErrorMessages.noRequestedObject;
                message = ErrorMessages.noPost();
            } else {
                query = "update post set isDeleted = 0 where thread = " + threadId + ";";
                logger.info(LoggerHelper.query(), query);
                result = mySqlServer.executeUpdate(query);
                logger.info(LoggerHelper.resultUpdate(), result);
            }
        }
        try {
            createResponse(response, status, message, threadId);
        } catch (SQLException e) {
            logger.error(LoggerHelper.responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        logger.info(LoggerHelper.finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, long threadId) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);

        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        if (status != ErrorMessages.ok) {
            data.put("error", message);
        } else {
            data.put("thread", threadId);
        }
        obj.put("response", data);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}