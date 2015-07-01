package frontend.forum;

import helper.CommonHelper;
import helper.ErrorMessages;
import helper.LoggerHelper;
import mysql.MySqlConnect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static helper.CommonHelper.appendDateAndAscAndLimit;
import static helper.ErrorMessages.*;
import static helper.LoggerHelper.*;


public class ForumListPostsServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ForumListPostsServlet.class.getName());
    private MySqlConnect mySqlServer;

    public ForumListPostsServlet() {
        this.mySqlServer = new MySqlConnect();
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        mySqlServer.init();

        Map<String, String[]> paramMap = request.getParameterMap();
        String forum = paramMap.containsKey("forum") ? paramMap.get("forum")[0] : null;
        String asc = paramMap.containsKey("order") ? paramMap.get("order")[0] : null;
        String since = paramMap.containsKey("since") ? paramMap.get("since")[0] : null;
        String limit = paramMap.containsKey("limit") ? paramMap.get("limit")[0] : null;
        String[] related = paramMap.get("related");

        short status = ok;
        String message = "";
        if (forum == null) {
            status = wrongData;
            message = wrongJSONData();
        }

        StringBuilder query = new StringBuilder();
        ResultSet resultSet = null;
        Statement statement = mySqlServer.getStatement();
        int forumId = mySqlServer.getForumIdByShortName(forum);
        // TODO index forum: short_name, id || post: forum_id, date_of_creating
        if (status == ok) {
            query
                    .append("select id from post use index (forum_id__data) ")
                    .append("where forum_id = '")
                    .append(forumId)
                    .append("' ");
            appendDateAndAscAndLimit(query, since, asc, limit);
            resultSet = mySqlServer.executeSelect(query.toString(), statement);
        }
        try {
            createResponse(response, status, message, resultSet, related);
        } catch (SQLException e) {
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }

        mySqlServer.closeExecution(resultSet, statement);

        mySqlServer.close();
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, ResultSet resultSet, String[] related) throws IOException, SQLException {
        CommonHelper.setResponse(response);
        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();

        JSONArray listPosts= new JSONArray();

        if (status != ErrorMessages.ok || resultSet == null) {
            data.put("error", message);
            obj.put("response", data);
        } else {
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
                            status = wrongData;
                            message = wrongJSONData();
                    }
                }
            }
            if (status == ErrorMessages.ok) {
                mySqlServer.prepareStatementsForPostDetails();
                while (resultSet != null && resultSet.next()) {
                    listPosts.add(mySqlServer.getPostDetailsWithPrepareStatement(resultSet.getInt("id"), user, thread, forum));
                }
                mySqlServer.closePrepareStatementForPostDetails();
                obj.put("response", listPosts);
            } else {
                data.put("error", message);
                obj.put("response", data);
            }
        }
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}