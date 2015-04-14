package frontend.thread;

import mysql.MySqlConnect;
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

    private MySqlConnect mySqlServer;

    public ThreadCreateServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Thread_create!");
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
        short status = 0;
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
        result = mySqlServer.executeUpdate(query);
        System.out.println("Strings affected: " + result);
        ResultSet resultSet = null;
        Statement statement = mySqlServer.getStatement();
        query = "select thread.date_of_creating as date, forum.name as forum, thread.id, isClosed, isDeleted, message, slug, title, email as user " +
                "from thread " +
                "join users on founder_id = users.id " +
                "join forum on forum.id = forum_id " +
                "where slug = '" + slug + "' and " +
                "forum.short_name = '" + short_name + "';";
        resultSet = mySqlServer.executeSelect(query, statement);
        try {
            createResponse(response, status, message, resultSet);
        } catch (SQLException e) {
            System.out.println("Error while creating response for PostCreate");
        }
        mySqlServer.closeExecution(resultSet, statement);
        System.out.println("Success!");
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
                status = 4;
                data.put("error", "Huston we have some problems in TreadCreate");
            }
        }
        obj.put("response", data);
        obj.put("code", status);
        response.getWriter().write(obj.toString());
    }
}