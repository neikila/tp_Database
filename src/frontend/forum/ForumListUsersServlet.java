package frontend.forum;

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

        String forum = request.getParameter("forum");
        String asc = request.getParameter("order");
        String since_id = request.getParameter("since");
        String limit = request.getParameter("limit");

        short status = 0;
        String message = "";

        String query;
        ResultSet resultSet;
        Statement statement = mySqlServer.getStatement();
        query = "select author_id from " +
                "(select distinct author_id from post where forum_id = " +
                "(select id from forum where short_name = '" + forum + "')) as a join users on a.author_id = users.id " +
                (since_id != null?("where users.id > '" + since_id + "' "):"") +
                "order by name " +
                (asc == null?("desc "):asc + " ") +
                (limit != null?("limit " + limit):"") +
                ";";
        logger.info(LoggerHelper.query(), query);

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
        JSONObject data = new JSONObject();

        JSONArray listUser = new JSONArray();

        if (status != 0 || resultSet == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            data.put("error", message);
            obj.put("response", data);
        } else {
            while (resultSet.next()) {
                listUser.add(mySqlServer.getUserDetail(resultSet.getInt("author_id")));
            }
            obj.put("response", listUser);
        }
        obj.put("code", data.containsKey("error")?1:0);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}