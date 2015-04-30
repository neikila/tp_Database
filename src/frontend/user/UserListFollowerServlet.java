package frontend.user;

import helper.ErrorMessages;
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

import static helper.ErrorMessages.ok;
import static helper.LoggerHelper.*;

public class UserListFollowerServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(UserListFollowerServlet.class.getName());

    private MySqlConnect mySqlServer;

    public UserListFollowerServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        String email = request.getParameter("user");
        String asc = request.getParameter("order");
        String since_id = request.getParameter("since");
        String limit = request.getParameter("limit");

        short status = ok;
        String message = "";

        String query;
        ResultSet resultSet;
        Statement statement = mySqlServer.getStatement();

        query = "select id from users join follow on id = follower_id where followee_id = " +
                "(select id from users where email = '" + email + "') " +
                (since_id != null ? ("and id > '" + since_id + "' ") : "") +
                "order by name " +
                (asc == null ? ("desc ") : asc + " ") +
                (limit != null ? ("limit " + limit) : "") +
                ";";
        resultSet = mySqlServer.executeSelect(query, statement);

        try {
            createResponse(response, status, message, resultSet);
        } catch (SQLException e) {
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        mySqlServer.closeExecution(resultSet, statement);
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, ResultSet resultSet) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);
        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        JSONArray iAmFollowed = new JSONArray();

        if (status != ErrorMessages.ok || resultSet == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            data.put("error", message);
            obj.put("response", data);
        } else {
            while (resultSet.next()) {
                iAmFollowed.add(mySqlServer.getUserDetail(resultSet.getInt("id")));
            }
            if (iAmFollowed.size() > 0) {
                obj.put("response", iAmFollowed);
            } else {
                status = 1;
                data.put("error", message);
                obj.put("response", data);
            }
        }
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}