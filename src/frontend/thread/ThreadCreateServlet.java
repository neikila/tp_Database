package frontend.thread;

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

public class ThreadCreateServlet extends HttpServlet {
    private final Logger logger = LogManager.getLogger(ThreadCreateServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ThreadCreateServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());
        JSONObject req = getJSONFromRequest(request, "ThreadCreate");
        boolean isDeleted = false;
        if (req.containsKey("isDeleted")) {
            isDeleted = (boolean) req.get("isDeleted");
        }
        String short_name = (String)req.get("forum");
        String title = (String)req.get("title");
        boolean isClosed = (boolean)req.get("isClosed");
        String user = (String)req.get("user");
        String messageThread = (String)req.get("message");
        String slug = (String)req.get("slug");
        String date = (String)req.get("date");
        short status = ErrorMessages.ok;
        String message = "";
        int result;
        String query =
                "insert into thread set " +
                "forum_id = (select id from forum where short_name = '" + short_name + "'), " +
                "title = '" + title + "', " +
                "isClosed = " + (isClosed ? 1 : 0) + ", " +
                "founder_id = (select id from users where email = '" + user + "'), " +
                "date_of_creating = '" + date + "', " +
                "message = '" + messageThread + "', " +
                "slug = '" + slug + "' " +
                (isDeleted ? ", isDeleted = 1;" : ";");
        logger.info(LoggerHelper.query(), query);
        result = mySqlServer.executeUpdate(query);
        logger.info(LoggerHelper.resultUpdate(), result);
        ResultSet resultSet = null;
        Statement statement = mySqlServer.getStatement();
        query = "select thread.date_of_creating as date, forum.name as forum, thread.id, isClosed, isDeleted, message, slug, title, email as user " +
                "from thread " +
                "join users on founder_id = users.id " +
                "join forum on forum.id = forum_id " +
                "where slug = '" + slug + "' and " +
                "forum.short_name = '" + short_name + "';";
        logger.info(LoggerHelper.query(), query);
        resultSet = mySqlServer.executeSelect(query, statement);
        try {
            createResponse(response, status, message, resultSet);
        } catch (SQLException e) {
            logger.error(LoggerHelper.responseCreating());
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
                data.put("forum", resultSet.getString("forum"));
                data.put("id", resultSet.getInt("id"));
                data.put("isClosed", resultSet.getBoolean("isClosed"));
                data.put("isDeleted", resultSet.getBoolean("isDeleted"));
                data.put("message", resultSet.getString("message"));
                data.put("slug", resultSet.getString("slug"));
                data.put("title", resultSet.getString("title"));
                data.put("user", resultSet.getString("user"));
                data.put("date", resultSet.getString("date").substring(0,19));
            } else {
                status = ErrorMessages.noRequestedObject;
                data.put("error", ErrorMessages.noThread());
            }
        }
        obj.put("response", data);
        obj.put("code", status);
        response.getWriter().write(obj.toString());
    }
}