package frontend.post;

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static main.JsonInterpreterFromRequest.getJSONFromRequest;

public class PostCreateServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(PostCreateServlet.class.getName());
    private MySqlConnect mySqlServer;

    public PostCreateServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());
        JSONObject req = getJSONFromRequest(request, "PostCreateServlet");

        boolean isDeleted = false;
        boolean isApproved = false;
        boolean isHighlighted = false;
        boolean isEdited = false;
        boolean isSpam = false;

        if (req.containsKey("isDeleted")) {
            isDeleted = (boolean) req.get("isDeleted");
        }
        if (req.containsKey("isApproved")) {
            isApproved = (boolean) req.get("isApproved");
        }
        if (req.containsKey("isHighlighted")) {
            isHighlighted = (boolean) req.get("isHighlighted");
        }
        if (req.containsKey("isEdited")) {
            isEdited = (boolean) req.get("isEdited");
        }
        if (req.containsKey("isSpam")) {
            isSpam = (boolean) req.get("isSpam");
        }

        int parent_id = 0;
        if (req.containsKey("parent")) {
            parent_id = (int)(long)req.get("parent");
        }


        String short_name = (String)req.get("forum");
        String email = (String)req.get("user");
        String messagePost = (String)req.get("message");
        int thread =  (int)(long)req.get("thread");
        String date = (String)req.get("date");
        short status = 0;
        String message = "";
        int result;
        ResultSet resultSet1 = null;
        Statement statement1 = mySqlServer.getStatement();

        String query;
        String mat_path = "";
        if (parent_id != 0) {
            query = "select parent from post where id = " + parent_id + ";";
            logger.info(LoggerHelper.query(), query);
            resultSet1 = mySqlServer.executeSelect(query, statement1);

            try {
                if(resultSet1.next()) {
                    mat_path = resultSet1.getString("parent") + String.format("_%03d", parent_id);
                } else {
                    status = 4;
                    message = "There is no a such parent!";
                }
            } catch (SQLException e) {
                logger.error(e);
                e.printStackTrace();
            }
            mySqlServer.closeExecution(resultSet1, statement1);
        }
        ResultSet resultSet = null;
        Statement statement = mySqlServer.getStatement();
        if (status == 0) {
            query =
                    "insert into post set " +
                            "thread = " + thread + ", " +
                            "message = '" + messagePost + "', " +
                            "author_id = (select id from users where email = '" + email + "'), " +
                            "date_of_creating = '" + date + "', " +
                            "forum_id = (select id from forum where short_name = '" + short_name + "'), " +      //Проверка налчичия такого форума
                            "parent = '" + mat_path + "', " +
                            "isApproved = " + (isApproved ? 1 : 0) + ", " +
                            "isHighlighted = " + (isHighlighted ? 1 : 0)+ ", " +
                            "isEdited = " + (isEdited ? 1 : 0) + ", " +
                            "isSpam = " + (isSpam ? 1 : 0) + ", " +
                            "isDeleted = " + (isDeleted ? 1 : 0) + ";";
            logger.info(LoggerHelper.query(), query);
            result = mySqlServer.executeUpdate(query);
            System.out.println("Strings affected: " + result);

            query = "select post.id, post.date_of_creating as date, forum.name as forum, isAnonymous, isApproved, isDeleted, isEdited, isSpam, isHighlighted, message, parent, thread, email as user " +
                    "from post " +
                    "join forum on forum.id = forum_id " +
                    "join users on users.id = author_id " +
                    "where email = '" + email + "' and " +
                    "post.date_of_creating = '" + date + "';";
            logger.info(LoggerHelper.query(), query);
            resultSet = mySqlServer.executeSelect(query, statement);
        }
        try {
            createResponse(response, status, message, resultSet);
        } catch (SQLException e) {
            logger.error(LoggerHelper.responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        mySqlServer.closeExecution(resultSet, statement);
        mySqlServer.closeExecution(resultSet1, statement1);
        logger.info(LoggerHelper.finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, ResultSet resultSet) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);

        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        if (status != 0) {
            data.put("error", message);
        } else {
            if (resultSet.next()) {
                data.put("date", resultSet.getString("date").substring(0, 19));
                data.put("forum", resultSet.getString("forum"));
                data.put("id", resultSet.getInt("id"));
                data.put("isApproved", resultSet.getBoolean("isApproved"));
                data.put("isHighlighted", resultSet.getBoolean("isHighlighted"));
                data.put("isEdited", resultSet.getBoolean("isEdited"));
                data.put("isSpam", resultSet.getBoolean("isSpam"));
                data.put("isDeleted", resultSet.getBoolean("isDeleted"));
                data.put("message", resultSet.getString("message"));
                String temp = resultSet.getString("parent");
                if (temp.equals("")) {
                    data.put("parent", null);
                }else {
                    data.put("parent", Integer.parseInt(temp.substring(temp.length() - 3)));
                }
                data.put("thread", resultSet.getInt("thread"));
                data.put("user", resultSet.getString("user"));

            } else {
                status = 4;
                data.put("error", "Huston we have some problems in PostCreate");
            }
        }
        obj.put("response", data);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}