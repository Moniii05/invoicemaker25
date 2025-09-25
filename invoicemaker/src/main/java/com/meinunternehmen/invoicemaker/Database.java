package com.meinunternehmen.invoicemaker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;



public final class Database {

    private final String url;

    public Database(String url) {
        this.url = url;
    }

    public void initSchema() throws SQLException {
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt  = conn.createStatement()) {
  
            stmt.execute("PRAGMA foreign_keys = ON;");   
       
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS customer (
                  id          INTEGER PRIMARY KEY AUTOINCREMENT,
                  name        TEXT    NOT NULL,
                  street      TEXT,
                  city        TEXT,
                  postal_code TEXT,
                  vat_id      TEXT,
                  iban        TEXT,
                  bank_name   TEXT
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS service (
                  id          INTEGER PRIMARY KEY AUTOINCREMENT,
                  description TEXT    NOT NULL,
                  unit_price       REAL    NOT NULL
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS invoice (
                  id             INTEGER PRIMARY KEY AUTOINCREMENT,
                  invoice_number TEXT UNIQUE,
                  customer_id    INTEGER NOT NULL REFERENCES customer(id),
                  issue_date     TEXT,
                  delivery_date  TEXT,
                  due_date       TEXT,
                  vat_rate       REAL
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS invoice_line (
                  id           INTEGER PRIMARY KEY AUTOINCREMENT,
                  invoice_id   INTEGER NOT NULL REFERENCES invoice(id),
                  service_id   INTEGER REFERENCES service(id),
                  quantity     INTEGER,
                  unit_price   REAL,
                  line_total   REAL
                );
            """);
         
        }
    }

    public Connection openConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON;");         
        }
        return conn;
    }
}
