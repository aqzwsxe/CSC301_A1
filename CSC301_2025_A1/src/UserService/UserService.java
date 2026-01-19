package UserService;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import Utils.ConfigReader;

public class UserService {
//    Use ConcurrentHashMap instead rather than HashMap, because server is multi-threaded
//    create an interface or super-class, the map is the placeholder; when receive the new requirement; create
    // the actual subclass for the database
    public static Map<Integer, User> userDatabase = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        // Get port from config
//        int port = Utils.ConfigReader.getPort(args[0], "UserService");
        if(args.length < 1){
            System.err.println("Miss the config.json");
            System.exit(1);
        }

        String configPath = args[0];
        try{
            int port = ConfigReader.getPort(configPath, "UserService");
            // new InetSocketAddress(port): combine the IP address and the port number
            // Don't really need to specify the ip address
            HttpServer server = HttpServer.create(new InetSocketAddress(port),0);
            // Handle everything start with /user.
            // routing logic of the microservice. Acts as a filter;
            // Whenever an Http request comes in with a path that starts with /user, hand
            // it over to the UserHandler object to deal with it.
            server.createContext("/user", new UserHandler());
            // Determines how the UserServer handle concurrent requests;
            // Executor: decide
            server.setExecutor(null);
            server.start();
            System.out.println("UserService is listening on port " + port);
        } catch (IOException e){
            System.err.println("Failed to start server: " + e.getMessage());

        }catch (Exception e){
            System.err.println("Error reading config: " + e.getMessage());
        }

    }
}
