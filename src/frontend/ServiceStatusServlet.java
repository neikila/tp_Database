package frontend;

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

import static helper.LoggerHelper.finish;
import static helper.LoggerHelper.start;
import static javax.servlet.http.HttpServletResponse.SC_OK;

public class ServiceStatusServlet extends HttpServlet {
    private final Logger logger = LogManager.getLogger(ServiceStatusServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ServiceStatusServlet() {
        this.mySqlServer = new MySqlConnect();
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info(start());
        mySqlServer.init();

        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(SC_OK);
        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();


        ResultSet resultSet = null;
        Statement statement = mySqlServer.getStatement();

        String query = "select count(1) as amount from users;";
        resultSet = mySqlServer.executeSelect(query, statement);
        try {
            resultSet.next();
            data.put("user", resultSet.getInt("amount"));
        } catch (Exception e) {
            logger.error(e);
        }

        query = "select count(1) as amount from forum;";
        resultSet = mySqlServer.executeSelect(query, statement);
        try {
            resultSet.next();
            data.put("forum", resultSet.getInt("amount"));
        } catch (Exception e) {
            logger.error(e);
        }

        query = "select count(1) as amount from thread;";
        resultSet = mySqlServer.executeSelect(query, statement);
        try {
            resultSet.next();
            data.put("thread", resultSet.getInt("amount"));
        } catch (Exception e) {
            logger.error(e);
        }

        query = "select count(1) as amount from post;";
        resultSet = mySqlServer.executeSelect(query, statement);
        try {
            resultSet.next();
            data.put("post", resultSet.getInt("amount"));
        } catch (Exception e) {
            logger.error(e);
        }

        obj.put("response", data);
        obj.put("code", 0);
        response.getWriter().write(obj.toString());
        mySqlServer.close();
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, int result) throws IOException, SQLException {
    }
}