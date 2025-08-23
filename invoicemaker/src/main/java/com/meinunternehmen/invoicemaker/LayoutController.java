package com.meinunternehmen.invoicemaker;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.NumberStringConverter;

import java.sql.*;
import java.time.LocalDate;

public class LayoutController {

    @FXML private ComboBox<Customer> customerCombo;
    @FXML private TextField kundenNummerField;

    // Adress-Felder (Empfänger)
    @FXML private TextField streetField;
    @FXML private TextField postalCodeField;
    @FXML private TextField cityField;

    // Positionstabelle
    @FXML private TableView<InvoiceLine> leistungenTable;
    @FXML private TableColumn<InvoiceLine, Integer> colNummer;
    @FXML private TableColumn<InvoiceLine, String>  colDescription;
    @FXML private TableColumn<InvoiceLine, Number>  colQuantity;
    @FXML private TableColumn<InvoiceLine, Number>  colPrice;
    @FXML private TableColumn<InvoiceLine, Number>  colTotal;

    // Rechnungsdaten
    @FXML private DatePicker dpIssue, dpDelivery, dpDue;
    @FXML private TextField rechnungsNummerField;

    @FXML
    public void initialize() throws SQLException {
        // --- 0) Rechnungsnummer & ComboBox ---
        rechnungsNummerField.setEditable(true);
        rechnungsNummerField.setText(generateNextInvoiceNumber());
        customerCombo.setEditable(true);

        // --- 1) StringConverter für ComboBox<Customer> ---
        customerCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Customer c) {
                return c == null ? "" : c.getName();
            }
            @Override public Customer fromString(String text) {
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
                    streetField.setText(sel.getStreet());
                    postalCodeField.setText(sel.getZip());
                    cityField.setText(sel.getCity());
                }
            }
        });

        // --- 3) Kunden aus DB nachladen ---
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM customer")) {
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
        colNummer.setCellValueFactory(c -> c.getValue().numberProperty().asObject());

        colDescription.setCellValueFactory(c -> c.getValue().descriptionProperty());
        colDescription.setCellFactory(tc -> new EditingCell<>(new DefaultStringConverter()));
        colDescription.setOnEditCommit(evt -> {
            evt.getRowValue().setDescription(evt.getNewValue());
            leistungenTable.refresh();
        });

        colQuantity.setCellValueFactory(c -> c.getValue().quantityProperty());
        colQuantity.setCellFactory(tc -> new EditingCell<>(new NumberStringConverter()));
        colQuantity.setOnEditCommit(evt -> {
            evt.getRowValue().setQuantity(evt.getNewValue().doubleValue());
            recalcTotals();
        });

        colPrice.setCellValueFactory(c -> c.getValue().priceProperty());
        colPrice.setCellFactory(tc -> new EditingCell<>(new NumberStringConverter()));
        colPrice.setOnEditCommit(evt -> {
            evt.getRowValue().setPrice(evt.getNewValue().doubleValue());
            recalcTotals();
        });

        colTotal.setCellValueFactory(c -> c.getValue().totalProperty());

        leistungenTable.setEditable(true);
        leistungenTable.getItems().addListener((ListChangeListener<InvoiceLine>) change -> recalcTotals());
        leistungenTable.getItems().add(new InvoiceLine(1, "", 1, 0.0));

        // --- 5) Datums-Defaults setzen ---
        LocalDate today = LocalDate.now();
        dpIssue.setValue(today);
        dpDelivery.setValue(today);
        dpDue.setValue(today.plusDays(14));
    }

    @FXML
    private void onNewCustomer() {
        // ggf. Dialog zum Anlegen eines Kunden
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
    private void onPdfErzeugen() {
        // --- Rechnungs-/Kopfwerte abholen ---
        String invoiceId = rechnungsNummerField.getText();
        String issue     = (dpIssue.getValue()    != null) ? dpIssue.getValue().toString()    : null;
        String delivery  = (dpDelivery.getValue() != null) ? dpDelivery.getValue().toString() : null;
        String due       = (dpDue.getValue()      != null) ? dpDue.getValue().toString()      : null;

        String customerId = kundenNummerField.getText();

        // --- Absender (Biller) festlegen (später gern aus GUI/Config laden) ---
        String billerVatRaw = "DE123456789";
        String billerVat    = sanitizeVat(billerVatRaw, "DE");
        String billerPrefix = countryPrefix(billerVat, "DE"); // z.B. "DE"

        // --- Empfängername + Empfänger-USt-ID (kann leer sein) ---
        String recipientName = (customerCombo.getValue() != null)
                ? nvl(customerCombo.getValue().getName())
                : nvl(customerCombo.getEditor().getText());
        String recipientVatRaw = (customerCombo.getValue() != null)
                ? nvl(customerCombo.getValue().getVatId())
                : "";

        // Sanitisieren: min. Länderpräfix damit Typst nicht bei slice(0,2) crasht
        String recipientVat = sanitizeVat(recipientVatRaw, billerPrefix);

        // --- EIN Datum für alle Positionen: Delivery > Issue > Heute ---
        String rowDate = (dpDelivery.getValue() != null ? dpDelivery.getValue()
                : (dpIssue.getValue() != null ? dpIssue.getValue()
                : java.time.LocalDate.now())).toString(); // "YYYY-MM-DD"

        // --- Items als JSON ---
        StringBuilder itemsSb = new StringBuilder();
        for (int i = 0; i < leistungenTable.getItems().size(); i++) {
            InvoiceLine ln = leistungenTable.getItems().get(i);
            if (i > 0) itemsSb.append(',');
            itemsSb.append("{")
                  .append("\"date\":\"").append(esc(rowDate)).append("\",")
                  .append("\"description\":\"").append(esc(nvl(ln.getDescription()))).append("\",")
                  .append("\"quantity\":").append(ln.getQuantity()).append(",")
                  .append("\"dur-min\":").append(0).append(",") // keine Zeitabrechnung
                  .append("\"price\":").append(ln.getPrice())
                  .append("}");
        }
        String itemsJson = itemsSb.toString();

        // --- Gesamtes JSON-Dokument bauen (ohne externe Lib) ---
        StringBuilder json = new StringBuilder(2048);
        json.append("{");
        propStr(json, "language",   "de", true);
        propStr(json, "currency",   "€",  true);
        propStr(json, "invoice-id", invoiceId, true);
        propOpt(json, "issuing-date",  issue,    true);
        propOpt(json, "delivery-date", delivery, true);
        propOpt(json, "due-date",      due,      true);
        propStr(json, "customer-id",   customerId, true);

        // recipient
        json.append("\"recipient\":{");
        propStr(json, "name", recipientName, true);
        json.append("\"address\":{");
        propStr(json, "street",      streetField.getText(), true);
        propStr(json, "city",        cityField.getText(),   true);
        propStr(json, "postal-code", postalCodeField.getText(), false);
        json.append("},");
        propStr(json, "vat-id", recipientVat, false); // min. 2 Zeichen dank Fallback; wird NICHT gedruckt
        json.append("},");

        // biller
        json.append("\"biller\":{");
        propStr(json, "name", "Mein Unternehmen GmbH", true);
        json.append("\"address\":{");
        propStr(json, "street",      "Musterstraße 1", true);
        propStr(json, "city",        "Musterstadt",    true);
        propStr(json, "postal-code", "12345",          false);
        json.append("},");
        propStr(json, "vat-id",   billerVat, true); // wird gedruckt
        propStr(json, "iban",     "DE44500105175407324931", true);
        propStr(json, "bank-name","Deutsche Bank", false);
        json.append("},");

        // Sicherheit: kein Zeitpreismodus aktivieren
        json.append("\"hourly-rate\":").append(0).append(",");

        // items & vat
        json.append("\"items\":[").append(itemsJson).append("],");
        json.append("\"vat\":0.19");
        json.append("}");

        // --- PDF erzeugen ---
        TypstGenerator.generateJson(json.toString());
    }

    
    

    // ----------------- Hilfsfunktionen -----------------

    /** Null-sicherer String */
    private static String nvl(String s) { return (s == null) ? "" : s; }

    /** JSON-escape für Strings */
    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** String-Property: null/leer -> als "" schreiben (nie null) */
    private static void propStr(StringBuilder sb, String key, String value, boolean trailingComma) {
        sb.append("\"").append(key).append("\":")
          .append("\"").append(esc(nvl(value))).append("\"");
        if (trailingComma) sb.append(',');
    }

    /** Optionale Property (z. B. Datum): null bleibt null, sonst String */
    private static void propOpt(StringBuilder sb, String key, String value, boolean trailingComma) {
        sb.append("\"").append(key).append("\":");
        if (value == null) sb.append("null");
        else sb.append("\"").append(esc(value)).append("\"");
        if (trailingComma) sb.append(',');
    }
    
    
    private static String countryPrefix(String vat, String fallback) {
        String v = nvl(vat).replaceAll("\\s+", "");
        if (v.length() >= 2) return v.substring(0, 2);
        return fallback;
    }
    
    private static String sanitizeVat(String vatRaw, String defaultPrefix) {
        String v = nvl(vatRaw).replaceAll("\\s+", "");
        if (v.length() < 2) return defaultPrefix;
        return v;
    }

    private void recalcTotals() {
        for (InvoiceLine ln : leistungenTable.getItems()) {
            ln.setTotal(ln.getQuantity() * ln.getPrice());
        }
        leistungenTable.refresh();
    }

    private String generateNextInvoiceNumber() throws SQLException {
        try (Connection c = Database.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT MAX(id) FROM invoice")) {
            int max = rs.next() ? rs.getInt(1) : 0;
            return "Re" + String.format("%04d", max + 1);
        }
    }
    
    
   
 
    
    
    
    
}
