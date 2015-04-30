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

import static helper.ErrorMessages.noRequestedObject;
import static helper.ErrorMessages.noThread;
import static helper.LoggerHelper.resultUpdate;
import static main.JsonInterpreterFromRequest.getJSONFromRequest;

public class ThreadUpdateServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ThreadUpdateServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ThreadUpdateServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());

        JSONObject req = getJSONFromRequest(request, "PostUpdate");

        short status = ErrorMessages.ok;
        String message = "";

        long thread= 0;
        String messageThread = null;
        String slug = null;

        if (req.containsKey("thread") && req.containsKey("message") && req.containsKey("slug")) {
            thread = (long) req.get("thread");
            messageThread = (String)req.get("message");
            slug = (String)req.get("slug");
        } else {
            status = ErrorMessages.wrongData;
            message = ErrorMessages.wrongJSONData();
        }

        int result = 0;

        String query;
        if (status == ErrorMessages.ok) {
            query = "update thread set " +
                    "message = '" + messageThread + "'," +
                    "slug = '" + slug + "'" +
                    " where id = " + thread + " ;";
            result = mySqlServer.executeUpdate(query);
            logger.info(resultUpdate(), result);
            if (result != 1) {
                status = noRequestedObject;
                message = noThread();
            }
        }
        try {
            createResponse(response, status, message, thread);
        } catch (SQLException e) {
            logger.error(LoggerHelper.responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        logger.info(LoggerHelper.finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, long thread) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);

        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        if (status != ErrorMessages.ok) {
            data.put("error", message);
        } else {
            data = mySqlServer.getThreadDetailsById((int)thread, false, false);
        }
        obj.put("response", data);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}