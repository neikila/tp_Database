package main;

import mysql.MySqlConnect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.util.Scanner;

public class Main {
    public static double cpuUs = 0.0;
    private static final Logger logger = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        int port = 8089;
        if (args.length > 0) {
            String portString = args[0];
            port = Integer.valueOf(portString);
        } else {
            System.out.println("Порт не задан.");
            System.out.println("Порт:" + port);
        }
        logger.info("Starting at port: {}", String.valueOf(port));

        final int delay = 3;
        new Thread(new Runnable() {
            @Override
            public void run() {
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Double sum = 0.0;
                        for (int i = 0; i < delay; ++i) {
                            Process process = Runtime.getRuntime().exec("./test.sh");
                            Scanner scanner = new Scanner(new InputStreamReader(process.getInputStream()));
                            scanner.nextLine();
                            String result = scanner.nextLine();
                            scanner.close();
                            sum += Double.parseDouble(result.split(" +")[1].replace(',', '.'));
                        }
                        cpuUs = sum / delay;
                        logger.error("CPU: {}", cpuUs);
                    } catch (Exception e) {
                        logger.error("CPU:\n{}", e);
                    }
                }
            }
        }).start();

        AppServer server = new AppServer(port);

        server.start();
    }
}