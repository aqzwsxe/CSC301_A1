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
        String path = exchange.getRequestURI().getPath();
        URI targetUri = URI.create(iscsUrl + path);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(targetUri);
            if(method.equalsIgnoreCase("POST")){
                byte[] body = exchange.getRequestBody().readAllBytes();
                requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body));
            }else{
                requestBuilder.GET();
            }
            HttpRequest request = requestBuilder.build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            sendResponse(exchange, response.statusCode(), response.body());

        } catch (InterruptedException e) {
            byte[] error = "Error connecting to ISCS".getBytes();
            sendResponse(exchange, 502, error);
            throw new RuntimeException(e);

        }
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


}
