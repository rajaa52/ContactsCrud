package ma.ensa.examlocalisation.classes;

public class Contact {
    private String name;
    private String number;

    public Contact(String name, String number) {
        this.name = name;
        this.number = number;
    }

    // Getters
    public String getName() { return name; }
    public String getNumber() { return number; }
}