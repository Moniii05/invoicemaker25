package com.meinunternehmen.invoicemaker;

public class Customer {
    private final int id;
    private final String name, street, city, postalCode, vatId;

    public Customer(int id, String name, String street, String city, String zip, String vatId) {
        this.id = id;
        this.name = name;
        this.street = street;
        this.city = city;
        this.postalCode = zip;
        this.vatId = vatId;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getPostalCode() { return postalCode; }
    public String getVatId() { return vatId; }

    @Override
    public String toString() {
        return name;
        // ersatz für Dopdown
    }
}
