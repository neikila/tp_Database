package frontend;


import helper.CommonHelper;
import mysql.MySqlConnect;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StatisticServlet extends HttpServlet {
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {

        CommonHelper.setResponse(response);
        Runtime runtime = Runtime.getRuntime();

        JSONObject obj = new JSONObject();
        obj.put("memory_used", runtime.totalMemory() - runtime.freeMemory());
        obj.put("memory_free", runtime.freeMemory());
        obj.put("memory_total", runtime.totalMemory());
        obj.put("memory_max", runtime.maxMemory());
        obj.put("rps", MySqlConnect.rps);
        obj.put("request_counter", MySqlConnect.requestCounter);
        response.getWriter().write(obj.toString());
    }


    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException, IOException {
    }
}