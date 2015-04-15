package frontend.forum;

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

public class ForumCreateServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ForumCreateServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ForumCreateServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());
        JSONObject req = getJSONFromRequest(request, "ForumCreateServlet");
        short status = 0;
        String message = "";
        int result = 0;
        String query = "insert into forum set founder_id = (select id from users where email = '" + req.get("user") + "'), " +
                "name='" + req.get("name") + "', " +
                "short_name='" + req.get("short_name") + "';\n";
        logger.info(LoggerHelper.query(), query);
        try {
            result = mySqlServer.executeUpdate(query);
        } catch (Exception e) {
            message = ErrorMessages.forumCreateError();
            logger.info(message);
            logger.error(e);
            result = 0;
            status = 4;
        }
        logger.info(LoggerHelper.resultUpdate(), result);
        ResultSet resultSet = null;
        Statement statement = mySqlServer.getStatement();
        if (result == 1) {
            query = "select email as user, short_name, forum.id, forum.name from forum join users on founder_id = users.id where forum.name = '" + req.get("name") + "';";
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
            resultSet.next();
            data.put("name", resultSet.getString("name"));
            data.put("user", resultSet.getString("user"));
            data.put("short_name", resultSet.getString("short_name"));
            data.put("id", resultSet.getString("id"));
        }
        obj.put("response", data);
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}