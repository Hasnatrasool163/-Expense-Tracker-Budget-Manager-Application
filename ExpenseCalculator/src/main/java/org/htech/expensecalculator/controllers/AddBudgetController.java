package org.htech.expensecalculator.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.htech.expensecalculator.database.DBConnection;
import org.htech.expensecalculator.utilities.CallbackHandler;
import org.htech.expensecalculator.utilities.SessionManager;
import org.htech.expensecalculator.utilities.UtilityMethods;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;

public class AddBudgetController implements CallbackHandler {

    private Runnable onSuccessCallback;
    @FXML
    private Button cancelButton;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextField limitField;

    @FXML
    private Button saveButton;

    private HashMap<String, Integer> categoriesMap = new HashMap<>();

    @FXML
    public void initialize() {
        loadCategories();
        saveButton.setOnAction(event -> handleSave());
        cancelButton.setOnAction(event -> handleCancel());
    }

    private void loadCategories() {
        String query = "SELECT category_id, name FROM CATEGORY";

        try {
            Connection connection = DBConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String categoryName = rs.getString("name");
                int categoryId = rs.getInt("category_id");

                categoryComboBox.getItems().add(categoryName);
                categoriesMap.put(categoryName, categoryId);
            }
        } catch (SQLException e) {
            UtilityMethods.showPopupWarning("Error loading categories: " + e.getMessage());
        }
    }

    private void handleSave() {
        String category = categoryComboBox.getValue();
        String limit = limitField.getText();
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        if (category == null || category.isEmpty() || limit.isEmpty()) {
            UtilityMethods.showPopupWarning("Please fill all fields.");
            return;
        }

        double newLimit;
        try {
            newLimit = Double.parseDouble(limit);
        } catch (NumberFormatException e) {
            UtilityMethods.showPopupWarning("Please enter a valid limit amount.");
            return;
        }

        Integer categoryId = categoriesMap.get(category);
        if (categoryId == null) {
            UtilityMethods.showPopupWarning("Invalid category selected.");
            return;
        }

        try (Connection connection = DBConnection.getConnection()) {
            if (connection == null || connection.isClosed()) {
                UtilityMethods.showPopupWarning("Failed to connect to the database.");
                return;
            }

            connection.setAutoCommit(false);

            int userId = SessionManager.getCurrentUserId();

            String checkBudgetQuery = "SELECT BI.budget_item_id, BI.limit_amount, B.budget_id " +
                    "FROM BUDGET_ITEM BI " +
                    "INNER JOIN BUDGET B ON BI.budget_id = B.budget_id " +
                    "WHERE B.user_id = ? AND B.month = ? AND B.year = ? " +
                    "AND BI.category_id = ?";
            try (PreparedStatement checkBudgetPs = connection.prepareStatement(checkBudgetQuery)) {
                checkBudgetPs.setInt(1, userId);
                checkBudgetPs.setInt(2, month);
                checkBudgetPs.setInt(3, year);
                checkBudgetPs.setInt(4, categoryId);

                ResultSet resultSet = checkBudgetPs.executeQuery();

                if (resultSet.next()) {
                    int budgetItemId = resultSet.getInt("budget_item_id");
                    double existingLimit = resultSet.getDouble("limit_amount");
                    double updatedLimit = existingLimit + newLimit;

                    String updateBudgetItemQuery = "UPDATE BUDGET_ITEM SET limit_amount = ? WHERE budget_item_id = ?";
                    try (PreparedStatement updatePs = connection.prepareStatement(updateBudgetItemQuery)) {
                        updatePs.setDouble(1, updatedLimit);
                        updatePs.setInt(2, budgetItemId);

                        updatePs.executeUpdate();
                    }
                    UtilityMethods.showPopup("Budget updated successfully.");
                } else {
                    String budgetQuery = "INSERT INTO BUDGET (user_id, group_id, month, year, created_at) VALUES (?, NULL, ?, ?, NOW())";
                    try (PreparedStatement budgetPs = connection.prepareStatement(budgetQuery, Statement.RETURN_GENERATED_KEYS)) {
                        budgetPs.setInt(1, userId);
                        budgetPs.setInt(2, month);
                        budgetPs.setInt(3, year);

                        budgetPs.executeUpdate();
                        ResultSet generatedKeys = budgetPs.getGeneratedKeys();
                        if (!generatedKeys.next()) {
                            throw new SQLException("Failed to retrieve budget ID.");
                        }
                        int budgetId = generatedKeys.getInt(1);

                        String budgetItemQuery = "INSERT INTO BUDGET_ITEM (budget_id, category_id, limit_amount) VALUES (?, ?, ?)";
                        try (PreparedStatement budgetItemPs = connection.prepareStatement(budgetItemQuery)) {
                            budgetItemPs.setInt(1, budgetId);
                            budgetItemPs.setInt(2, categoryId);
                            budgetItemPs.setDouble(3, newLimit);

                            budgetItemPs.executeUpdate();
                        }
                    }
                    UtilityMethods.showPopup("Budget added successfully.");
                }
            }
            connection.commit();
            clearFields();
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            ((Stage) saveButton.getScene().getWindow()).close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void handleCancel() {
        clearFields();
    }

    private void clearFields() {
        categoryComboBox.setValue(null);
        limitField.clear();
    }

    @Override
    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }
}
