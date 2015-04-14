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
/*
    public JSONObject getUserDetail(int id) throws IOException, SQLException {
        String query, message = "";
        int status = 0;
        ResultSet resultSet;
        Statement statement = null;

        ResultSet followee = null, follower = null, subscription = null;
        Statement statement_followee = null, statement_follower = null, statement_subscription = null;
        query = "select * from users where id = '" + id + "';";
        resultSet = mySqlServer.executeSelect(query, statement);
        try {
            if(resultSet.next()) {
                query = "select email from users join follow on followee_id = id where follower_id = " + resultSet.getInt("id") + ";";
                followee = mySqlServer.executeSelect(query, statement_followee);
                query = "select email from users join follow on follower_id = id where followee_id = " + resultSet.getInt("id") + ";";
                follower = mySqlServer.executeSelect(query, statement_follower);
                query = "select thread_id from users join subscribtion on user_id = id where id = " + resultSet.getInt("id") + ";";
                subscription = mySqlServer.executeSelect(query, statement_follower);
            }
            else {
                resultSet = null;
                status = 1;
                message = "There is no user with such email!";
            }
        } catch (SQLException e) {
            System.out.println("User Details error");
        }
        JSONObject data = new JSONObject();
        JSONArray toFollow = new JSONArray();
        JSONArray iAmFollowed = new JSONArray();
        JSONArray subscribed = new JSONArray();

        if (status != 0 || resultSet == null) {
            data.put("error", message);
        } else {
            data.put("isAnonymous", resultSet.getBoolean("isAnonymous"));
            data.put("email", resultSet.getString("email"));
            data.put("about", resultSet.getString("about"));
            data.put("name", resultSet.getString("name"));
            data.put("username", resultSet.getString("username"));
            data.put("id", resultSet.getInt("id"));
            while(followee.next()) {
                toFollow.add(followee.getString("email"));
            }
            while(follower.next()) {
                iAmFollowed.add(follower.getString("email"));
            }
            while(subscription.next()) {
                subscribed.add(subscription.getString("thread_id"));
            }
            data.put("following", toFollow);
            data.put("followers", iAmFollowed);
            data.put("subscriptions", subscribed);
        }
        mySqlServer.closeExecution(resultSet, statement);
        mySqlServer.closeExecution(followee, statement_followee);
        mySqlServer.closeExecution(follower, statement_follower);
        mySqlServer.closeExecution(subscription, statement_subscription);
        return data;
    }

*/
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