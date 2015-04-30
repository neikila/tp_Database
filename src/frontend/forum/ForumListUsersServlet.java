package frontend.forum;

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

public class ForumListUsersServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ForumListUsersServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ForumListUsersServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());
        logger.info(request.getParameterMap().toString());

        String forum = request.getParameter("forum");
        String asc = request.getParameter("order");
        String since_id = request.getParameter("since_id");
        String limit = request.getParameter("limit");

        short status = ErrorMessages.ok;
        String message = "";

        String query;
        ResultSet resultSet;
        Statement statement = mySqlServer.getStatement();
        int forumId = mySqlServer.getForumIdByShortName(forum);
        // TODO index user: id, name || post: forum_id, author_id
        query = "select distinct p.author_id from post p " +
                "join users u on p.author_id = u.id " +
                "where p.forum_id = " + forumId + " " +
                (since_id != null?("and p.author_id > " + since_id + " "):"") +
                "order by u.name " +
                (asc == null?("desc "):asc + " ") +
                (limit != null?("limit " + limit):"") +
                ";";

        resultSet = mySqlServer.executeSelect(query, statement);
        try {
            createResponse(response, status, message, resultSet);
        } catch (SQLException e) {
            logger.error(LoggerHelper.responseCreating());
            logger.error(e);
            e.printStackTrace();
        }

        mySqlServer.closeExecution(resultSet, statement);

        logger.info(LoggerHelper.finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, ResultSet resultSet) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);
        JSONObject obj = new JSONObject();

        JSONArray listUser = new JSONArray();

        if (status != ErrorMessages.ok || resultSet == null) {
            obj.put("response", message);
        } else {
            while (resultSet.next()) {
                listUser.add(mySqlServer.getUserDetail(resultSet.getInt("author_id")));
            }
            obj.put("response", listUser);
        }
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}