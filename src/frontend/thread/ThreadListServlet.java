package frontend.thread;

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

public class ThreadListServlet extends HttpServlet {

    private Logger logger = LogManager.getLogger(ThreadListServlet.class.getName());
    private MySqlConnect mySqlServer;

    public ThreadListServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());

        short status = ok;
        String message = "";

        String forum = request.getParameter("forum");
        String email = request.getParameter("user");
        String asc = request.getParameter("order");
        String since = request.getParameter("since");
        String limit = request.getParameter("limit");

        if (forum == null && email == null) {
            status = 3;
            message = "Wrong data";
        }

        String query;
        ResultSet resultSet;
        Statement statement = mySqlServer.getStatement();
        query = "select id from thread " +
                (forum != null ? "where forum_id = (select id from forum where short_name = '" + forum + "') "
                        : "where founder_id = (select id from users where email = '" + email + "') ") +
                (since != null ? ("and date_of_creating > '" + since + "' ") : "") +
                "order by date_of_creating " +
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

        JSONArray listThreads = new JSONArray();

        if (status != ErrorMessages.ok || resultSet == null) {
            data.put("error", message);
            obj.put("response", data);
        } else {
            while (resultSet.next()) {
                listThreads.add(mySqlServer.getThreadDetailsById(resultSet.getInt("id"), false, false));
            }
            obj.put("response", listThreads);
        }
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}