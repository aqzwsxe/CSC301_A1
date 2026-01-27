package OrderService;

public class Order {
    private  int productId;
    private  int userId;
    private  int quantity;
    private String status;

    public Order(int productId, int userId, int quantity, String status){
        this.productId = productId;
        this.userId = userId;
        this.quantity = quantity;
        this.status = status;
    }

}
