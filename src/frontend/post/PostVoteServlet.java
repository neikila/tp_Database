package frontend.post;

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

public class PostVoteServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(PostVoteServlet.class.getName());
    private MySqlConnect mySqlServer;

    public PostVoteServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());
        JSONObject req = getJSONFromRequest(request, "PostCreate");

        short status = 0;
        String message = "";

        long postId = 0;
        long vote = 0;
        if (req.containsKey("post")) {
            postId = (long)req.get("post");
        } else {
            status = 2;
            message = "Wrong json";
        }
        if (req.containsKey("vote")) {
            vote = (long)req.get("vote");
            if (vote != 1 && vote != -1) {
                status = 3;
                message = "Wrong vote";
            }
        } else {
            status = 2;
            message = "Wrong json";
        }

        int result = 0;
        String query;

        if (status == 0) {
            String likes = vote > 0 ? "likes" : "dislikes";
            query = "update post set " + likes + " = " + likes + " + 1" + " where id = " + postId + ";";
            logger.info(LoggerHelper.query(), query);
            result = mySqlServer.executeUpdate(query);
            logger.info(LoggerHelper.resultUpdate(), result);
            if (result == 0) {
                status = 1;
                message = "No such post";
            }
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
        JSONObject data = new JSONObject();
        if (status != 0) {
            data.put("error", message);
        } else {
            data = mySqlServer.getPostDetails((int)postId, false, false, false);
        }
        obj.put("response", data);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}