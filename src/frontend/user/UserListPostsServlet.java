package frontend.user;

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


public class UserListPostsServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(UserListPostsServlet.class.getName());

    private MySqlConnect mySqlServer;

    public UserListPostsServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());

        short status = 0;
        String message = "";
        ResultSet resultSet = null;
        Statement statement = mySqlServer.getStatement();
        String date = request.getParameter("since");
        String limit = request.getParameter("limit");
        String asc = request.getParameter("order");
        String query = "select post.id from post join (select id from users where email = '" + request.getParameter("user") + "') as u " +
                "on u.id = post.author_id where 1 > 0 " +
                (date != null?("and date_of_creating > '" + date + "' "):"") +
                "order by date_of_creating " +
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

        JSONArray postList = new JSONArray();
        JSONObject post;
        if (resultSet != null) {
            while (resultSet.next()) {
                post = mySqlServer.getPostDetails(resultSet.getInt("id"), false, false, false);
                postList.add(post);
            }
        } else {
            status = 4;
        }
        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        if (status != 0) {
            data.put("error", message);
            obj.put("response", data);
        } else {
            obj.put("response", postList);

        }

        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}