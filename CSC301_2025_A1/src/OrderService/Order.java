package OrderService;

import java.util.concurrent.atomic.AtomicInteger;

public class Order {
    public static final AtomicInteger id_counter = new AtomicInteger(0);
    private  int product_id;
    private  int user_id;
    private  int quantity;
    private String status;
    private int id;

    public Order(int product_id, int user_id, int quantity, String status){
        this.id = id_counter.getAndIncrement();
        this.product_id = product_id;
        this.user_id = user_id;
        this.quantity = quantity;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getProduct_id() {
        return product_id;
    }

    public int getUser_id() {
        return user_id;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }


    public String toJson(){
        String result = String.format("{\n" +
                        "    \"id\": %d,\n" +
                        "    \"product_id\": %d,\n" +
                        "    \"user_id\": %d,\n" +
                        "    \"quantity\": %d,\n" +
                        "    \"status\": \"%s\"\n" +
                        "}",
                id, this.product_id, this.user_id, this.quantity, status);
        return result;
    }
}
