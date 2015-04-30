package frontend.post;

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

public class PostUpdateServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(PostUpdateServlet.class.getName());
    private MySqlConnect mySqlServer;

    public PostUpdateServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());
        JSONObject req = getJSONFromRequest(request, "PostUpdate");

        short status = ErrorMessages.ok;
        String message = "";

        String messagePost = (String) req.get("message");;
        long postId = (long) req.get("post");;

        if (postId == 0 || messagePost == null) {
            status = ErrorMessages.wrongData;
            message = ErrorMessages.wrongJSONData();
        }

        int result = 0;

        if (status == ErrorMessages.ok) {
            final String query = "update post set " +
                    "message = '" + messagePost + "' where " +
                    " id = " + postId + " ;";
            result = mySqlServer.executeUpdate(query);
            logger.info(LoggerHelper.resultUpdate(), result);
        }
        if (result != 1) {
            status = ErrorMessages.noRequestedObject;
            message = ErrorMessages.noPost();
        }
        try {
            createResponse(response, status, message, postId);
        } catch (SQLException e) {
            logger.error(LoggerHelper.responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        logger.info(LoggerHelper.finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, long postId) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);

        JSONObject obj = new JSONObject();
        JSONObject data;
        data = mySqlServer.getPostDetails((int)postId, false, false, false);
        if (data == null) {
            status = ErrorMessages.noRequestedObject;
            message = ErrorMessages.noPost();
        }
        if (status == ErrorMessages.ok) {
            obj.put("response", data);
        } else {
            obj.put("error", message);
        }
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}