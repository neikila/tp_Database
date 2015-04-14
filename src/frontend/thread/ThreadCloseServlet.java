package frontend.thread;

import mysql.MySqlConnect;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

import static main.JsonInterpreterFromRequest.getJSONFromRequest;

public class ThreadCloseServlet extends HttpServlet {

    private MySqlConnect mySqlServer;

    public ThreadCloseServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Post_create!");
        JSONObject req = getJSONFromRequest(request, "PostCreate");

        short status = 0;
        String message = "";

        long threadId= 0;
        if (req.containsKey("thread")) {
            threadId = (long)req.get("thread");
        } else {
            status = 2;
            message = "Wrong json";
        }

        int result = 0;
        String query;

        if (status == 0) {
            query = "update thread set isClosed = 1 where id = " + threadId + ";";
            result = mySqlServer.executeUpdate(query);
            System.out.println("Strings affected: " + result);
        }
        if (result == 0) {
            status = 1;
            message = "No such post";
        }
        try {
            createResponse(response, status, message, threadId);
        } catch (SQLException e) {
            System.out.println("Error while creating response for PostCreate");
        }
        System.out.println("Success!");
    }

    private void createResponse(HttpServletResponse response, short status, String message, long threadId) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);

        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        if (status != 0) {
            data.put("error", message);
        } else {
            data.put("thread", threadId);
        }
        obj.put("response", data);
        obj.put("code", status);
        response.getWriter().write(obj.toString());
    }
}