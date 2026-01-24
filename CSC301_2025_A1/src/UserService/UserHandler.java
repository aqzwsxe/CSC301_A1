package UserService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class UserHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        System.out.println("[User] method: " + method);
        System.out.println("[User] path: " + path);
        try {
            if(method.equals("GET")){
                handleGet(exchange,path);
            } else if (method.equals("POST")) {
                handlePost(exchange);
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }



    private void handleGet(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if(parts.length < 3){
            // SendResponse
            sendResponse(exchange, 400, "{}");
            return;
        }
        int id = Integer.parseInt(parts[2]);
        User user = UserService.userDatabase.get(id);
        String res1 = String.format("{\n" +
                "        \"id\": %d,\n" +
                "        \"username\": \"%s\",\n" +
                "        \"email\": \"%s\",\n" +
                "        \"password\": \"%s\"\n" +
                "    }", user.getId(), user.getUsername(), user.getEmail(), user.getPassword());

        if(user!=null){
            sendResponse(exchange, 200, user.toJson());
        }
        else{
            sendResponse(exchange,404, "{}");
        }
    }
    // bridge the gap between a raw HTTP request and the User data
    // handle both Get requests and the Post requests


    private void handlePost(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        String command = getJsonValue(body, "command");
        String idStr = getJsonValue(body, "id");

        if(command==null || idStr == null){
            sendResponse(exchange, 400, "{}");
            return;
        }
        int id = Integer.parseInt(idStr);

        // command: represent the value associated with the command key inside the JSON payload
        // that the client sends to your server
        switch (command){
            case "create":
                handleCreate(exchange,id,body);
                break;
            case "update":
                handleUpdate(exchange,id,body);
                break;
            case "delete":
                handleDelete(exchange, id, body);
                break;
            default:
                sendResponse(exchange, 400, "{}");

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
        // Add new line character, so the terminal prompt will start a new line
        String response1 = response + "\n";
        byte[] bytes = response1.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type","application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try(OutputStream os = exchange.getResponseBody()){
            os.write(bytes);
        }
    }


    public  void  handleCreate(HttpExchange exchange, int id, String body) throws IOException {
        if(UserService.userDatabase.containsKey(id)){
            sendResponse(exchange,409,"{}");
            return;
        }

        String username = getJsonValue(body, "username");
        String email = getJsonValue(body, "email");
        String password = getJsonValue(body, "password");
        if(username==null || username.isEmpty()||
                email==null || email.isEmpty()||
                password==null||password.isEmpty()){
            sendResponse(exchange,400, "{}");
            return;
        }


        User newUser = new User(id, username, email, password);
        UserService.userDatabase.put(id,newUser);
        String res1 = String.format("{\n" +
                "        \"id\": %d,\n" +
                "        \"username\": \"%s\",\n" +
                "        \"email\": \"%s\",\n" +
                "        \"password\": \"%s\"\n" +
                "    }", id, username, email, password);
        sendResponse(exchange, 200, res1);


    }

    public  void handleUpdate(HttpExchange exchange, int id, String body) throws IOException {
        User user = UserService.userDatabase.get(id);
        if(user==null){
            sendResponse(exchange, 404, "{}");
            return;
        }

        String newUsername = getJsonValue(body, "username");
        String newEmail = getJsonValue(body, "email");
        String newPassword = getJsonValue(body, "password");

        // According to the instruction: only update the info that are exist
        if(newUsername!=null){
            user.setUsername(newUsername);
        }
        if(newEmail != null){
            user.setEmail(newEmail);
        }
        if(newPassword != null){
            user.setPassword(newPassword);
        }
        String res1 = String.format("{\n" +
                "        \"id\": %d,\n" +
                "        \"username\": \"%s\",\n" +
                "        \"email\": \"%s\",\n" +
                "        \"password\": \"%s\"\n" +
                "    }", id, username, email, password);
        sendResponse(exchange, 200, res1);
    }

    public void handleDelete(HttpExchange exchange, int id, String body) throws IOException {
        User user = UserService.userDatabase.get(id);
        if(user==null){
            sendResponse(exchange,404, "{}");
        }

        String reqUser = getJsonValue(body,"username");
        String reqEmail = getJsonValue(body, "email");
        String reqPassword = getJsonValue(body, "password");

        boolean match = user.getUsername().equals(reqUser)&&
                user.getEmail().equals(reqEmail)&&
                user.getPassword().equalsIgnoreCase(reqPassword);

        if(match){
            UserService.userDatabase.remove(id);
            sendResponse(exchange, 200, "{");
        } else{
            sendResponse(exchange, 401, "{"}");
        }

    }


}
