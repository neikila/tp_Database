package main;

import mysql.MySqlConnect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        System.out.println(System.getProperty("java.class.path"));
        int port = 8089;
        if (args.length > 0) {
            String portString = args[0];
            port = Integer.valueOf(portString);
        } else {
            System.out.append("Порт не задан.");
        }
        logger.info("Starting at port: {}", String.valueOf(port));

        new Thread(new Runnable() {
            @Override
            public void run() {
                int delay = 3;
                while (true) {
                    try {
                        MySqlConnect.rps.set(MySqlConnect.requestCounter.getAndSet(0) / delay);
                        Thread.sleep(1000 * delay);
                    } catch (Exception e) {
                        System.out.println("Ouch");
                        System.out.println(e);
                    }
                }
            }
        }).start();

        AppServer server = new AppServer(port);

        server.start();
    }
}