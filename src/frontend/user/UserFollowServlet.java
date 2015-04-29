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

import static main.JsonInterpreterFromRequest.getJSONFromRequest;

public class UserFollowServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(UserFollowServlet.class.getName());

    private MySqlConnect mySqlServer;

    public UserFollowServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());
        JSONObject req = getJSONFromRequest(request, "UserFollow");

        String email = (String) req.get("follower");

        String query = "insert into follow set " +
                "followee_id = (select id from users where email = '" + req.get("followee") + "'), " +
                "follower_id = (select id from users where email = '" + email + "');";
        logger.info(LoggerHelper.query(), query);
        int result = mySqlServer.executeUpdate(query);
        logger.info(LoggerHelper.resultUpdate(), result);
        short status = ErrorMessages.ok;
        String message = "";

        ResultSet resultSet;
        Statement statement = mySqlServer.getStatement();

        query = "select * from users where email = '" + email + "';";
        logger.info(LoggerHelper.query(), query);
        resultSet = mySqlServer.executeSelect(query, statement);

        ResultSet followee = null, follower = null, subscription = null;
        Statement statement_followee = mySqlServer.getStatement(), statement_follower = mySqlServer.getStatement(), statement_subscription = mySqlServer.getStatement();
        try {
            // TODO check документацию возможно тут стоит заменить на details
            if(resultSet.next()) {
                query = "select email from users join follow on followee_id = id where follower_id = " + resultSet.getInt("id") + ";";
                logger.info(LoggerHelper.query(), query);
                followee = mySqlServer.executeSelect(query, statement_followee);
                query = "select email from users join follow on follower_id = id where followee_id = " + resultSet.getInt("id") + ";";
                logger.info(LoggerHelper.query(), query);
                follower = mySqlServer.executeSelect(query, statement_follower);
                query = "select thread_id from users join subscribtion on user_id = id where id = " + resultSet.getInt("id") + ";";
                logger.info(LoggerHelper.query(), query);
                subscription = mySqlServer.executeSelect(query, statement_subscription );
            }
            else {
                resultSet = null;
                status = ErrorMessages.noRequestedObject;
                message = ErrorMessages.noUser();
                logger.info(message);
            }
        } catch (SQLException e) {
            logger.error("User Details error");
        }
        try {
            createResponse(response, status, message, resultSet, followee, follower, subscription);
        } catch (SQLException e) {
            logger.error(LoggerHelper.responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        mySqlServer.closeExecution(resultSet, statement);
        mySqlServer.closeExecution(followee, statement_followee);
        mySqlServer.closeExecution(follower, statement_follower);
        mySqlServer.closeExecution(subscription, statement_subscription);
        logger.info(LoggerHelper.finish());
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