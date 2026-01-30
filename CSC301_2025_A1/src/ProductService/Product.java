package ProductService;

/**
 * Represents a product with an unique id.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>{@code pid} always exists.</li>
 *   <li>{@code price} always positive.</li>
 *   <li>{@code quantity} always positive.</li>
 * </ul>
 */
public class Product {
    int pid;
    String name;
    String description;
    float price;
    int quantity_in_stock;

    /**
     * Initializes a new {@code Product}.
     *
     * @param pid the product id
     * @param name the product name; must be non-empty
     * @param description the product description; must be non-empty
     * @param price the product price; must be positive
     * @param quantity_in_stock the product quantity; must be positive
     */
    public Product(int pid, String name, String description, float price, int quantity_in_stock) {
        this.pid = pid;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity_in_stock = quantity_in_stock;
    }

    /**
     * Returns the product id
     *
     * @return the product id
     */
    public int getPid() {
        return this.pid;
    }

    /**
     * Returns the product name
     *
     * @return the product name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the product description
     *
     * @return the product description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the product price
     *
     * @return the product price
     */
    public float getPrice() {
        return this.price;
    }

    /**
     * Returns the product price
     *
     * @return the product price
     */
    public int getQuantity() {
        return this.quantity_in_stock;
    }

    /**
     * Update the product name
     *
     * @param name the product name; must be String
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Update the product description
     *
     * @param description the product description; must be non-empty
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Update the product price
     *
     * <p> If price is negative, then the value will not change
     *
     * @param price the product price; must be positive
     */
    public void setPrice(float price) {
        if (price >= 0) {
            this.price = price;
        }
    }

    /**
     * Update the product quantity
     *
     * <p> If quantity is negative, then the value will not change
     *
     * @param quantity the product quantity; must be positive
     */
    public void setQuantity(int quantity) {
        this.quantity_in_stock = quantity;
    }

    /**
     * Convert the product information into json format.
     *
     * @return a JSON string contains the product's id, name, description, price and quantity
     */
    public String toJson(){
        return String.format("{\n" +
                        "\"id\": %d, \n" +
                        "\"name\": \"%s\", \n" +
                        "\"description\": \"%s\", \n" +
                        "\"price\": %.2f, \n" +
                        "\"quantity\": %d\n" +
                        "}\n",
                this.pid, this.name, this.description, this.price, this.quantity_in_stock);
    }
}
