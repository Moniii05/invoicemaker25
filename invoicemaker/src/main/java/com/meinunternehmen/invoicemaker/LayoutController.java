package com.meinunternehmen.invoicemaker;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.NumberStringConverter;

import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.Map;

public class LayoutController {

    @FXML private ComboBox<Customer> customerCombo;
    @FXML private TextField kundenNummerField;

    // die drei neuen Adress-Felder
    @FXML private TextField streetField;
    @FXML private TextField postalCodeField;
    @FXML private TextField cityField;

    @FXML private TableView<InvoiceLine> leistungenTable;
    @FXML private TableColumn<InvoiceLine,Integer> colNummer;
    @FXML private TableColumn<InvoiceLine,String>  colDescription;
    @FXML private TableColumn<InvoiceLine,Number>  colQuantity;
    @FXML private TableColumn<InvoiceLine,Number>  colPrice;
    @FXML private TableColumn<InvoiceLine,Number>  colTotal;

    @FXML private DatePicker dpIssue, dpDelivery, dpDue;
    @FXML private TextField rechnungsNummerField;

    @FXML
    public void initialize() throws SQLException {
        // --- 0) Rechnungsnummer & ComboBox ---
        rechnungsNummerField.setEditable(true);
        rechnungsNummerField.setText(generateNextInvoiceNumber());
        customerCombo.setEditable(true);

        // --- 1) StringConverter für ComboBox<Customer> ---
        customerCombo.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer c) {
                return c == null ? "" : c.getName();
            }
            @Override
            public Customer fromString(String text) {
                for (Customer c : customerCombo.getItems()) {
                    if (c.getName().equals(text)) return c;
                }
                return new Customer(0, text, "", "", "", "");
            }
        });

        // --- 2) Wenn Editor-Focus verloren, Felder füllen ---
        customerCombo.getEditor().focusedProperty().addListener((obs, wasFoc, isNowFoc) -> {
            if (!isNowFoc) {
                Customer sel = customerCombo.getConverter()
                                            .fromString(customerCombo.getEditor().getText().trim());
                customerCombo.setValue(sel);
                if (sel.getId() > 0) {
                    kundenNummerField.setText(String.valueOf(sel.getId()));
                    // jetzt in die Einzel-Felder aufteilen:
                    streetField     .setText(sel.getStreet());
                    postalCodeField .setText(sel.getZip());
                    cityField       .setText(sel.getCity());
                }
            }
        });

        // --- 3) Kunden aus DB nachladen ---
        try ( Connection c = Database.getConnection();
              Statement s = c.createStatement();
              ResultSet rs = s.executeQuery("SELECT * FROM customer") ) {
            while (rs.next()) {
                customerCombo.getItems().add(new Customer(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("street"),
                    rs.getString("city"),
                    rs.getString("zip"),
                    rs.getString("vat_id")
                ));
            }
        }

        // --- 4) Tabelle konfigurieren ---
        colNummer
            .setCellValueFactory(c -> c.getValue().numberProperty().asObject());

        colDescription
            .setCellValueFactory(c -> c.getValue().descriptionProperty());
        colDescription.setCellFactory(tc -> new EditingCell<>(new DefaultStringConverter()));
        colDescription.setOnEditCommit(evt -> {
            evt.getRowValue().setDescription(evt.getNewValue());
            leistungenTable.refresh();
        });

        colQuantity
            .setCellValueFactory(c -> c.getValue().quantityProperty());
        colQuantity.setCellFactory(tc -> new EditingCell<>(new NumberStringConverter()));
        colQuantity.setOnEditCommit(evt -> {
            evt.getRowValue().setQuantity(evt.getNewValue().doubleValue());
            recalcTotals();
        });

        colPrice
            .setCellValueFactory(c -> c.getValue().priceProperty());
        colPrice.setCellFactory(tc -> new EditingCell<>(new NumberStringConverter()));
        colPrice.setOnEditCommit(evt -> {
            evt.getRowValue().setPrice(evt.getNewValue().doubleValue());
            recalcTotals();
        });

        colTotal
            .setCellValueFactory(c -> c.getValue().totalProperty());

        leistungenTable.setEditable(true);
        leistungenTable.getItems().addListener(
            (ListChangeListener<InvoiceLine>) change -> recalcTotals());
        leistungenTable.getItems().add(new InvoiceLine(1, "", 1, 0.0));

        // --- 5) Datum setzen ---
        LocalDate today = LocalDate.now();
        dpIssue   .setValue(today);
        dpDelivery.setValue(today);
        dpDue     .setValue(today.plusDays(14));
    }

    @FXML
    private void onNewCustomer() {
        // Dialog o.Ä.
    }

    @FXML
    private void onAddRow() {
        if (leistungenTable.getEditingCell() != null) {
            leistungenTable.edit(-1, null);
        }
        int next = leistungenTable.getItems().size() + 1;
        leistungenTable.getItems().add(new InvoiceLine(next, "", 1, 0.0));
        leistungenTable.scrollTo(next - 1);
    }

    @FXML
    private void onPdfErzeugen() throws Exception {
        String invoiceId = rechnungsNummerField.getText();
        String issue     = dpIssue.getValue().toString();
        String delivery  = dpDelivery.getValue().toString();
        String due       = dpDue.getValue().toString();

        Customer cust = customerCombo.getValue();
        String street = streetField.getText().trim();
        String postal = postalCodeField.getText().trim();
        String city   = cityField.getText().trim();

        Map<String,String> recipientAddress = Map.of(
            "street",      street,
            "postal-code", postal,
            "city",        city
        );
        Map<String,Object> recipient = Map.of(
            "name",    cust.getName(),
            "address", recipientAddress,
            "vat-id",  cust.getVatId()
        );

        Map<String,Object> biller = Map.of(
            "name",      "Mein Unternehmen GmbH",
            "address",   Map.of(
                "street",      "Musterstraße 1",
                "postal-code", "12345",
                "city",        "Musterstadt"
            ),
            "vat-id",    "DE123456789",
            "iban",      "DE89370400440532013000",
            "bank-name", "Berliner Sparkasse"
        );

        var data = Map.<String,Object>of(
            "language",      "de",
            "invoice-id",    invoiceId,
            "issuing-date",  issue,
            "delivery-date", delivery,
            "due-date",      due,
            "customer-id",   kundenNummerField.getText(),
            "items",         leistungenTable.getItems().stream()
                                            .map(InvoiceLine::toMap)
                                            .toList(),
            "recipient",     recipient,
            "biller",        biller
        );

        TypstGenerator.generate("main.typ", data, Path.of("invoice.pdf"));
        System.out.println(data);

    }

    private void recalcTotals() {
        for (InvoiceLine ln : leistungenTable.getItems()) {
            ln.setTotal(ln.getQuantity() * ln.getPrice());
        }
        leistungenTable.refresh();
    }

    private String generateNextInvoiceNumber() throws SQLException {
        try ( Connection c = Database.getConnection();
              Statement s = c.createStatement();
              ResultSet rs = s.executeQuery("SELECT MAX(id) FROM invoice") ) {
            int max = rs.next() ? rs.getInt(1) : 0;
            return "Re" + String.format("%04d", max + 1);
        }
    }
}

