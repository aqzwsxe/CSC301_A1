package OrderService;

import Utils.ConfigReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OrderHandler implements HttpHandler {
    private final String iscsUrl;
    private final HttpClient client;



    public OrderHandler(String configFile) throws IOException {
        String rawIp = ConfigReader.getIp(configFile, "InterServiceCommunication");
        int port = ConfigReader.getPort(configFile, "InterServiceCommunication");
        String cleanIp = rawIp.replace("\"","");

        this.iscsUrl = "http://" + cleanIp + ":" + port;
        this.client = HttpClient.newHttpClient();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        String bodyString = new String(requestBody, StandardCharsets.UTF_8);
        String path = exchange.getRequestURI().getPath();
        System.out.println("The Order handle method: " );
        System.out.println("method "+ method);
        System.out.println("The bodyString: "+ bodyString);
        try {
            // Check if it's a request to Place an order
            if(method.equalsIgnoreCase("POST") && path.startsWith("/order")  && bodyString.contains("place order")){
                handlePlaceOrder(exchange, bodyString);
            }else{
                forwardToISCS(exchange,method,path,requestBody);
            }
        }catch (Exception e){
            try {
                sendError(exchange, 400, "Invalid Request");
            }catch (Exception e1){}

        }
    }

    private  void  handlePlaceOrder(HttpExchange exchange, String body) throws IOException, InterruptedException {
        try {
            String userId = getJsonValue(body, "user_id");
            String productId = getJsonValue(body, "product_id");
            String quantityStr = getJsonValue(body, "quantity");

            if(userId==null || productId == null || quantityStr == null){
                sendError(exchange,400, "Invalid Request");
                return;
            }

            int quantity = Integer.parseInt(quantityStr);
            if(quantity <= 0){
                sendError(exchange, 400, "Invalid Request");
                return;
            }

            HttpResponse<String> userRes = client.send(HttpRequest.newBuilder().uri(URI.create(iscsUrl + "/user/" + userId)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            if(userRes.statusCode() == 404){
                sendError(exchange, 404, "Invalid Request");
                return;
            }

            HttpResponse<String> prodRes = client.send(
                    HttpRequest.newBuilder().uri(URI.create(iscsUrl + "/product/" + productId)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if(prodRes.statusCode() == 404){
                sendError(exchange, 404, "Invalid Request");
                return;
            }
            int availableQuantity = Integer.parseInt(getJsonValue(prodRes.body(), "quantity"));
            if(quantity > availableQuantity){
                sendError(exchange, 400, "Exceeded quantity limit");
                return;
            }

            int newStock = availableQuantity-quantity;
            String updateBody = String.format(
                    "{\"command\": " +
                            "\"update\", " +
                            "\"id\": %s, " +
                            "\"quantity\": %d}",
                    productId, newStock
            );


            String successJson = String.format(
                    "{\n" +
                            "        \"product_id\": %s,\n" +
                            "        \"user_id\": %s,\n" +
                            "        \"quantity\": %d,\n" +
                            "        \"status\": \"Success\"\n" +
                            "    }",
                    productId, userId, quantity);
            client.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(iscsUrl + "/product"))
                            .POST(HttpRequest.BodyPublishers.ofString(updateBody))
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            sendResponse(exchange, 200, successJson.getBytes());
            Integer.parseInt(productId);
            Order newOrder = new Order(Integer.parseInt(productId), Integer.parseInt(userId), quantity, "Success");
            OrderService.orderDatabase.put(newOrder.getId(), newOrder);
        }catch (Exception e){
            sendError(exchange, 500, "Internal Server Error");
        }


    }


    private void forwardToISCS(HttpExchange exchange, String method, String path, byte[] requestBody) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(iscsUrl+path));
        if(method.equalsIgnoreCase("POST")){
            builder.header("Content-Type", "application/json");
            builder.POST(HttpRequest.BodyPublishers.ofByteArray(requestBody));
        }else{
            builder.GET();
        }
        HttpResponse<byte[]> res = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        sendResponse(exchange, res.statusCode(), res.body());
    }

    private void sendResponse(HttpExchange exchange, int statusCode, byte[] response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String json = String.format("{\"status\": \"%s\"}\n", message);
        sendResponse(exchange, code, json.getBytes());
    }

    private String getJsonValue(String json, String key){
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if(start==-1){
            return null;
        }

        start += pattern.length();
        int end = json.indexOf(",", start);
        if(end == -1){
            end = json.indexOf("}", start);
        }

        String value = json.substring(start,end).trim();

        if(value.startsWith("\"")){
            value = value.substring(1,value.length()-1);
        }
        return value;
    }


}
