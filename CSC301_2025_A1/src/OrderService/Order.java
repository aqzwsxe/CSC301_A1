package OrderService;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a order with an unique id.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>{@code product_id} always exists.</li>
 *   <li>{@code user_id} always exists.</li>
 *   <li>{@code quantity} always exists and positive.</li>
 *   <li>{@code status} always exists.</li>
 *   <li>{@code id} always exists.</li>
 * </ul>
 */
public class Order {
    public static final AtomicInteger id_counter = new AtomicInteger(0);
    private  int product_id;
    private  int user_id;
    private  int quantity;
    private String status;
    private int id;

    /**
     * Initializes a new Product.
     *
     * @param product_id the product id
     * @param user_id the user id
     * @param quantity the order quantity; must be positive
     * @param status the order status
     */
    public Order(int product_id, int user_id, int quantity, String status){
        this.id = id_counter.getAndIncrement();
        this.product_id = product_id;
        this.user_id = user_id;
        this.quantity = quantity;
        this.status = status;
    }

    /**
     * Returns the order id
     *
     * @return the order id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the product id
     *
     * @return the product id
     */
    public int getProduct_id() {
        return product_id;
    }

    /**
     * Returns the user id
     *
     * @return the user id
     */
    public int getUser_id() {
        return user_id;
    }

    /**
     * Returns the order quantity
     *
     * @return the order quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Returns the order status
     *
     * @return the order status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Convert the order information into json format.
     *
     * @return a JSON string contains the order's id, product id, user id, quantity and status
     */
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
