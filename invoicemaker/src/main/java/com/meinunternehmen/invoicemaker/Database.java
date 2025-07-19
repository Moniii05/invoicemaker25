package com.meinunternehmen.invoicemaker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

	 // JDBC-URL zur lokalen SQLite-Datei
	  private static final String URL = "jdbc:sqlite:invoicemaker.db";

	  public static void init() throws SQLException {
	    Path dbFile = Paths.get("invoicemaker.db");
	    boolean existed = Files.exists(dbFile);

	    // Verbindung öffnen (legt Datei an, falls sie nicht da ist)
	    try (Connection conn = DriverManager.getConnection(URL)) {
	      if (!existed) {
	        // Tabellen anlegen
	        try (Statement stmt = conn.createStatement()) {
	          stmt.executeUpdate("""
	            CREATE TABLE customer (
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
	            CREATE TABLE service (
	              id          INTEGER PRIMARY KEY AUTOINCREMENT,
	              description TEXT    NOT NULL,
	              price       REAL    NOT NULL
	            );
	            """);

	          stmt.executeUpdate("""
	            CREATE TABLE invoice (
	              id             INTEGER PRIMARY KEY AUTOINCREMENT,
	              invoice_number TEXT    UNIQUE,
	              customer_id    INTEGER NOT NULL REFERENCES customer(id),
	              issue_date     TEXT,
	              delivery_date  TEXT,
	              due_date       TEXT,
	              vat_rate       REAL
	            );
	            """);

	          stmt.executeUpdate("""
	            CREATE TABLE invoice_line (
	              id           INTEGER PRIMARY KEY AUTOINCREMENT,
	              invoice_id   INTEGER NOT NULL REFERENCES invoice(id),
	              service_id   INTEGER REFERENCES service(id),
	              quantity     INTEGER,
	              duration_min INTEGER,
	              unit_price   REAL,
	              line_total   REAL
	            );
	            """);
	        }
	      }       
	    }
	  }
	  
	  /** Liefert Dir jederzeit eine offene Connection zur DB. */
	    public static Connection getConnection() throws SQLException {
	        return DriverManager.getConnection(URL);
	    } 
}
