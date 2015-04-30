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

import static helper.ErrorMessages.forumCreateError;
import static helper.ErrorMessages.unknownError;
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
        short status = ErrorMessages.ok;
        String message = "";
        String email = (String)req.get("user");
        String shortName = (String)req.get("short_name");
        String forumName = (String)req.get("name");
        int result = 0;
        String query = "insert into forum set founder_id = " + mySqlServer.getUserIdByEmail(email) + ", " +
                "name = '" + forumName + "', " +
                "short_name = '" + shortName + "';\n";
        try {
            result = mySqlServer.executeUpdate(query);
        } catch (Exception e) {
            logger.error(e);
            status = unknownError;
            message = forumCreateError();
        }
        logger.info(LoggerHelper.resultUpdate(), result);
        ResultSet resultSet = null;
        Statement statement = mySqlServer.getStatement();
        if (result == 1) {
            // TODO forum: short_name ; cover: id, name
            query = "select '" + email + "' as user, short_name, forum.id, forum.name from forum " +
                    "where forum.short_name = '" + shortName + "';";
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
        if (status != ErrorMessages.ok) {
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