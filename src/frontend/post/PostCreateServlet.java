package frontend.post;

import helper.CommonHelper;
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

import static helper.ErrorMessages.*;
import static helper.LoggerHelper.*;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static main.JsonInterpreterFromRequest.getJSONFromRequest;

public class PostCreateServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(PostCreateServlet.class.getName());
    private MySqlConnect mySqlServer;

    public PostCreateServlet() {
        this.mySqlServer = new MySqlConnect();
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        long a = currentTimeMillis();
        while (currentTimeMillis() - a < 10) ;
        mySqlServer.init();
        JSONObject req = getJSONFromRequest(request, "PostCreateServlet");
        short status = ok;
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


        long parentId = 0;
        if (req.containsKey("parent")) {
            parentId = req.get("parent") == null ? 0 : (long) req.get("parent");
        }

        String shortName = (String) req.get("forum");
        String email = (String) req.get("user");
        String messagePost = (String) req.get("message");
        long thread = (long) req.get("thread");
        String date = (String) req.get("date");
        int result;

        String matPath = "";
        if (parentId != 0) {
            String parent = mySqlServer.getParentPathByParentId(parentId);
            if (parent == null) {
                status = noRequestedObject;
                message = noParent();
            } else {
                if (parent.equals("")) {
                    matPath = format("%03d", parentId);
                } else {
                    matPath = parent + format("_%03d", parentId);
                }
            }
        }

        JSONObject data = null;
        StringBuilder query = new StringBuilder();
        if (status == ok) {
            int authorId = mySqlServer.getUserIdByEmail(email);
            int forumId = mySqlServer.getForumIdByShortName(shortName);
            if (forumId > 0) {
                if (authorId > 0) {
                    String forumName = mySqlServer.getForumNameById(forumId);
                    query
                            .append("insert into post set ")
                            .append("thread = ").append(thread).append(", ")
                            .append("message = '").append(messagePost).append("', ")
                            .append("author_id = ").append(authorId).append(", ")
                            .append("date_of_creating = '").append(date).append("', ")
                            .append("forum_id = ").append(forumId).append(", ")
                            .append("parent = '").append(matPath).append("', ")
                            .append("isApproved = ").append(isApproved ? 1 : 0).append(", ")
                            .append("isHighlighted = ").append(isHighlighted ? 1 : 0).append(", ")
                            .append("isEdited = ").append(isEdited ? 1 : 0).append(", ")
                            .append("isSpam = ").append(isSpam ? 1 : 0).append(", ")
                            .append("isDeleted = ").append(isDeleted ? 1 : 0).append(", ")
                            .append("name = '").append(mySqlServer.getNameById(authorId)).append("'")
                            .append(";");
                    result = mySqlServer.executeUpdate(query.toString());
                    logger.info(resultUpdate(), result);

                    if (result == 1) {
                        query.delete(0, query.length());
                        query
                                .append("update thread set amountOfPost = amountOfPost + 1 where id = ")
                                .append(thread)
                                .append(';');
                        result = mySqlServer.executeUpdate(query.toString());
                        logger.info(resultUpdate(), result);
                    }

                    query.delete(0, query.length());

                    query
                            .append("select post.id ")
                            .append("from post ")
                            .append("where forum_id = ").append(forumId).append(" and author_id = ")
                            .append(authorId)
                            .append(" and ")
                            .append("post.date_of_creating = '")
                            .append(date)
                            .append("';");
                    int id = -1;
                    ResultSet resultSet = null;
                    Statement statement = null;
                    try {
                        statement = mySqlServer.getStatement();
                        resultSet = mySqlServer.executeSelect(query.toString(), statement);
                        if (resultSet != null && resultSet.next()) {
                            id = resultSet.getInt(1);
                        }
                    } catch (SQLException e) {
                        logger.error(e);
                        e.printStackTrace();
                    } finally {
                        mySqlServer.closeExecution(resultSet, statement);
                    }

                    if (id != -1) {
                        data = new JSONObject();
                        data.put("date", date);
                        data.put("forum", forumName);
                        data.put("id", id);
                        data.put("isApproved", isApproved);
                        data.put("isHighlighted", isHighlighted);
                        data.put("isEdited", isEdited);
                        data.put("isSpam", isSpam);
                        data.put("isDeleted", isDeleted);
                        data.put("message", messagePost);
                        if (matPath.equals("")) {
                            data.put("parent", null);
                        } else {
                            // TODO коссссяк
                            int indexLast = matPath.lastIndexOf("_");
                            data.put("parent", parseInt(matPath.substring(indexLast + 1)));
                        }
                        data.put("thread", thread);
                        data.put("user", email);
                    } else {
                        status = noRequestedObject;
                        message = "Error while PostCreate";
                    }
                } else {
                    status = noRequestedObject;
                    message = noUser();
                    logger.info(noUserOrForum());
                }
            } else {
                status = noRequestedObject;
                message = noForum();
                logger.info(noUserOrForum());
            }
        }
        try {
            createResponse(response, status, message, data);
        } catch (SQLException e) {
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        mySqlServer.close();
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, JSONObject data) throws IOException, SQLException {
        CommonHelper.setResponse(response);
        JSONObject obj = new JSONObject();
        if (status != ErrorMessages.ok) {
            data = new JSONObject();
            data.put("error", message);
        }
        obj.put("response", data);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}