package frontend.thread;

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

import static helper.CommonHelper.appendDateAndAscAndLimit;
import static helper.CommonHelper.appendLimitAndAsc;
import static helper.ErrorMessages.*;
import static helper.LoggerHelper.*;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;

public class ThreadListPostsServlet extends HttpServlet {
    private Logger logger = LogManager.getLogger(ThreadListPostsServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ThreadListPostsServlet() {
        this.mySqlServer = new MySqlConnect();
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        mySqlServer.init();

        short status = ok;
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
            case "flat":
                sortType = 0;
                break;
            case "tree":
                sortType = 1;
                break;
            case "parent_tree":
                sortType = 2;
                break;
        }

        if (thread_str == null || parseInt(thread_str) == 0 || sortType == -1) {
            status = wrongData;
            message = wrongJSONData();
        }

        StringBuilder query = new StringBuilder();
        ResultSet resultSet = null;
        Statement statement = mySqlServer.getStatement();
        if (status == ok) {
            switch (sortType) {
                case 0:
                    query
                            .append("select id from post ")
                            .append("where thread = ")
                            .append(parseInt(thread_str))
                            .append(" ");
                    appendDateAndAscAndLimit(query, since, asc, limit);
                    break;
                case 1:
                    query
                            .append("select id from post ")
                            .append("where thread = ").append(parseInt(thread_str)).append(" ");
                    if (since != null) {
                        query.append("and date_of_creating > '").append(since).append("' ");
                    }
                    query.append("order by parent, date_of_creating ");
                    appendLimitAndAsc(query, limit, asc);
                    break;
                case 2:
                    String subQuery = "select id from post where thread = " + parseInt(thread_str) + " and parent = '' order by date_of_creating limit " + limit + ";";
                    Statement statementSub = mySqlServer.getStatement();
                    ResultSet resultSetSub = mySqlServer.executeSelect(subQuery, statementSub);
                    StringBuilder parents = new StringBuilder();
                    parents.append("('000'");
                    try {
                        while (resultSetSub != null && resultSetSub.next()) {
                            parents.append(" '" + format("%03d", resultSetSub.getInt("id")) + "'");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    parents.append(')');
                    mySqlServer.closeExecution(resultSetSub, statementSub);
                    query
                            .append("select id from post where thread = ")
                            .append(parseInt(thread_str))
                            .append(" and LEFT(parent, 3) in ").append(parents).append(";");
            }
            resultSet = mySqlServer.executeSelect(query.toString(), statement);
        }
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
        JSONArray listPosts = new JSONArray();

        if (status != ErrorMessages.ok || resultSet == null) {
            JSONObject data = new JSONObject();
            data.put("error", message);
            obj.put("response", data);
        } else {
            mySqlServer.prepareStatementsForPostDetails();
            while (resultSet.next()) {
                listPosts.add(mySqlServer.getPostDetailsWithPrepareStatement(resultSet.getInt("id"), false, false, false));
            }
            mySqlServer.closePrepareStatementForPostDetails();
            obj.put("response", listPosts);
        }
        obj.put("code", status);
        logger.info(LoggerHelper.responseJSON(), obj.toString());
        response.getWriter().write(obj.toString());
    }
}