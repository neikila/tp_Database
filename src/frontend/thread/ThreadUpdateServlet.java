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

public class ThreadUpdateServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ThreadUpdateServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ThreadUpdateServlet() {
        this.mySqlServer = new MySqlConnect();
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        mySqlServer.init();

        JSONObject req = getJSONFromRequest(request, "PostUpdate");

        short status = ok;
        String message = "";

        long thread = 0;
        String messageThread = (String) req.get("message");
        String slug = (String) req.get("slug");

        if (req.containsKey("thread") && slug != null && messageThread != null) {
            thread = (long) req.get("thread");
        } else {
            status = wrongData;
            message = wrongJSONData();
        }

        int result = 0;

        if (status == ok) {
            String query;
            query = "update thread set " +
                    "message = '" + messageThread + "', " +
                    "slug = '" + slug + "' " +
                    "where id = " + thread + ";";
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
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        mySqlServer.close();
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, long thread) throws IOException, SQLException {
        CommonHelper.setResponse(response);

        JSONObject obj = new JSONObject();
        JSONObject data = null;
        if (status == ErrorMessages.ok) {
            data = mySqlServer.getThreadDetailsById((int)thread, false, false);
            if (data == null) {
                status = ErrorMessages.noRequestedObject;
                message = ErrorMessages.noThread();
            }
        }
        obj.put("response", status == ErrorMessages.ok? data: message);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}