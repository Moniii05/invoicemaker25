package com.meinunternehmen.invoicemaker;

//import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.NumberStringConverter;

import java.sql.*;
import java.time.LocalDate;

import javafx.scene.Node;

import org.json.JSONObject;
import org.json.JSONArray;

import javafx.scene.control.cell.TextFieldTableCell;

/*import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern; */


public class LayoutController {

    @FXML private ComboBox<Customer> customerCombo;
    @FXML private TextField kundenNummerField;

    @FXML private TextField streetField;
    @FXML private TextField postalCodeField;
    @FXML private TextField cityField;

    @FXML private TableView<InvoiceLine> leistungenTable;
    @FXML private TableColumn<InvoiceLine, Integer> colNummer;
    @FXML private TableColumn<InvoiceLine, String>  colDescription;
    @FXML private TableColumn<InvoiceLine, Number>  colQuantity;
    @FXML private TableColumn<InvoiceLine, Number>  colPrice;
    @FXML private TableColumn<InvoiceLine, Number>  colTotal;

    @FXML private DatePicker DateIssued, DateDelivered, DateDue;
    @FXML private TextField rechnungsNummerField;

    private Database db;
    public void setDatabase(Database db) { this.db = db; }

    @FXML
    public void initialize() {
        rechnungsNummerField.setEditable(true);

        customerCombo.setEditable(true);

        customerCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Customer customer) {
            return customer == null ? "" : customer.getName(); 
            }
            
            
            @Override public Customer fromString(String text) {
               for (Customer existing : customerCombo.getItems()) {
                    if (existing.getName().equals(text)) return existing;
                }
                return new Customer(0, text, "", "", "", "");
            }
        });

        // fokus weg --> felder füllen/leeren
        customerCombo.getEditor().focusedProperty().addListener((ignored, notUsed, isFocused) -> {
            if (!isFocused) {
                Customer selected = customerCombo.getConverter()
                        .fromString(customerCombo.getEditor().getText().trim());
                customerCombo.setValue(selected);
                if (selected.getId() > 0) {
                    kundenNummerField.setText(String.valueOf(selected.getId()));
                    streetField.setText(selected.getStreet());
                    postalCodeField.setText(selected.getPostalCode());
                    cityField.setText(selected.getCity());
                } else {
                    kundenNummerField.clear();
                    streetField.clear();
                    postalCodeField.clear();
                    cityField.clear();
                }
            }
        });

        // Tabelle 
        colNummer.setCellValueFactory(cell -> cell.getValue().lineNumberProperty().asObject()); // asObject für primitive unt/double

      //  colDescription.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
       // colDescription.setCellFactory(column -> new EditingCell<>(new DefaultStringConverter()));
      //  colDescription.setOnEditCommit(event -> {
       //     event.getRowValue().setDescription(event.getNewValue());
       //     leistungenTable.refresh();
            
            colDescription.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
            colDescription.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
            colDescription.setOnEditCommit(evt -> {
                evt.getRowValue().setDescription(evt.getNewValue());
        });

            colQuantity.setCellValueFactory(cell -> cell.getValue().quantityProperty());
            colQuantity.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));
            colQuantity.setOnEditCommit(evt -> {
                // newValue = (double), weil erwartet int eigentlcih:
                evt.getRowValue().setQuantity(evt.getNewValue().doubleValue());
           
        });

            colPrice.setCellValueFactory(cell -> cell.getValue().unitPriceProperty());
            colPrice.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));
            colPrice.setOnEditCommit(evt -> {
                evt.getRowValue().setUnitPrice(evt.getNewValue().doubleValue());
           
        });

        colTotal.setCellValueFactory(cell -> cell.getValue().lineTotalProperty());

        leistungenTable.setEditable(true);
      // leistungenTable.getItems().addListener(
       //         (ListChangeListener<InvoiceLine>) change -> recalcTotals());

        leistungenTable.getItems().add(new InvoiceLine(1, "", 1, 0.0));

     
        LocalDate today = LocalDate.now();
        DateIssued.setValue(today);
        DateDelivered.setValue(today);
        DateDue.setValue(today.plusDays(14));
    }

    // lädt kunde + vergibt ReNr
    public void loadRecipientData() {
        customerCombo.getItems().clear(); // clear ListenMethode um Duplikate verhindern
        try (Connection DBconn = db.openConnection();
             Statement stmt = DBconn.createStatement();
             ResultSet results = stmt.executeQuery("""
                     SELECT id, name, street, city, postal_code
                     FROM customer
                     ORDER BY name
                 """)) {
            while (results.next()) { // next = JDBC
                customerCombo.getItems().add(new Customer(
                        results.getInt("id"),
                        results.getString("name"),
                        results.getString("street"),
                        results.getString("city"),
                        results.getString("postal_code"),
                        "" // vatId
                ));   }
        } catch (SQLException ex) {
            showError("Fehler beim Laden der Kunden", ex);   
            }
        
        rechnungsNummerField.setText("Re0001");

        /* ReNR vorschlagen
        try {
            rechnungsNummerField.setText(generateNextInvoiceNumber());
        } catch (SQLException ex) {
            rechnungsNummerField.setText("Re0001"); // JavaFX
        } */
    }

    @FXML
    private void onNewCustomer() {
        Dialog<Customer> customerDialog = new Dialog<>();
        customerDialog.setTitle("Neuen Kunden anlegen");
        customerDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameInput       = new TextField();
        TextField streetInput     = new TextField();
        TextField postalCodeInput = new TextField();
        TextField cityInput       = new TextField();

        GridPane formGrid = new GridPane();
        formGrid.setHgap(8);
        formGrid.setVgap(6);
        formGrid.addRow(0, new Label("Name*:"),  nameInput);
        formGrid.addRow(1, new Label("Straße:"), streetInput);
        formGrid.addRow(2, new Label("PLZ:"),    postalCodeInput);
        formGrid.addRow(3, new Label("Stadt:"),  cityInput);
        customerDialog.getDialogPane().setContent(formGrid);

        Node okButton = customerDialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true); //von node

        nameInput.textProperty().addListener((ignoredObs, ignoredOld, newText) ->
                okButton.setDisable(newText.trim().isEmpty())
        );

        customerDialog.setResultConverter(clicked -> {
            if (clicked == ButtonType.OK) {
                return new Customer(
                        0,                             
                        nameInput.getText().trim(),
                        streetInput.getText().trim(),
                        cityInput.getText().trim(),
                        postalCodeInput.getText().trim(),
                        ""             
                        ); }
            return null;
        });

        Customer created = customerDialog.showAndWait().orElse(null); // show von dialog 
        if (created == null) return;

        try (Connection conn = db.openConnection();
             PreparedStatement ps = conn.prepareStatement("""
                 INSERT INTO customer (name, street, city, postal_code)
                 VALUES (?, ?, ?, ?)
             """, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, created.getName());
            ps.setString(2, created.getStreet());
            ps.setString(3, created.getCity());
            ps.setString(4, created.getPostalCode());
            ps.executeUpdate();

            int generatedId = 0; 
            try (ResultSet keys = ps.getGeneratedKeys()) {
                generatedId = keys.next() ? keys.getInt(1) : 0;
                
            }

            Customer saved = new Customer(
                    generatedId,
                    created.getName(),
                    created.getStreet(),
                    created.getCity(),
                    created.getPostalCode(),
                    ""   );

            customerCombo.getItems().add(saved); 
            customerCombo.setValue(saved);

            kundenNummerField.setText(String.valueOf(generatedId));
            streetField.setText(saved.getStreet());      
            postalCodeField.setText(saved.getPostalCode());
            cityField.setText(saved.getCity());

        } catch (SQLException ex) {
            showError("Kunde konnte nicht gespeichert werden", ex);
        }
    }


    @FXML
    private void onAddRow() {
        if (leistungenTable.getEditingCell() != null) {
            leistungenTable.edit(-1, null);
        }
        int nextRowNumber = leistungenTable.getItems().size() + 1;
        
        leistungenTable.getItems().add(new InvoiceLine(nextRowNumber, "", 1, 0.0));
        
        leistungenTable.scrollTo(nextRowNumber - 1);
    }

    @FXML
    private void onDeleteRow() {
        InvoiceLine selectedRow = leistungenTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) return;

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION,
                "Ausgewählte Zeile wirklich löschen?",
                ButtonType.OK, ButtonType.CANCEL);

        ButtonType clicked = confirmDialog.showAndWait().orElse(ButtonType.CANCEL);
        if (clicked != ButtonType.OK) return;

        leistungenTable.getItems().remove(selectedRow);

        // nummern neu vergeben
        for (int i = 0; i < leistungenTable.getItems().size(); i++) {
            leistungenTable.getItems().get(i).setLineNumber(i + 1);
        }
      //  recalcTotals();
    }

    
    @FXML
    private void onPdfErzeugen() {
       // String invoiceId = rechnungsNummerField.getText();
        String invoiceId = rechnungsNummerField.getText();

        
        String issued     = (DateIssued.getValue()    != null) ? DateIssued.getValue().toString()    : null;
        String delivered  = (DateDelivered.getValue() != null) ? DateDelivered.getValue().toString() : null;
        String due       = (DateDue.getValue()       != null) ? DateDue.getValue().toString()       : null;

        String customerId   = kundenNummerField.getText();
        String billerVatId  = "DE123456789"; 
        String recipientName = (customerCombo.getValue() != null) ? orEmpty(customerCombo.getValue().getName()) : orEmpty(customerCombo.getEditor().getText());
        String recipientVat  = "DE"; // typst

      // weiß noch nicht wie viele row es gibt 
        JSONArray items = new JSONArray();
        for (InvoiceLine line : leistungenTable.getItems()) {
            JSONObject item = new JSONObject()
                    .put("description", orEmpty(line.getDescription()))
                    .put("quantity", line.getQuantity())
                    .put("price",    line.getUnitPrice());
            items.put(item);
        }

        JSONObject recipientAddress = new JSONObject()
                .put("street",      streetField.getText())
                .put("city",        cityField.getText())
                .put("postal-code", postalCodeField.getText());

        JSONObject recipient = new JSONObject()
                .put("name",    recipientName)
                .put("address", recipientAddress)
                .put("vat-id",  recipientVat);

        JSONObject billerAddress = new JSONObject()
                .put("street",      "Musterstraße 1")
                .put("city",        "Musterstadt")
                .put("postal-code", "12345");

        JSONObject biller = new JSONObject()
                .put("name",      "Mein Unternehmen GmbH")
                .put("address",   billerAddress)
                .put("vat-id",    billerVatId)
                .put("iban",      "DE44500105175407324931")
                .put("bank-name", "Deutsche Bank");

        JSONObject invoice = new JSONObject()
                .put("language",   "de")
                .put("currency",   "€")
                .put("invoice-id", invoiceId)
                
                .put("issuing-date",  (issued    == null) ? JSONObject.NULL : issued)
                .put("delivery-date", (delivered == null) ? JSONObject.NULL : delivered)
                .put("due-date",      (due      == null) ? JSONObject.NULL : due)
                .put("customer-id",   customerId)
                .put("recipient",     recipient)
                .put("biller",        biller)
                .put("hourly-rate",   0)
                .put("items",         items)
                .put("vat",           0.19);

        // an typst weitergeben
        TypstGenerator.generateJson(invoice.toString());


    }

    
    private static String orEmpty(String empty) {
        return (empty == null) ? "" : empty;
    }

   /* private void recalcTotals() {
        for (InvoiceLine ln : leistungenTable.getItems()) {
            ln.setTotal(ln.getQuantity() * ln.getPrice());
        }
        leistungenTable.refresh();
    }   

    private String generateNextInvoiceNumber() throws SQLException {
        try (Connection conn = db.openConnection();
             Statement stmt = conn.createStatement();
             ResultSet resultSet = stmt.executeQuery("SELECT MAX(id) FROM invoice")) {
            int max = resultSet.next() ? resultSet.getInt(1) : 0;
            return "Re" + String.format("%04d", max + 1);
        }
    } */

    private void showError(String title, Exception ex) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
        errorAlert.setHeaderText(title);
        errorAlert.showAndWait();
    }
   
   

    

    
}
