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

import static helper.CommonHelper.appendLimitAndAsc;
import static helper.ErrorMessages.ok;
import static helper.LoggerHelper.*;

public class ForumListUsersServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ForumListUsersServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ForumListUsersServlet(MySqlConnect mySqlServer) {
        // this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        mySqlServer = new MySqlConnect(true);
        logger.info(request.getParameterMap().toString());

        String forum = request.getParameter("forum");
        String asc = request.getParameter("order");
        String since_id = request.getParameter("since_id");
        String limit = request.getParameter("limit");

        short status = ok;
        String message = "";

        StringBuilder query = new StringBuilder();
        ResultSet resultSet;
        Statement statement = mySqlServer.getStatement();
        int forumId = mySqlServer.getForumIdByShortName(forum);
        // TODO index user: id, name || post: forum_id, author_id
        if (since_id != null) {
            query
                    .append("select distinct p.author_id from post p join users u on p.author_id = u.id where p.forum_id = ")
                    .append(forumId)
                    .append(" ")
                    .append("and p.author_id > ")
                    .append(since_id)
                    .append(" ")
                    .append("order by u.name ");
        } else {
            query
                    .append("select distinct author_id from post p where forum_id = ")
                    .append(forumId)
                    .append(" ")
                    .append("order by name ");
        }
        appendLimitAndAsc(query, limit, asc);

        resultSet = mySqlServer.executeSelect(query.toString(), statement);
        try {
            createResponse(response, status, message, resultSet);
        } catch (SQLException e) {
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }

        mySqlServer.closeExecution(resultSet, statement);

        mySqlServer.close();
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, ResultSet resultSet) throws IOException, SQLException {
        CommonHelper.setResponse(response);
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