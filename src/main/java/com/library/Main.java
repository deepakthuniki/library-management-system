package com.library;

import com.library.ui.ConsoleUI;
import com.library.util.DatabaseConnection;
import com.library.util.DataSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            DataSeeder.seedIfEmpty();
        } catch (SQLException e) {
            log.error("Failed to seed database. Check DB connection settings.", e);
            System.out.println("FATAL: Could not connect to database. See logs/library-app.log for details.");
            return;
        }

        try {
            new ConsoleUI().start();
        } finally {
            DatabaseConnection.shutdown();
        }
    }
}
