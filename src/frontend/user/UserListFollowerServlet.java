package frontend.user;

import helper.CommonHelper;
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

import static helper.CommonHelper.appendLimitAndAsc;
import static helper.CommonHelper.appendSinceId;
import static helper.ErrorMessages.ok;
import static helper.LoggerHelper.*;

public class UserListFollowerServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(UserListFollowerServlet.class.getName());

    private MySqlConnect mySqlServer;

    public UserListFollowerServlet(MySqlConnect mySqlServer) {
        // this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        mySqlServer = new MySqlConnect(true);
        String email = request.getParameter("user");
        String asc = request.getParameter("order");
        String since_id = request.getParameter("since");
        String limit = request.getParameter("limit");

        short status = ok;
        String message = "";

        StringBuilder query = new StringBuilder();
        ResultSet resultSet;
        Statement statement = mySqlServer.getStatement();
        int userId = mySqlServer.getUserIdByEmail(email);
        query
                .append("select id from users join follow on id = follower_id where followee_id = ").append(userId).append(" ");
        appendSinceId(query, since_id);
        query.append("order by name ");
        appendLimitAndAsc(query, limit, asc);
        resultSet = mySqlServer.executeSelect(query.toString(), statement);

        try {
            createResponse(response, status, message, resultSet);
        } catch (SQLException e) {
            logger.error(responseCreating());
            logger.error(e);
            e.printStackTrace();
        }
        mySqlServer.closeExecution(resultSet, statement);
        mySqlServer.close();
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, short status, String message, ResultSet resultSet) throws IOException, SQLException {
        CommonHelper.setResponse(response);
        JSONObject obj = new JSONObject();
        JSONArray iAmFollowed = new JSONArray();

        if (status != ErrorMessages.ok || resultSet == null) {
            obj.put("response", message);
        } else {
            while (resultSet.next()) {
                iAmFollowed.add(mySqlServer.getUserDetail(resultSet.getInt("id")));
            }
            obj.put("response", iAmFollowed);
        }
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}