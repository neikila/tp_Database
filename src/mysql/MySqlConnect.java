package mysql;

/**
 * Created by neikila on 08.03.15.
 */

import com.sun.istack.internal.Nullable;
import helper.ErrorMessages;
import helper.LoggerHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class MySqlConnect {
    private static Logger logger = LogManager.getLogger(MySqlConnect.class.getName());

    private static Connection connection;

    public MySqlConnect() {
        try {
        DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver").newInstance());
        Properties properties=new Properties();
        properties.setProperty("user","admin");
        properties.setProperty("password","subd_project");
        properties.setProperty("useUnicode","true");
        properties.setProperty("characterEncoding","windows-1251");


        connection = null;
        String url = "jdbc:mysql://localhost:3306/SMDB";
            connection = DriverManager.getConnection(url, properties);
            logger.info(LoggerHelper.connection(), url);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Nullable
    public Statement getStatement() {
        try {
            return connection.createStatement();
        } catch (Exception e) {
            return null;
        }
    }

    public ResultSet executeSelect(String query, Statement statement) {
        ResultSet resultSet = null;
        try {
            if (statement == null) {
                statement = connection.createStatement();
            }
            logger.info(LoggerHelper.query(), query);
            resultSet = statement.executeQuery(query);
        } catch (SQLException ex) {
            logger.error(ex);
        }
        return resultSet;
    }


    public int executeUpdate(String query){
        int result = 0;
        try {
            Statement statement = connection.createStatement();
            logger.info(LoggerHelper.query(), query);
            result = statement.executeUpdate(query);
            statement.close();
        } catch (SQLException ex) {
            logger.error(ex);
        }
        return result;
    }

    public void closeExecution(ResultSet resultSet, Statement statement){
        try {
            if (statement != null) {
                statement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException ex) {
            logger.error(ex);
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public String getParentPathByParentId(long parentId) {
        ResultSet resultSet1;
        Statement statement1 = getStatement();

        final String query = "select parent from post where id = " + parentId + ";";
        resultSet1 = executeSelect(query, statement1);

        String parent = null;
        try {
            if(resultSet1.next()) {
                parent = resultSet1.getString("parent");
            }
        } catch (SQLException e) {
            logger.error(e);
            e.printStackTrace();
        }
        closeExecution(resultSet1, statement1);
        return parent;
    }

    public String getForumNameById(int id) {
        ResultSet resultSet1 = null;
        Statement statement1 = getStatement();
        String query = "select name from forum where id = " + id + ";";
        resultSet1 = executeSelect(query, statement1);
        String forumName = null;
        try {
            if(resultSet1.next()) {
                forumName = resultSet1.getString("name");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }
        closeExecution(resultSet1, statement1);
        return forumName;
    }

    public int getUserIdByEmail(String email) {
        ResultSet preSet = null;
        Statement preStatement = getStatement();
        String query = "select id from users where email = '" + email + "';";
        preSet = executeSelect(query, preStatement);
        int userId = -1;

        try {
            if (preSet.next()) {
                userId = preSet.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        } finally {
            closeExecution(preSet, preStatement);
        }
        return userId;
    }

    public int getForumIdByShortName(String forum) {
        int forumId = -1;
        ResultSet preSet;
        Statement preStatement = getStatement();
        String query = "select id from forum where short_name = '" + forum + "';";
        preSet = executeSelect(query, preStatement);

        try {
            if (preSet.next()) {
                forumId = preSet.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        } finally {
            closeExecution(preSet, preStatement);
        }
        return forumId;
    }

    public JSONObject getUserDetail(String email) throws IOException, SQLException {
        String query;
        ResultSet resultSet;
        Statement statement = getStatement();

        ResultSet followee = null, follower = null, subscription = null;
        Statement statementFollowee = getStatement(), statementFollower = getStatement(), statementSubscription = getStatement();
        query = "select * from users where email = '" + email + "';";
        JSONObject data = new JSONObject();
        resultSet = executeSelect(query, statement);
        try {
            if(resultSet.next()) {
                data.put("isAnonymous", resultSet.getBoolean("isAnonymous"));
                data.put("email", resultSet.getString("email"));
                data.put("about", resultSet.getString("about").equals("")? null : resultSet.getString("about"));
                data.put("name", resultSet.getString("name").equals("")? null : resultSet.getString("name"));
                data.put("username", resultSet.getString("username").equals("")? null : resultSet.getString("username") );
                data.put("id", resultSet.getInt("id"));

                query = "select email from users join follow on followee_id = id where follower_id = " + resultSet.getInt("id") + ";";
                followee = executeSelect(query, statementFollowee);

                query = "select email from users join follow on follower_id = id where followee_id = " + resultSet.getInt("id") + ";";
                follower = executeSelect(query, statementFollower);

                query = "select thread_id from users join subscribtion on user_id = id where id = " + resultSet.getInt("id") + ";";
                subscription = executeSelect(query, statementSubscription);
            } else {
                data = null;
            }
        } catch (SQLException e) {
            logger.error(LoggerHelper.userDetailError());
            logger.error(e);
            e.printStackTrace();
        }
        if (data != null) {
            JSONArray toFollow = new JSONArray();
            JSONArray iAmFollowed = new JSONArray();
            JSONArray subscribed = new JSONArray();

            while(followee.next()) {
                toFollow.add(followee.getString("email"));
            }
            while(follower.next()) {
                iAmFollowed.add(follower.getString("email"));
            }
            while(subscription.next()) {
                subscribed.add(subscription.getInt("thread_id"));
            }
            data.put("following", toFollow);
            data.put("followers", iAmFollowed);
            data.put("subscriptions", subscribed);
            logger.info(LoggerHelper.userDetailJSON(), data.toJSONString());
        }
        closeExecution(resultSet, statement);
        closeExecution(followee, statementFollowee);
        closeExecution(follower, statementFollower);
        closeExecution(subscription, statementSubscription);
        return data;
    }


    public JSONObject getUserDetail(int id) throws IOException, SQLException {
        String query, message = "";
        int status = ErrorMessages.ok;
        ResultSet resultSet;
        Statement statement = getStatement();

        ResultSet followee = null, follower = null, subscription = null;
        Statement statement_followee = getStatement(), statement_follower = getStatement(), statement_subscription = getStatement();
        query = "select * from users where id = '" + id + "';";
        resultSet = executeSelect(query, statement);
        try {
            if(resultSet.next()) {
                query = "select email from users join follow on followee_id = id where follower_id = " + resultSet.getInt("id") + ";";
                followee = executeSelect(query, statement_followee);
                query = "select email from users join follow on follower_id = id where followee_id = " + resultSet.getInt("id") + ";";
                follower = executeSelect(query, statement_follower);
                query = "select thread_id from users join subscribtion on user_id = id where id = " + resultSet.getInt("id") + ";";
                subscription = executeSelect(query, statement_subscription);
            }
            else {
                resultSet = null;
                status = ErrorMessages.noRequestedObject;
                message = ErrorMessages.noUser();
                logger.error(message);
            }
        } catch (SQLException e) {
            logger.error(LoggerHelper.userDetailError());
        }
        JSONObject data = new JSONObject();
        JSONArray toFollow = new JSONArray();
        JSONArray iAmFollowed = new JSONArray();
        JSONArray subscribed = new JSONArray();

        if (status != ErrorMessages.ok || resultSet == null) {
            data.put("error", message);
        } else {
            data.put("isAnonymous", resultSet.getBoolean("isAnonymous"));
            data.put("email", resultSet.getString("email"));
            data.put("about", resultSet.getString("about").equals("") ? null : resultSet.getString("about"));
            data.put("name", resultSet.getString("name").equals("") ? null : resultSet.getString("name"));
            data.put("username", resultSet.getString("username").equals("")? null : resultSet.getString("username") );
            data.put("id", resultSet.getInt("id"));
            while(followee.next()) {
                toFollow.add(followee.getString("email"));
            }
            while(follower.next()) {
                iAmFollowed.add(follower.getString("email"));
            }
            while(subscription.next()) {
                subscribed.add(subscription.getInt("thread_id"));
            }
            data.put("following", toFollow);
            data.put("followers", iAmFollowed);
            data.put("subscriptions", subscribed);
        }
        closeExecution(resultSet, statement);
        closeExecution(followee, statement_followee);
        closeExecution(follower, statement_follower);
        closeExecution(subscription, statement_subscription);
        logger.info(LoggerHelper.userDetailJSON(), data.toString());
        return data;
    }



    public JSONObject getForumDetails(String short_name, String related) throws IOException, SQLException {
        ResultSet resultSet;
        Statement statement = getStatement();

        String query = "select forum.id, founder_id, forum.name, short_name, email from forum " +
                "join users on founder_id = users.id " +
                "where short_name = '" + short_name +"';";
        resultSet = executeSelect(query, statement);

        JSONObject data = new JSONObject();
        if (resultSet.next()) {
            if (related != null) {
                data.put("user", getUserDetail(resultSet.getInt("founder_id")));
            } else {
                data.put("user", resultSet.getString("email"));
            }
            data.put("name", resultSet.getString("name"));
            data.put("id", resultSet.getString("id"));
            data.put("short_name", resultSet.getString("short_name"));
            logger.info(LoggerHelper.forumDetailJSON(), data.toString());
        } else {
            data = null;
        }
        closeExecution(resultSet, statement);
        return data;
    }


    public JSONObject getThreadDetailsById(int id, boolean user, boolean forum) throws IOException, SQLException {

        ResultSet resultSetCount;
        Statement statementCount = getStatement();

        String query = "select count(*) as amount from post where thread = " + id + " and isDeleted = 0;";

        resultSetCount = executeSelect(query, statementCount);

        ResultSet resultSet = null;
        Statement statement = getStatement();


        query = "select thread.date_of_creating as date, forum.short_name as forum, short_name, thread.id, isClosed, isDeleted, message, slug, title, email as user, likes, dislikes " +
                "from thread " +
                "join users on founder_id = users.id " +
                "join forum on forum.id = forum_id " +
                "where thread.id = " + id  + ";";

        resultSet = executeSelect(query, statement);
        JSONObject data = new JSONObject();

        if (resultSet.next() && resultSetCount.next()) {
            data.put("date", resultSet.getString("date").substring(0, 19));
            data.put("dislikes", resultSet.getInt("dislikes"));
            if (forum) {
                data.put("forum", getForumDetails(resultSet.getString("short_name"), null));
            } else {
                data.put("forum", resultSet.getString("forum"));
            }
            data.put("id", resultSet.getInt("id"));
            data.put("isClosed", resultSet.getBoolean("isClosed"));
            data.put("isDeleted", resultSet.getBoolean("isDeleted"));
            data.put("likes", resultSet.getInt("likes"));
            data.put("message", resultSet.getString("message"));
            data.put("points", resultSet.getInt("likes") - resultSet.getInt("dislikes") );
            data.put("posts", resultSetCount.getInt("amount"));
            data.put("slug", resultSet.getString("slug"));
            data.put("title", resultSet.getString("title"));
            if (user) {
                data.put("user", getUserDetail(resultSet.getString("user")));
            } else {
                data.put("user", resultSet.getString("user"));
            }
        } else {
            String message = "There is no thread with such id!";
            data.put("error", message);
            logger.error(message);
        }

        closeExecution(resultSet, statement);
        logger.info(LoggerHelper.threadDetailJSON(), data.toString());
        return data;
    }


    public JSONObject getPostDetails(int id, boolean user, boolean thread, boolean forum) throws IOException, SQLException {

        ResultSet resultSet;
        Statement statement = getStatement();

        String query = "select post.id, post.date_of_creating as date, post.likes, post.dislikes, forum.short_name as forum, short_name, isAnonymous, isApproved, isDeleted, isEdited, isSpam, isHighlighted, message, parent, thread, email as user " +
                "from post " +
                "join forum on forum.id = forum_id " +
                "join users on users.id = author_id " +
                "where post.id = " + id + ";";

        resultSet = executeSelect(query, statement);

        JSONObject data = new JSONObject();
        if (resultSet.next()) {
            data.put("date", resultSet.getString("date").substring(0, 19));
            if (forum) {
                data.put("forum", getForumDetails(resultSet.getString("short_name"), null));
            } else {
                data.put("forum", resultSet.getString("forum"));
            }
            data.put("id", resultSet.getInt("id"));
            data.put("isApproved", resultSet.getBoolean("isApproved"));
            data.put("isHighlighted", resultSet.getInt("isHighlighted") == 1 ? true : false);
            data.put("isEdited", resultSet.getBoolean("isEdited"));
            data.put("isSpam", resultSet.getBoolean("isSpam"));
            data.put("isDeleted", resultSet.getBoolean("isDeleted"));
            data.put("message", resultSet.getString("message"));
            data.put("likes", resultSet.getInt("likes"));
            data.put("dislikes", resultSet.getInt("dislikes"));
            data.put("points", resultSet.getInt("likes") - resultSet.getInt("dislikes"));
            String temp = resultSet.getString("parent");
            if (temp.equals("")) {
                data.put("parent", null);
            }else {
                data.put("parent", Integer.parseInt(temp.substring(temp.length() - 3)));
            }
            if (thread) {
                data.put("thread", getThreadDetailsById(resultSet.getInt("thread"), false, false));
            } else {
                data.put("thread", resultSet.getInt("thread"));
            }

            if (user) {
                data.put("user", getUserDetail(resultSet.getString("user")));
            } else {
                data.put("user", resultSet.getString("user"));
            }
            logger.info(LoggerHelper.postDetailJSON(), data.toString());
        } else {
            data = null;
        }
        closeExecution(resultSet, statement);
        return data;
    }
}