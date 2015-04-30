package frontend.forum;

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

import static helper.ErrorMessages.noForum;
import static helper.ErrorMessages.noRequestedObject;

public class ForumDetailsServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ForumDetailsServlet.class.getName());
    private MySqlConnect mySqlServer;

    public ForumDetailsServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());

        String forum = request.getParameter("forum");
        String related = request.getParameter("related");

        short status = ErrorMessages.ok;
        String message = "";

        if (related != null && !related.equals("user")) {
            status = ErrorMessages.wrongData;
            message = ErrorMessages.wrongJSONData();
        }

        try {
            createResponse(response, status, related, message, forum);
        } catch (SQLException e) {
            logger.error(LoggerHelper.responseCreating());
            logger.error(e);
            e.printStackTrace();
        }

        logger.info(LoggerHelper.finish());
    }

    private void createResponse(HttpServletResponse response, short status, String related, String message, String short_name) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);
        JSONObject obj = new JSONObject();
        JSONObject data;

        data = mySqlServer.getForumDetails(short_name, related);
        if (data == null) {
            status = noRequestedObject;
            message = noForum();
        }

        obj.put("response", status == ErrorMessages.ok ? data: message);
        obj.put("code", status);

        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}