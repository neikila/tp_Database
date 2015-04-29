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
import java.util.Map;

public class ThreadDetailsServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ThreadDetailsServlet.class.getName());
    private MySqlConnect mySqlServer;

    public ThreadDetailsServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());
        Map<String, String[]> paramMap = request.getParameterMap();

        String thread = request.getParameter("thread");
        String[] related = paramMap.get("related");
        short status = ErrorMessages.ok;

        try {
            createResponse(response, status, thread, related);
        } catch (SQLException e) {
            logger.error(LoggerHelper.responseCreating());
            logger.error(e);
            e.printStackTrace();
        }

        logger.info(LoggerHelper.finish());
    }


    private void createResponse(HttpServletResponse response, short status, String thread, String[] related) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);
        JSONObject obj = new JSONObject();
        boolean user = false;
        boolean forum = false;
        if (related != null) {
            for (String aRelated : related) {
                switch (aRelated) {
                    case "user":
                        user = true;
                        break;
                    case "forum":
                        forum = true;
                        break;
                    default:
                        status = 3;
                }
            }
        }
        JSONObject data;
        if (status == 0) {
           data = mySqlServer.getThreadDetailsById(Integer.parseInt(thread), user, forum);
        } else {
            data = new JSONObject();
            data.put("error", "Wrong related arg");
        }
        obj.put("response", data);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}