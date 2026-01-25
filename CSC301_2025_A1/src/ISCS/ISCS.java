package ISCS;

import Utils.ConfigReader;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ISCS {
    public static void main(String[] args) throws IOException {
        if(args.length < 1){
            System.out.println("ISCS: cannot find the config.json");
            return;
        }

        String configFile = args[0];
        int port = ConfigReader.getPort(configFile,"InterServiceCommunication");
        // Uses a server to listen to the order service
        // It waits for an incoming connection, parses the request, and sends back a response
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // This line tells the server which path prefix should trigger handler
        server.createContext("/", new ISCSHandler(configFile));
        server.setExecutor(Executors.newFixedThreadPool(10));
        System.out.println("ISCS Service started on port "+ port);
        server.start();
    }
}
