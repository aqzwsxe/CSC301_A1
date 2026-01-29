package ProductService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ProductHandler implements HttpHandler {
    String errorResponse = "{}\n";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        try {
            if(method.equals("GET")){
                handleGet(exchange,path);
            } else if (method.equals("POST")) {
                handlePost(exchange);
            }
        } catch (Exception e){

        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if(parts.length < 3){
            // SendResponse
            sendResponse(exchange, 400, errorResponse);
            return;
        }
        String idStr = parts[2];
        int id;
        if (idStr == null) {
            sendResponse(exchange, 400, errorResponse);
            return;
        } else {
            try {
                id = Integer.parseInt(idStr);
            } catch (Exception e) {
                sendResponse(exchange,400, errorResponse);
                return;
            }
        }

        Product product = ProductService.productDatabase.get(id);

        if(product != null){
            sendResponse(exchange, 200, product.toJson());
        }
        else{
            sendResponse(exchange,404, errorResponse);
        }
    }
    // bridge the gap between a raw HTTP request and the product data
    // handle both Get requests and the Post requests


    private void handlePost(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        String command = getJsonValue(body, "command");
        if (command == null) {
            sendResponse(exchange, 400, errorResponse);
            return;
        } else {
            if (command.isEmpty()) {
                sendResponse(exchange, 400, errorResponse);
                return;
            }
        }

        String idStr = getJsonValue(body, "id");
        int id;
        if (idStr == null) {
            sendResponse(exchange, 400, errorResponse);
            return;
        } else {
            try {
                id = Integer.parseInt(idStr);
            } catch (Exception e) {
                sendResponse(exchange,400, errorResponse);
                return;
            }
        }

        // command: represent the value associated with the command key inside the JSON payload
        // that the client sends to your server
        switch (command){
            case "create":
                handleCreate(exchange, id, body);
                break;
            case "update":
                handleUpdate(exchange, id, body);
                break;
            case "delete":
                handleDelete(exchange, id, body);
                break;
            default:
                sendResponse(exchange, 400, errorResponse);
        }
    }


    private String getJsonValue(String json, String key){
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if(start==-1){
            return null;
        }

        start += pattern.length();
        int end = json.indexOf(",", start);
        if(end==-1){
            end = json.indexOf("}", start);
        }
        String value = json.substring(start, end).trim();
        if(value.startsWith("\"")){
            value = value.substring(1, value.length()-1);
        }
        return value;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type","application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try(OutputStream os = exchange.getResponseBody()){
            os.write(bytes);
        }
    }

    private Boolean inputContentCheck(String body){
        // Name issues:
        String name = getJsonValue(body, "name");
        if (name == null) {
            return false;
        } else {
            if (name.isEmpty()) {
                return false;
            }
        }

        // Description issues:
        String description = getJsonValue(body, "description");
        if (description == null) {
            return false;
        } else {
            if (description.isEmpty()) {
                return false;
            }
        }

        // Price issues:
        String priceStr = getJsonValue(body, "price");
        float price;
        if (priceStr == null) {
            return false;
        } else {
            try {
                price = Float.parseFloat(priceStr);
                if (price < 0) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        // Quantity issues:
        String quantityStr = getJsonValue(body, "quantity");
        int quantity;
        if (quantityStr == null){
            return false;
        } else {
            try {
                quantity = Integer.parseInt(quantityStr);
                if (quantity < 0) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    public void handleCreate(HttpExchange exchange, int id, String body) throws IOException {
        // ID issues:
        if(ProductService.productDatabase.containsKey(id)){
            sendResponse(exchange,409,errorResponse);
            return;
        }

        if (inputContentCheck(body)) {
            String name = getJsonValue(body, "name");
            String description = getJsonValue(body, "description");
            float price = Float.parseFloat(getJsonValue(body, "price"));
            int quantity = Integer.parseInt(getJsonValue(body, "quantity"));

            Product newProduct = new Product(id, name, description, price, quantity);
            ProductService.productDatabase.put(id, newProduct);

            sendResponse(exchange, 200, newProduct.toJson());
        } else {
            sendResponse(exchange,400, errorResponse);
        }
    }

    public  void handleUpdate(HttpExchange exchange, int id, String body) throws IOException {
        Product product = ProductService.productDatabase.get(id);
        if(product == null){
            sendResponse(exchange, 404, errorResponse);
            return;
        }

        if (inputContentCheck(body)) {
            String name = getJsonValue(body, "name");
            String description = getJsonValue(body, "description");
            float price = Float.parseFloat(getJsonValue(body, "price"));
            int quantity = Integer.parseInt(getJsonValue(body, "quantity"));

            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setQuantity(quantity);

            sendResponse(exchange, 200, product.toJson());
        } else {
            sendResponse(exchange,400, errorResponse);
        }
    }

    public void handleDelete(HttpExchange exchange, int id, String body) throws IOException {

        Product product = ProductService.productDatabase.get(id);
        if(product == null){
            sendResponse(exchange, 404, errorResponse);
            return;
        }

        if (inputContentCheck(body)) {
            String name = getJsonValue(body, "name");
            String description = getJsonValue(body, "description");
            float price = Float.parseFloat(getJsonValue(body, "price"));
            int quantity = Integer.parseInt(getJsonValue(body, "quantity"));

            if (product.getName().equals(name) && product.getDescription().equals(description) &&
                    product.getPrice() == price && product.getQuantity() == quantity) {
                ProductService.productDatabase.remove(id);
            }
            sendResponse(exchange, 200, "{}\n");
        } else {
            sendResponse(exchange,400, errorResponse);
        }
    }
}
