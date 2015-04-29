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
import java.util.Map;

public class PostDetailsServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(PostDetailsServlet.class.getName());

    private MySqlConnect mySqlServer;

    public PostDetailsServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());
        short status = ErrorMessages.ok;
        Map<String, String[]> paramMap = request.getParameterMap();
        int id = Integer.parseInt(paramMap.containsKey("post") ? paramMap.get("post")[0] : "0");
        if (id == 0) {
            status = 3;
        }
        String[] related = paramMap.get("related");
        try {
            createResponse(response, status, id, related);
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(LoggerHelper.responseCreating());
            logger.error(e);
            e.printStackTrace();
        }

        logger.info(LoggerHelper.finish());
    }

    private void createResponse(HttpServletResponse response, short status, int id, String[] related) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);
        JSONObject obj = new JSONObject();
        boolean user = false;
        boolean forum = false;
        boolean thread = false;
        if (related != null) {
            for (String aRelated : related) {
                switch (aRelated) {
                    case "user":
                        user = true;
                        break;
                    case "forum":
                        forum = true;
                        break;
                    case "thread":
                        thread = true;
                        break;
                    default:
                        status = 3;
                }
            }
        }
        JSONObject data;
        if (status == 0) {
            data = mySqlServer.getPostDetails(id, user, thread, forum);
            obj.put("code", (data.containsKey("error")?1:0)==1?1:status);
            obj.put("response", data);
        } else {
            obj.put("code", status);
            obj.put("error", "Wrong data");
        }
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}