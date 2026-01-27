package ProductService;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import Utils.ConfigReader;

public class ProductService {
    public static Map<Integer, Product> productDatabase = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        if(args.length < 1){
            System.err.println("Miss the config.json");
            System.exit(1);
        }

        String configPath = args[0];
        try{
            int port = ConfigReader.getPort(configPath, "ProductService");
            // new InetSocketAddress(port): combine the IP address and the port number
            // Don't really need to specify the ip address
            HttpServer server = HttpServer.create(new InetSocketAddress(port),0);
            // Handle everything start with /product.
            // routing logic of the microservice. Acts as a filter;
            // Whenever an Http request comes in with a path that starts with /product, hand
            // it over to the ProductHandler object to deal with it.
            server.createContext("/product", new ProductHandler());
            // Determines how the ProductServer handle concurrent requests;
            // Executor: decide
            server.setExecutor(null);
            server.start();
            System.out.println("ProductService is listening on port " + port);
        } catch (IOException e){
            System.err.println("Failed to start server: " + e.getMessage());

        }catch (Exception e){
            System.err.println("Error reading config: " + e.getMessage());
        }

    }
}
