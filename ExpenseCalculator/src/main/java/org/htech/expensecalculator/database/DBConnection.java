package org.htech.expensecalculator.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/expense_tracker";
    private static final String USER = "test_user";
    private static final String PASSWORD = "your_password";
    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Connection established to MySQL database: ");
            } catch (SQLException | ClassNotFoundException e) {
                System.err.println("MySQL DBConnection failed: " + e.getMessage());
            }
        }
        return connection;
    }
}
