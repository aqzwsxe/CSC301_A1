package ProductService;

public class Product {
    int pid;
    String name;
    String description;
    float price;
    int quantity_in_stock;

    public Product(int pid, String name, String description, float price, int quantity_in_stock) {
        this.pid = pid;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity_in_stock = quantity_in_stock;
    }

    public int getPid() {
        return this.pid;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public float getPrice() {
        return this.price;
    }

    public int getQuantity() {
        return this.quantity_in_stock;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public void setQuantity(int quantity) {
        this.quantity_in_stock = quantity;
    }

    public String toJson(){
        return String.format("{\"id\": %d, \"productname\": \"%s\", \"description\": \"%s\", \"price\": \"%f\", \"quantity\": \"%d\"}",
                this.pid, this.name, this.description, this.price, this.quantity_in_stock);
    }
}
