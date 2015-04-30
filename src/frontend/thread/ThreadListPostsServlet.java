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

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

public class ThreadListPostsServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ThreadListPostsServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ThreadListPostsServlet(MySqlConnect mySqlServer) {
        this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(LoggerHelper.start());

        short status = ErrorMessages.ok;
        String message = "";

        String thread_str = request.getParameter("thread");
        String asc = request.getParameter("order");
        String since = request.getParameter("since");
        String limit = request.getParameter("limit");
        String sort = request.getParameter("sort");

        //TODO change to enum
        int sortType = -1;
        if (sort == null) {
            sort = "flat";
        }

        switch (sort) {
            case "flat": sortType = 0; break;
            case "tree": sortType = 1; break;
            case "parent_tree": sortType = 2; break;
        }

        if(thread_str == null || Integer.parseInt(thread_str) == 0 || sortType == -1) {
            status = ErrorMessages.wrongData;
            message = ErrorMessages.wrongJSONData();
        }

        String query = null, subQuery = null;
        ResultSet resultSet = null;
        Statement statement = mySqlServer.getStatement();
        if (status == ErrorMessages.ok) {
            switch (sortType) {
                case 0:
                    query = "select id from post " +
                            "where thread = " + parseInt(thread_str) + " " +
                            (since != null ? ("and date_of_creating > '" + since + "' ") : "") +
                            "order by date_of_creating " +
                            (asc == null ? ("desc ") : asc + " ") +
                            (limit != null ? ("limit " + limit) : "") +
                            ";";
                    break;
                case 1:
                    query = "select id from post " +
                            "where thread = " + parseInt(thread_str) + " " +
                            (since != null ? ("and date_of_creating > '" + since + "' ") : "") +
                            "order by parent, date_of_creating " +
                            (asc == null ? ("desc ") : asc + " ") +
                            (limit != null ? ("limit " + limit) : "") +
                            ";";
                    break;
            }
            if (query == null) {
                subQuery = "select id from post where thread = " + parseInt(thread_str) + " and parent = '' order by date_of_creating limit " + limit + ";";
                Statement statementSub = mySqlServer.getStatement();
                ResultSet resultSetSub = mySqlServer.executeSelect(subQuery, statementSub);
                StringBuilder parents = new StringBuilder();
                parents.append("('000'");
                try {
                    while (resultSetSub.next()) {
                        parents.append(" '" + format("%03d", resultSetSub.getInt("id")) + "'");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                parents.append(')');
                mySqlServer.closeExecution(resultSetSub, statementSub);
                query = "select id from post where thread = " + parseInt(thread_str) + " and LEFT(parent, 3) in " + parents + ";";
            }
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
        JSONArray listPosts = new JSONArray();

        if (status != ErrorMessages.ok || resultSet == null) {
            JSONObject data = new JSONObject();
            data.put("error", message);
            obj.put("response", data);
        } else {
            while (resultSet.next()) {
                listPosts.add(mySqlServer.getPostDetails(resultSet.getInt("id"), false, false, false));
            }
            obj.put("response", listPosts);
        }
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}