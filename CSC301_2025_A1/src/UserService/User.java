package UserService;

public class User  {
    int id;
    String username;
    String email;
    String password;

    public User(int id, String username, String email, String password){
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }


    public void setId(int id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /*
    * This method convert the User object to a Json file
    * */
    public String toJson(){
        return String.format("{\"id\": %d, \"username\": \"%s\", \"email\": \"%s\"}",
                this.id, this.username,this.email);
    }
}
