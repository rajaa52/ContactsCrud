package ma.ensa.examlocalisation.classes;

public class Contact {
    private Long id;  // Add this field
    private String name;
    private String number;

    public Contact(String name, String number) {
        this.name = name;
        this.number = number;
    }

    // Add getter and setter for id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Existing getters
    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    // Add setters if needed
    public void setName(String name) {
        this.name = name;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}