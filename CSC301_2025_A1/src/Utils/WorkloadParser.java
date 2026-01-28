package Utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class WorkloadParser {

    private static String orderUrl;
    private static final HttpClient client = HttpClient.newHttpClient();
    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
        // when run the program via terminal through runme.sh, pass the path to the text file
        // java WorkloadParser workload3u20c.txt
        // args[0] becomes workload3u20c.txt
        File file = new File(args[0]);
        // Use the scanner to read the content of the workload file
        Scanner sc = new Scanner(file, StandardCharsets.UTF_8);

        String configPath = "config.json";
        int port = ConfigReader.getPort(configPath, "OrderService");
        String ip = ConfigReader.getIp(configPath, "OrderService");
        orderUrl = "http://" + ip + ":" + port;
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(" ");
            String service = parts[0]; // USER, PRODUCT, or ORDER
            String command = parts[1]; // create get, update, delete or place order

            // Although all the request will be sent to the OrderService first
            // Still need to differ them here, because we need to build the path
            if (service.equals("USER")) {
                handleUser(command,parts);
            } else if (service.equals("ORDER")) {
                handleOrder(parts);
            } else if (service.equals("PRODUCT")) {
                handleProduct(command,parts);
            }
        }
    }


    /*
    *  This method build the jsonBody that will be sent to OrderService endpoint first. But eventually, the
    *  jsonBody will be sent to the UserService
    * */
    public static void handleUser(String command, String[] parts) throws IOException, URISyntaxException, InterruptedException {
        //build JSON and send to Order Service
        if (command.equals("get")) {
            sendGetRequest("/user/"+parts[2]);
        } else {
            String jsonBody = "";
            if (command.equals("create")||command.equals("delete")) {
                jsonBody = String.format("{\"command\":\"%s\",\"id\":%s,\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                        command, parts[2], parts[3], parts[4], parts[5]);
            } else if (command.equals("update")) {
                String id = parts[2];
                String username = parts[3].replace("username:","");
                String email = parts[4].replace("email:","");
                String password = parts[5].replace("password:","");
                jsonBody = String.format(
                        "{\"command\":\"update\",\"id\":%s,\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                        id, username, email, password
                );
            }
            sendPostRequest("/user", jsonBody);
        }
    }




    public static void handleOrder(String[] parts) throws IOException, URISyntaxException, InterruptedException {
        //build JSON and send to Order Service
        //ORDER place <product_id> <user_id> <quantity>
//        if(parts[1].equals("get")){ // I don't think there is a get for ORDER check WorkLoadTemplate.txt
//            sendGetRequest("/order/" + parts[2]);
//        }else if(parts[1].equals("place") && parts[2].equals("order")){
//            String jsonBody = String.format(
//                    "{\"command\":\"place order\",\"user_id\":%s,\"product_id\":%s,\"quantity\":%s}",
//                    parts[3], parts[4], parts[5]);
//            sendPostRequest("/order", jsonBody);
//        }
        if (parts[1].equals("place")) { // I don't think there is a get for ORDER check WorkLoadTemplate.txt
            String jsonBody = String.format(
                    "{\"command\":\"place order\",\"user_id\":%s,\"product_id\":%s,\"quantity\":%s}",
                    parts[3], parts[2], parts[4]);
            sendPostRequest("/order", jsonBody);
        }
    }

    /*
    * Build the jsonBody that will be sent to the OrderService. Eventually, the jsonBody will be sent to product service
    * */
    public static void handleProduct(String command, String[] parts) throws IOException, URISyntaxException, InterruptedException {
        if (command.equalsIgnoreCase("info")) {
            sendGetRequest("/product/" + parts[2]);
        } else if (command.equalsIgnoreCase("create")){
            String id = parts[2];
            String name = parts[3];
            String description = parts[4];
            String price = parts[5];
            String quantity = parts[6];
            String json = String.format(
                    "{\"command\":\"create\"," +
                            "\"id\":%s," +
                            "\"name\":\"%s\"," +
                            "\"description\":\"%s\"," +
                            "\"price\":%s," +
                            "\"quantity\":%s}",
                    id, name, description, price, quantity
            );
            sendPostRequest("/product", json);
        } else if (command.equalsIgnoreCase("update")){
            String id = parts[2];
            String name = parts[3].replace("name:","");
            String description = parts[4].replace("description:","");
            String price = parts[5].replace("price:","");
            String quantity = parts[6].replace("quantity", "");
            String json = String.format(
                    "{\"command\":\"update\"," +
                            "\"id\":%s," +
                            "\"name\":\"%s\"," +
                            "\"description\":\"%s\"," +
                            "\"price\":%s," +
                            "\"quantity\":%s}",
                    id, name, description, price, quantity
            );
            sendPostRequest("/product", json);
        } else if (command.equalsIgnoreCase("delete")){
            String id = parts[2];
            String name = parts[3];
            String price = parts[4];
            String quantity = parts[5];
            String json = String.format(
                    "{\"command\":\"update\"," +
                            "\"id\":%s," +
                            "\"name\":\"%s\"," +
                            "\"price\":%s," +
                            "\"quantity\":%s}",
                    id, name, price, quantity
            );
            sendPostRequest("/product", json);
        }
    }


    /*
    * Specify the orderUrl. The requests will be sent to the Order Service
    * */
    public static void sendGetRequest(String endpoint) throws IOException, InterruptedException, URISyntaxException {
        try {
            // when the code executes
            // 1: WorkloadParser (client) sends the Get request to the orderSErvice
            // 2: OrderService: (Gateway) receives it and forwards it to the ISCS
            // 3: ISCS (router) identifies the path (/user/2) and route it to the correct UserService instance
            // 4: User service returns the json, which travels back through the ISCS and OrderService to the parser
            //URI (the identifier): the general name for any string that identifies a resource
            //URL (The name): A spacific type of URI that identifies a resource by a persistent unique name (like book's ISBN),
            // even if its location changes
            // Structure of URI
            // 1: Scheme: http (specifies the protocol/method of access)
            // 2: Authority: the IP address and port; For example, 127.0.0.1(IP):14000(port);
            HttpRequest request1 = HttpRequest.newBuilder().
                    uri(URI.create(orderUrl+endpoint)).
                    GET().build();
            // Execution phase; HttpRequest defines what to do; this line actually does it
            // client.send(): synchronous; the program pauses on this line until the order service
            // response or the connection times out.
            HttpResponse<String> response = client.send(request1, HttpResponse.BodyHandlers.ofString());
            System.out.println("GET " + endpoint + " | Status: " + response.statusCode());
            System.out.println("Data: " + response.body());
        }catch (Exception e){
            System.err.println("Error sending GET request: "+ e.getMessage());
        }
    }

    /*
    * Specify the orderUrl. The request will be sent to the orderService
    * */
    public static void sendPostRequest(String endpoint, String jsonBody){
        try{
            HttpRequest request  = HttpRequest.newBuilder()
                    .uri(URI.create(orderUrl+endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("POST " + endpoint + "| Status: " + response.statusCode());
            if(response.statusCode() != 200){
                System.out.println("Response Body: " + response.body());
            }
        }catch (Exception e){

        }
    }
}
