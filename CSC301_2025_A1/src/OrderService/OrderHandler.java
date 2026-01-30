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
            if(method.equalsIgnoreCase("GET") && path.startsWith("/order/")){
                handleGetOrder(exchange,path);
            }else if(method.equalsIgnoreCase("DELETE")&& path.startsWith("/order/")){
                handleCancelOrder(exchange, path);
            } else if(method.equalsIgnoreCase("POST") && path.startsWith("/order")  && bodyString.contains("place order")){
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

    /**
     * Get order information based on order id
     *
     * <p><b>Responses:</b>
     * <ul>
     *   <li>{@code 200}: order information found success; response body is {@code order.toJson().getBytes()}</li>
     *   <li>{@code 400}: missing fields or invalid field type/value; response body is {@code {}}</li>
     *   <li>{@code 404}: order not found; response body is {@code {}}</li>
     * </ul>
     *
     * @param exchange the HTTP exchange used to read and write the response; must be non-null
     * @param path the request URI path; must be non-null
     * @throws IOException if an I/O error occurs while sending the response
     */
    private void handleGetOrder(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 3){
            sendError(exchange,400, "{}");
            return;
        }
        try {
            int orderId = Integer.parseInt(parts[2]);
            Order order = OrderService.orderDatabase.get(orderId);
            if(order != null){
                sendResponse(exchange, 200, order.toJson().getBytes());
            }else{
                sendError(exchange, 404, "{}");
            }
        }catch (NumberFormatException e){
            sendError(exchange, 400, "Invalid Order ID format");
            return;
        }
    }

    /**
     * Cancel order based on order id
     *
     * <p><b>Responses:</b>
     * <ul>
     *   <li>{@code 200}: order cancellation success; response body is {@code order.toJson().getBytes()}</li>
     *   <li>{@code 400}: missing fields or invalid field type/value; response body is {@code {}}</li>
     *   <li>{@code 404}: order not found; response body is {@code {}}</li>
     * </ul>
     *
     * @param exchange the HTTP exchange used to read and write the response; must be non-null
     * @param path the request URI path; must be non-null
     * @throws IOException if an I/O error occurs while sending the response
     */
    private void handleCancelOrder(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if(parts.length < 3){
            sendError(exchange, 400, "Invalid Order ID");
        }

        try {
            int orderId = Integer.parseInt(parts[2]);
            Order order = OrderService.orderDatabase.get(orderId);
            if(order==null){
                sendError(exchange, 404, "Order not found");
                return;
            }
            HttpResponse<String> prodRes = client.send(
                    HttpRequest.newBuilder().uri(URI.create(iscsUrl + "/product/" + order.getProduct_id())).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if(prodRes.statusCode() == 200){
                int currentStock = Integer.parseInt(getJsonValue(prodRes.body(), "quantity"));
                int restoredStock = currentStock + order.getQuantity();

                String updateBody = String.format(
                        "{\"command\": \"update\", \"id\": %d, \"quantity\": %d}",
                        order.getProduct_id(), restoredStock
                );

                client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(iscsUrl + "/product"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(updateBody))
                                .build(),
                        HttpResponse.BodyHandlers.ofByteArray()
                );
            }
            OrderService.orderDatabase.remove(orderId);
            sendResponse(exchange, 200, "{\"status\": \"Order cancelled and stock restored\"}".getBytes());
        }catch (Exception e){}
    }

    /**
     * Place an order based on user id and product id if corresponding product have sufficient quantity
     * in stock
     *
     * <p><b>Responses:</b>
     * <ul>
     *   <li>{@code 200}: order place success; response body is {@code successJson.toJson()}</li>
     *   <li>{@code 400}: missing fields or invalid field type/value; response body is {@code {}}</li>
     *   <li>{@code 404}: invalid order request; response body is {@code {}}</li>
     * </ul>
     *
     * @param exchange the HTTP exchange used to read and write the response; must be non-null
     * @param body a JSON string containing the product id, user id, and quantity
     * @throws IOException if an I/O error occurs while sending the response
     */
    private  void  handlePlaceOrder(HttpExchange exchange, String body) throws IOException, InterruptedException {
        try {
            String userId = getJsonValue(body, "user_id");
            String productId = getJsonValue(body, "product_id");
            // the quantity the order want
            String quantityStr = getJsonValue(body, "quantity");

            if(userId==null || productId == null || quantityStr == null ||
                    userId.equals("invalid-info") || productId.equals("invalid-info") || quantityStr.equals("invalid-info")){
                System.out.println("Enter the if statement; something is null");
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
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(updateBody))
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            sendResponse(exchange, 200, successJson.getBytes());
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
        System.out.println("Forward the information to the ISCS");
        HttpResponse<byte[]> res = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        sendResponse(exchange, res.statusCode(), res.body());
        System.out.println("Receive the response from the ISCS");
    }

    /**
     * Sends an HTTP response with a JSON body.
     *
     * <p><b>Assumptions:</b> {@code response} is a valid JSON string and {@code exchange} is open.</p>
     *
     * @param exchange the HTTP exchange used to send the response; must be non-null
     * @param statusCode the HTTP status code to send
     * @param response the response body to send; must be non-null
     * @throws IOException if an I/O error occurs while sending headers or writing the body
     */
    private void sendResponse(HttpExchange exchange, int statusCode, byte[] response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends an HTTP response with a error status code and error message.
     *
     * <p><b>Assumptions:</b> {@code response} is a valid JSON string and {@code exchange} is open.</p>
     *
     * @param exchange the HTTP exchange used to send the response; must be non-null
     * @param code the HTTP error code
     * @param message the error message
     * @throws IOException if an I/O error occurs while sending headers or writing the body
     */
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String json = String.format("{\"status\": \"%s\"}\n", message);
        sendResponse(exchange, code, json.getBytes());
    }

    /**
     * Gets the value from the JSON string with corresponding key.
     *
     * @param json a JSON string
     * @param key the key of the value searching for
     * @return the value found or null if key not found
     */
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
