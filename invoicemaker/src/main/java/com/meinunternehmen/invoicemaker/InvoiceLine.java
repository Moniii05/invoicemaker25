package com.meinunternehmen.invoicemaker;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Map;

public class InvoiceLine {
    // 1) Properties
    private final IntegerProperty number      = new SimpleIntegerProperty();
    private final StringProperty  description = new SimpleStringProperty();
    private final DoubleProperty  quantity    = new SimpleDoubleProperty();
    private final DoubleProperty  price       = new SimpleDoubleProperty();
    private final DoubleProperty  total       = new SimpleDoubleProperty();

    // 2) Konstruktor
    public InvoiceLine(int number, String description, double quantity, double price) {
        this.number.set(number);
        this.description.set(description);
        this.quantity.set(quantity);
        this.price.set(price);
        // Total initial setzen (wird im Controller bei Änderungen neu berechnet)
        this.total.set(quantity * price);
    }

    // 3) Getter
    public int    getNumber()      { return number.get(); }
    public String getDescription() { return description.get(); }
    public double getQuantity()    { return quantity.get(); }
    public double getPrice()       { return price.get(); }
    public double getTotal()       { return total.get(); }

    // 4) Setter
    public void setNumber(int number)               { this.number.set(number); }
    public void setDescription(String description)  { this.description.set(description); }
    public void setQuantity(double quantity)        { this.quantity.set(quantity); }
    public void setPrice(double price)              { this.price.set(price); }
    public void setTotal(double totalValue)         { this.total.set(totalValue); }

    // 5) Property-Methoden (für TableView)
    public IntegerProperty numberProperty()      { return number; }
    public StringProperty  descriptionProperty() { return description; }
    public DoubleProperty  quantityProperty()    { return quantity; }
    public DoubleProperty  priceProperty()       { return price; }
    public DoubleProperty  totalProperty()       { return total; }

    // 6) toMap für TypstGenerator
    public Map<String, Object> toMap() {
        return Map.of(
            "number",      getNumber(),
            "description", getDescription(),
            "quantity",    getQuantity(),
            "price",       getPrice(),
            "total",       getTotal()
        );
    }
}












