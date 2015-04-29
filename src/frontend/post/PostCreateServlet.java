package frontend.post;

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
        short status = ErrorMessages.ok;
        String message = "";

        //TODO Сюда нужен еще один валидатор

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

        long parent_id = 0;
        try {
            if (req.containsKey("parent")) {
                parent_id = (long) req.get("parent");
            }
        } catch (Exception e) {
            logger.info(e);
            e.printStackTrace();
            status = 3;
            message = ErrorMessages.wrongJSONData();
        }

        String shortName = (String)req.get("forum");
        String email = (String)req.get("user");
        String messagePost = (String)req.get("message");
        int thread =  (int)(long)req.get("thread");
        String date = (String)req.get("date");
        int result;

        String query;
        String matPath = "";
        if (parent_id != 0) {
            ResultSet resultSet1 = null;
            Statement statement1 = mySqlServer.getStatement();
            query = "select parent from post where id = " + parent_id + ";";
            logger.info(LoggerHelper.query(), query);
            resultSet1 = mySqlServer.executeSelect(query, statement1);

            try {
                if(resultSet1.next()) {
                    String parent = resultSet1.getString("parent");
                    if (parent.equals("")) {
                        matPath = String.format("%03d", parent_id);
                    } else {
                        matPath = parent + String.format("_%03d", parent_id);
                    }
                    logger.info(matPath);
                } else {
                    status = ErrorMessages.noRequestedObject;
                    message = ErrorMessages.noParent();
                    logger.info(message);
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
            int authorId = mySqlServer.getUserIdByEmail(email);
            int forumId = mySqlServer.getForumIdByShortName(shortName);
            // TODO Проверка налчичия такого форума
            if (forumId > 0 && authorId > 0) {
                String forumName = mySqlServer.getForumNameById(forumId);
                query =
                        "insert into post set " +
                                "thread = " + thread + ", " +
                                "message = '" + messagePost + "', " +
                                "author_id = " + authorId + ", " +
                                "date_of_creating = '" + date + "', " +
                                "forum_id = " + forumId + ", " +
                                "parent = '" + matPath + "', " +
                                "isApproved = " + (isApproved ? 1 : 0) + ", " +
                                "isHighlighted = " + (isHighlighted ? 1 : 0) + ", " +
                                "isEdited = " + (isEdited ? 1 : 0) + ", " +
                                "isSpam = " + (isSpam ? 1 : 0) + ", " +
                                "isDeleted = " + (isDeleted ? 1 : 0) + ";";
                logger.info(LoggerHelper.query(), query);
                result = mySqlServer.executeUpdate(query);
                logger.info(LoggerHelper.resultUpdate(), result);

                query = "select post.id, post.date_of_creating as date, '" + forumName + "' as forum, isApproved, isDeleted, isEdited, isSpam, isHighlighted, message, parent, thread, '" + email + "' as user " +
                        "from post " +
                        "where author_id = " + authorId + " and " +
                        "post.date_of_creating = '" + date + "';";
                logger.info(LoggerHelper.query(), query);
                resultSet = mySqlServer.executeSelect(query, statement);
            } else {
                status = ErrorMessages.noRequestedObject;
                message = ErrorMessages.noForum();
                logger.info(LoggerHelper.noUserOrForum());
            }
        }
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
        if (status != ErrorMessages.ok) {
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
                status = ErrorMessages.noRequestedObject;
                data.put("error", "Error while PostCreate");
            }
        }
        obj.put("response", data);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}