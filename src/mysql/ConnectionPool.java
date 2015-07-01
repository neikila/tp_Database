package mysql;

/**
 * Created by neikila on 30.06.15.
 */

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

public class ConnectionPool  {
    private final String DRIVER = "com.mysql.jdbc.Driver";
    private final String URL = "jdbc:mysql://localhost:3306/SMDB_test";

    private GenericObjectPool connectionPool = null;
    private DataSource dataSource = null;

    public ConnectionPool() {
        try {
            setUp();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setUp() throws Exception {

        Class.forName(DRIVER).newInstance();

        connectionPool = new GenericObjectPool();
        connectionPool.setMaxActive(2);

        Properties properties=new Properties();
        properties.setProperty("user","admin");
        properties.setProperty("password","subd_project");
        properties.setProperty("useUnicode","true");
        properties.setProperty("characterEncoding","windows-1251");

        ConnectionFactory cf = new DriverManagerConnectionFactory(
                URL,
                properties);

        new PoolableConnectionFactory(cf, connectionPool, null, null, false, true);
        dataSource = new PoolingDataSource(connectionPool);
    }

    public GenericObjectPool getConnectionPool() {
        return connectionPool;
    }

    public DataSource getDataSource() { return dataSource; }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void printStatus() {
        System.out.println("Max   : " + getConnectionPool().getMaxActive() + "; " +
                "Active: " + getConnectionPool().getNumActive() + "; " +
                "Idle  : " + getConnectionPool().getNumIdle());
    }
}