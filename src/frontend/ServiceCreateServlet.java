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
import java.sql.SQLException;

import static helper.LoggerHelper.finish;

public class ServiceCreateServlet extends HttpServlet {
    private final Logger logger = LogManager.getLogger(ServiceCreateServlet.class.getName());

    private MySqlConnect mySqlServer;

    public ServiceCreateServlet(MySqlConnect mySqlServer) {
        // this.mySqlServer = mySqlServer;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws ServletException, IOException {
        logger.info("Truncate All!");
        mySqlServer = new MySqlConnect(true);
        int result = 0;
        String queryCreateUser = "create table `users` (\n" +
                "`id` mediumint unsigned auto_increment not null,\n" +
                "`email` char(128) not null unique,\n" +
                "`username` char(64) not null,\n" +
                "`name` char(64) not null,\n" +
                "`isAnonymous` tinyint unsigned not null default '0',\n" +
                "`about` TEXT,\n" +
                "primary key (`id`)\n" +
                ") engine=InnoDB default charset=cp1251;\n";
        String queryCreateFollow = "create table `follow` (\n" +
                "`follower_id` mediumint unsigned not null,\n" +
                "`followee_id` mediumint unsigned not null,\n" +
                "primary key (`follower_id`, `followee_id`),\n" +
                "foreign key(`follower_id`) references `users`(id),\n" +
                "foreign key(`followee_id`) references `users`(id)\n" +
                ") engine=InnoDB default charset=cp1251;\n";
        String queryCreateForum = "create table `forum` (\n" +
                "`id` mediumint unsigned auto_increment not null,\n" +
                "`founder_id` mediumint unsigned not null,\n" +
                "`name` varchar(255) not null unique ,\n" +
                "`short_name` varchar(255) not null unique,\n" +
                "`date_of_creating` TIMESTAMP default NOW(),\n" +
                "primary key (`id`),\n" +
                "foreign key(`founder_id`) references `users`(id)\n" +
                ") engine=InnoDB default charset=cp1251;\n";
        String queryCreateThread = "create table `thread` (\n" +
                "`id` mediumint unsigned auto_increment not null unique,\n" +
                "`isDeleted` tinyint unsigned not null default '0',\n" +
                "`isClosed` tinyint unsigned not null default '0',\n" +
                "`founder_id` mediumint unsigned not null,\n" +
                "`forum_id` mediumint unsigned not null,\n" +
                "`message` LONGTEXT not null,\n" +
                "`title` char(255) not null,\n" +
                "`slug` char(255) not null,\n" +
                "`date_of_creating` TIMESTAMP default NOW(),\n" +
                "`likes` mediumint default 0,\n" +
                "`dislikes` mediumint default 0,\n" +
                "primary key (`forum_id`, `slug`),\n" +
                "foreign key(`founder_id`) references `users`(id),\n" +
                "foreign key(`forum_id`) references `forum`(id)\n" +
                ") engine=InnoDB default charset=cp1251;\n";
        String queryCreatePost = "create table `post` (\n" +
                "`id` mediumint unsigned auto_increment not null,\n" +
                "`isDeleted` tinyint unsigned not null default '0',\n" +
                "`isEdited` tinyint unsigned not null default '0',\n" +
                "`isApproved` tinyint unsigned not null default '0',\n" +
                "`isSpam` tinyint unsigned not null default '0',\n" +
                "`isHighlighted` tinyint unsigned not null default '0',\n" +
                "`author_id` mediumint unsigned not null,\n" +
                "`forum_id` mediumint unsigned not null,\n" +
                "`thread` mediumint unsigned not null,\n" +
                "`parent` varchar(250) not null,\n" +
                "`message` LONGTEXT not null,\n" +
                "`date_of_creating` TIMESTAMP default NOW(),\n" +
                "`likes` mediumint default 0,\n" +
                "`dislikes` mediumint default 0,\n" +
                "primary key (`id`),\n" +
                "foreign key(`author_id`) references `users`(id),\n" +
                "foreign key(`forum_id`) references `forum`(id),\n" +
                "foreign key(`thread`) references `thread`(id)\n" +
                ") engine=InnoDB default charset=cp1251;\n";
        String queryCreateSubscribtion = "create table `subscribtion` (\n" +
                "`user_id` mediumint unsigned not null,\n" +
                "`thread_id` mediumint unsigned not null,\n" +
                "primary key (`user_id`, `thread_id`),\n" +
                "foreign key(`user_id`) references `users`(id)\n" +
                ") engine=InnoDB default charset=cp1251;\n";
        mySqlServer.executeUpdate(queryCreateUser);
        mySqlServer.executeUpdate(queryCreateFollow);
        mySqlServer.executeUpdate(queryCreateForum);
        mySqlServer.executeUpdate(queryCreateThread);
        mySqlServer.executeUpdate(queryCreatePost);
        mySqlServer.executeUpdate(queryCreateSubscribtion);
        try {
            //TODO checking
            createResponse(response, result);
        } catch (SQLException e) {
            logger.info("Error while creating response for truncate");
        }
        mySqlServer.close();
        logger.info(finish());
    }

    private void createResponse(HttpServletResponse response, int result) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);

        JSONObject obj = new JSONObject();
        obj.put("status", result);
        obj.put("code", 0);
        response.getWriter().write(obj.toString());
    }
}