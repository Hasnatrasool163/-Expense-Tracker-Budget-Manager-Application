package org.htech.expensecalculator.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.htech.expensecalculator.database.DBConnection;
import org.htech.expensecalculator.utilities.CallbackHandler;
import org.htech.expensecalculator.utilities.SessionManager;
import org.htech.expensecalculator.utilities.UtilityMethods;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;

public class AddGroupBudgetController implements CallbackHandler {

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

    private String groupName = SessionManager.getCurrentGroupName();

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

        if (category == null || category.isEmpty() || limit.isEmpty()) {
            UtilityMethods.showPopupWarning("Please fill all fields.");
            return;
        }
        if (groupName == null) {
            UtilityMethods.showPopupWarning("Please select a group.");
            return;
        }

        try {
            Double.parseDouble(limit);
        } catch (NumberFormatException e) {
            UtilityMethods.showPopupWarning("Please enter a valid limit amount.");
            return;
        }

        Integer categoryId = categoriesMap.get(category);
        if (categoryId == null) {
            UtilityMethods.showPopupWarning("Invalid category selected.");
            return;
        }

        try {
            Connection connection = DBConnection.getConnection();
            if (connection == null || connection.isClosed()) {
                UtilityMethods.showPopupWarning("Failed to connect to the database.");
                return;
            }

            connection.setAutoCommit(false);

            int userId = SessionManager.getCurrentUserId();
            int groupId = getGroupIdByName(groupName);

            String checkBudgetQuery = "SELECT BI.budget_item_id, BI.limit_amount " +
                    "FROM BUDGET B " +
                    "INNER JOIN BUDGET_ITEM BI ON B.budget_id = BI.budget_id " +
                    "WHERE B.user_id = ? AND B.group_id = ? AND B.month = ? AND B.year = ? " +
                    "AND BI.category_id = ?";

            try (PreparedStatement checkBudgetPs = connection.prepareStatement(checkBudgetQuery)) {
                checkBudgetPs.setInt(1, userId);
                checkBudgetPs.setInt(2, groupId);
                checkBudgetPs.setInt(3, month);
                checkBudgetPs.setInt(4, LocalDate.now().getYear());
                checkBudgetPs.setInt(5, categoryId);

                ResultSet resultSet = checkBudgetPs.executeQuery();

                if (resultSet.next()) {
                    int budgetItemId = resultSet.getInt("budget_item_id");
                    double existingLimit = resultSet.getDouble("limit_amount");
                    double newLimit = existingLimit + Double.parseDouble(limit);

                    String updateBudgetItemQuery = "UPDATE BUDGET_ITEM SET limit_amount = ? WHERE budget_item_id = ?";
                    try (PreparedStatement updateBudgetItemPs = connection.prepareStatement(updateBudgetItemQuery)) {
                        updateBudgetItemPs.setDouble(1, newLimit);
                        updateBudgetItemPs.setInt(2, budgetItemId);

                        int rowsUpdated = updateBudgetItemPs.executeUpdate();
                        if (rowsUpdated == 0) {
                            throw new SQLException("Failed to update budget item.");
                        }
                    }
                    UtilityMethods.showPopup("Budget updated successfully.");
                } else {
                    String insertBudgetQuery = "INSERT INTO BUDGET (user_id, group_id, month, year, created_at) VALUES (?, ?, ?, ?, NOW())";
                    try (PreparedStatement insertBudgetPs = connection.prepareStatement(insertBudgetQuery, Statement.RETURN_GENERATED_KEYS)) {
                        insertBudgetPs.setInt(1, userId);
                        insertBudgetPs.setInt(2, groupId);
                        insertBudgetPs.setInt(3, month);
                        insertBudgetPs.setInt(4, LocalDate.now().getYear());

                        int rowsInserted = insertBudgetPs.executeUpdate();
                        if (rowsInserted == 0) {
                            throw new SQLException("Failed to insert budget.");
                        }

                        ResultSet generatedKeys = insertBudgetPs.getGeneratedKeys();
                        if (!generatedKeys.next()) {
                            throw new SQLException("Failed to retrieve budget ID.");
                        }
                        int budgetId = generatedKeys.getInt(1);

                        String insertBudgetItemQuery = "INSERT INTO BUDGET_ITEM (budget_id, category_id, limit_amount) VALUES (?, ?, ?)";
                        try (PreparedStatement insertBudgetItemPs = connection.prepareStatement(insertBudgetItemQuery)) {
                            insertBudgetItemPs.setInt(1, budgetId);
                            insertBudgetItemPs.setInt(2, categoryId);
                            insertBudgetItemPs.setDouble(3, Double.parseDouble(limit));

                            int itemRowsInserted = insertBudgetItemPs.executeUpdate();
                            if (itemRowsInserted == 0) {
                                throw new SQLException("Failed to insert budget item.");
                            }
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private int getGroupIdByName(String groupName) {
        String query = "SELECT group_id FROM GROUPS WHERE name = ?";
        try {Connection connection = DBConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);

            ps.setString(1, groupName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("group_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
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
