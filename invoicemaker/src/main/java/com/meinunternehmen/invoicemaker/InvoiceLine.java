package com.meinunternehmen.invoicemaker;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

//import java.util.Map;

public class InvoiceLine {
    
    private final IntegerProperty lineNumber      = new SimpleIntegerProperty();
    private final StringProperty  description = new SimpleStringProperty();
    private final DoubleProperty  quantity    = new SimpleDoubleProperty();
    private final DoubleProperty  unitPrice       = new SimpleDoubleProperty();
    private final DoubleProperty  lineTotal       = new SimpleDoubleProperty();

    // konstruktor
    public InvoiceLine(int number, String description, double quantity, double price) {
        this.lineNumber.set(number);
        this.description.set(description);
        this.quantity.set(quantity);
        this.unitPrice.set(price);
      
        this.lineTotal.bind(this.quantity.multiply(this.unitPrice));
    }

    // getter
    public int    getLineNumber()      { return lineNumber.get(); }
    public String getDescription() { return description.get(); }
    public double getQuantity()    { return quantity.get(); }
    public double getUnitPrice()       { return unitPrice.get(); }
    public double getLineTotal()       { return lineTotal.get(); }

    // setter
    public void setLineNumber(int number)               { this.lineNumber.set(number); }
    public void setDescription(String description)  { this.description.set(description); }
    public void setQuantity(double quantity)        { this.quantity.set(quantity); }
    public void setUnitPrice(double price)              { this.unitPrice.set(price); }
   // public void setLineTotal(double totalValue)         { this.lineTotal.set(totalValue); }

    // Properties (für TableView-bindings) + autom. akutalisieren bei Veränderung 
    public IntegerProperty lineNumberProperty()      { return lineNumber; }
    public StringProperty  descriptionProperty() { return description; }
    public DoubleProperty  quantityProperty()    { return quantity; }
    public DoubleProperty  unitPriceProperty()       { return unitPrice; }
    public DoubleProperty  lineTotalProperty()       { return lineTotal; }

    
}









