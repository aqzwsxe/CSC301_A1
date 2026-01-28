package OrderService;

public class Order {
    public static int id_counter = 0;
    private  int product_id;
    private  int user_id;
    private  int quantity;
    private String status;
    private int id;

    public Order(int product_id, int user_id, int quantity, String status){
        this.id = Order.id_counter;
        Order.id_counter++;
        this.product_id = product_id;
        this.user_id = user_id;
        this.quantity = quantity;
        this.status = status;
    }

}
