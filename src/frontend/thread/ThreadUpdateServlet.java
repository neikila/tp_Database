package frontend.thread;

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

        short status = 0;
        String message = "";

        long thread= 0;
        if (req.containsKey("thread")) {
            thread = (long) req.get("thread");
        } else {
            status = 2;
            message = "Wrong json";
        }

        String messageThread = null;
        if (req.containsKey("message")) {
            messageThread = (String)req.get("message");
        } else {
            status = 2;
            message = "Wrong json";
        }

        String slug = null;
        if (req.containsKey("slug")) {
            slug = (String)req.get("slug");
        } else {
            status = 2;
            message = "Wrong json";
        }

        int result = 0;

        String query;
        if (status == 0) {
            query = "update thread set " +
                    "message = '" + messageThread + "'," +
                    "slug = '" + slug + "'" +
                    " where id = " + thread + " ;";
            logger.info(LoggerHelper.query(), query);
            result = mySqlServer.executeUpdate(query);
            logger.info(LoggerHelper.resultUpdate(), result);
            if (result != 1) {
                status = 2;
                message = "No such thread";
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
        if (status != 0) {
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