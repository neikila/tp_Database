package frontend.user;

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

import static helper.ErrorMessages.ok;
import static helper.LoggerHelper.*;
import static main.JsonInterpreterFromRequest.getJSONFromRequest;

public class UserUnfollowServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(UserUnfollowServlet.class.getName());

    private MySqlConnect mySqlServer;

    public UserUnfollowServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());

        JSONObject req = getJSONFromRequest(request, "UserFollow");

        String email = (String) req.get("follower");

        String query = "delete from follow where " +
                "followee_id = (select id from users where email = '" + req.get("followee") + "') and " +
                "follower_id = (select id from users where email = '" + email + "');";
        int result = mySqlServer.executeUpdate(query);
        logger.info(resultUpdate(), result);
        short status = ok;
        String message = "";

        ResultSet resultSet;
        Statement statement = mySqlServer.getStatement();

        query = "select * from users where email = '" + email + "';";
        resultSet = mySqlServer.executeSelect(query, statement);

        ResultSet followee = null, follower = null, subscription = null;
        Statement statement_followee = mySqlServer.getStatement(), statement_follower = mySqlServer.getStatement(), statement_subscription = mySqlServer.getStatement();
        try {
            if (resultSet.next()) {
                query = "select email from users join follow on followee_id = id where follower_id = " + resultSet.getInt("id") + ";";
                followee = mySqlServer.executeSelect(query, statement_followee);
                query = "select email from users join follow on follower_id = id where followee_id = " + resultSet.getInt("id") + ";";
                follower = mySqlServer.executeSelect(query, statement_follower);
                query = "select thread_id from users join subscribtion on user_id = id where id = " + resultSet.getInt("id") + ";";
                subscription = mySqlServer.executeSelect(query, statement_subscription);
            } else {
                resultSet = null;
                status = 1;
                message = "There is no user with such email!";
            }
        } catch (SQLException e) {
            logger.error("User Details error");
        }
        try {
            createResponse(response, status, message, resultSet, followee, follower, subscription);
        } catch (SQLException e) {
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        mySqlServer.closeExecution(resultSet, statement);
        mySqlServer.closeExecution(followee, statement_followee);
        mySqlServer.closeExecution(follower, statement_follower);
        mySqlServer.closeExecution(subscription, statement_subscription);
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, ResultSet resultSet, ResultSet followee, ResultSet follower, ResultSet subscription) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);
        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        JSONArray toFollow = new JSONArray();
        JSONArray iAmFollowed = new JSONArray();
        JSONArray subscribed = new JSONArray();

        if (status != ErrorMessages.ok || resultSet == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
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
        obj.put("response", data);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}