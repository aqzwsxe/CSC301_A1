package OrderService;

import Utils.ConfigReader;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class OrderService {
    public static final Map<Integer, Order> orderDatabase = new ConcurrentHashMap<>();
    public static void main(String[] args) throws IOException {
        if(args.length < 1){
            System.out.println("The config file has some issues");
            return;
        }
        String configFile = args[0];
        int port = ConfigReader.getPort(configFile, "OrderService");

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new OrderHandler(configFile));
        server.setExecutor(Executors.newFixedThreadPool(10));
        System.out.println("Order Service started on port " + port);
        server.start();
    }


}
