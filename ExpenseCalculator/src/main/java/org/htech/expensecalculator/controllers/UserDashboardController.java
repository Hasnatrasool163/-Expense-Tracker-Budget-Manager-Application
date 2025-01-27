package org.htech.expensecalculator.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.htech.expensecalculator.database.DBConnection;
import org.htech.expensecalculator.modal.UserBudgetData;
import org.htech.expensecalculator.modal.UserCategoryTotals;
import org.htech.expensecalculator.utilities.SessionManager;
import org.htech.expensecalculator.utilities.UtilityMethods;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ResourceBundle;

public class UserDashboardController implements Initializable {

    @FXML
    private MenuItem budgetMenuItem;

    @FXML
    private MenuItem chartMenuItem;

    @FXML
    private MenuItem groupMenuItem;

    @FXML
    private MenuItem groupMenuItem2;

    @FXML
    private MenuItem profileMenuItem;

    @FXML
    private MenuItem todoMenuItem;

    @FXML
    private MenuItem todoMenuItem2;

    @FXML
    private MenuItem transactionMenuItem;

    @FXML
    private MenuItem transactionMenuItem2;

    @FXML
    private MenuItem scheduleMenuItem;

    @FXML
    private MenuItem logoutMenuItem;

    @FXML
    private Button logoutBtn;

    @FXML
    private Label usernameLbl;

    @FXML
    private TableColumn<UserCategoryTotals, String> totalsCategoryColumn;

    @FXML
    private TableColumn<UserCategoryTotals, Double> totalsAmountIncomeColumn;

    @FXML
    private TableColumn<UserCategoryTotals, String> dateColumn;

    @FXML
    private TableView<UserCategoryTotals> totalsTable;

    @FXML
    private ListView<UserBudgetData> budgetListView;

    @FXML
    private Button prevMonthBtn;

    @FXML
    private Button nextMonthBtn;

    @FXML
    private ComboBox<String> monthYearComboBox;

    @FXML
    private TextField balanceField;

    private BigDecimal currentBalance;

    private LocalDate currentDate = LocalDate.now();

    private final ObservableList<UserBudgetData> budgetList = FXCollections.observableArrayList();
    private final ObservableList<UserCategoryTotals> totalList = FXCollections.observableArrayList();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        usernameLbl.setText(SessionManager.getCurrentUserName());
        setButtonActions();
        populateMonthYearComboBox();
        monthYearComboBox.setOnAction(event -> updateDataBasedOnSelection());
        prevMonthBtn.setOnAction(event -> navigateToPreviousMonth());
        nextMonthBtn.setOnAction(event -> navigateToNextMonth());
        loadUserBalance();
        setUpBalanceFieldListener();
        initializeTotalsTable();
        loadUserTotals();
        loadBudgetTableData();
        initializeBudgetListView();
        usernameLbl.requestFocus();
        Platform.runLater(() -> usernameLbl.requestFocus());
    }

    private void setUpBalanceFieldListener(){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Balance");
        MenuItem saveItem = new MenuItem("Save Balance");
        contextMenu.getItems().addAll(editItem,saveItem);
        balanceField.setContextMenu(contextMenu);
        editItem.setOnAction(event -> balanceField.setEditable(true));
        saveItem.setOnAction(event -> {
            balanceField.setEditable(false);
            String updatedBalanceStr = balanceField.getText();

            try {
                BigDecimal updatedBalance = new BigDecimal(updatedBalanceStr);

                if (!updatedBalance.equals(currentBalance)) {
                    saveUpdatedBalance();
                } else {
                    UtilityMethods.showPopup("Balance remains unchanged.");
                }
            } catch (NumberFormatException e) {
                UtilityMethods.showPopupWarning("Invalid balance format.");
            }
        });
        balanceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                balanceField.setText(oldValue);
            }
        });
        balanceField.setOnAction(event -> {
            balanceField.setEditable(false);
            saveItem.fire();
        });
    }

    private void loadUserBalance() {
        String query = "SELECT balance FROM USERS WHERE user_id = ?";
        try {Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query);

            ps.setInt(1, SessionManager.getCurrentUserId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal balance = rs.getBigDecimal("balance");
                    balanceField.setText(balance.toPlainString());
                    currentBalance = balance;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveUpdatedBalance() {
        String updatedBalanceStr = balanceField.getText();
        try {
            BigDecimal updatedBalance = new BigDecimal(updatedBalanceStr);
            String updateQuery = "UPDATE USERS SET balance = ? WHERE user_id = ?";
            Connection connection = DBConnection.getConnection();
                 PreparedStatement ps = connection.prepareStatement(updateQuery);

                ps.setBigDecimal(1, updatedBalance);
                ps.setInt(2, SessionManager.getCurrentUserId());
                ps.executeUpdate();

                UtilityMethods.showPopup("Balance updated successfully.");
                currentBalance = updatedBalance;
        } catch (NumberFormatException e) {
            UtilityMethods.showPopupWarning("Invalid balance format.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeBudgetListView() {
        budgetListView.setItems(budgetList);
        budgetListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(UserBudgetData item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox card = createBudgetCard(item);
                    setGraphic(card);
                }
            }
        });
    }

    private HBox createBudgetCard(UserBudgetData item) {
        HBox card = new HBox();
        card.setSpacing(10);
        card.setStyle("-fx-padding: 10; -fx-background-color: #f8f8f8; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");
        card.setPrefHeight(35);

        VBox detailsBox = new VBox();
        detailsBox.setSpacing(3);

        Label categoryLabel = new Label(item.getCategory());
        categoryLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label limitLabel = new Label(String.format("%.0f $", item.getLimit()));
        limitLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #555;");
        TextField limitTextField = new TextField(String.valueOf(item.getLimit()));
        limitTextField.setStyle("-fx-font-size: 16px;");
        limitTextField.setVisible(false);

        limitLabel.setOnMouseClicked(event -> {
            String selectedMonthYear = monthYearComboBox.getSelectionModel().getSelectedItem();
            if (selectedMonthYear != null) {
                String[] parts = selectedMonthYear.split(" ");
                String monthName = parts[0];
                int monthValue = Month.valueOf(monthName.toUpperCase()).getValue();
                if (!(monthValue == LocalDate.now().getMonthValue())) {
                    UtilityMethods.showPopupWarning("Cannot update past budget");
                    return;
                }
            }
            boolean count = event.getClickCount()==2;
            if(count){
                limitLabel.setVisible(false);
                limitTextField.setVisible(true);
                limitTextField.requestFocus();
            }
        });

        limitTextField.setOnAction(event -> commitLimitEdit(limitTextField, limitLabel, item));
        limitTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                commitLimitEdit(limitTextField, limitLabel, item);
            }
        });

        detailsBox.getChildren().addAll(categoryLabel, limitLabel, limitTextField);

        ProgressBar progressBar = new ProgressBar();
        double progressWidth = item.getSpend() == 0 ? 200 : (item.getSpend() / item.getLimit()) * 200;
        Color barColor;
        if (item.getSpend() == 0) {
            barColor = Color.GRAY;
        } else {
            barColor = item.getSpend() <= item.getLimit() ? Color.LIGHTGREEN : Color.LIGHTCORAL;
        }
        progressBar.setProgress(item.getSpend() / item.getLimit());
        progressBar.setPrefWidth(progressWidth);
        progressBar.setPrefWidth(250);
        progressBar.setStyle("-fx-accent: " + barColor.toString().replace("0x", "#") + ";");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label spendLabel = new Label(String.format("%.0f $     %.0f%%", item.getSpend(),
                (item.getSpend() / item.getLimit()) * 100));
        spendLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #555;");

        VBox progressBox = new VBox(progressBar, spendLabel);
        progressBox.setSpacing(5);

        card.getChildren().addAll(detailsBox, spacer, progressBox);

        return card;
    }

    private void commitLimitEdit(TextField limitTextField, Label limitLabel, UserBudgetData item) {
        try {
            double newLimit = Double.parseDouble(limitTextField.getText());
            if (item.getLimit() == newLimit) {
                limitLabel.setVisible(true);
                limitTextField.setVisible(false);
                return;
            }
            if (newLimit > 0) {
                item.setLimit(newLimit);
                limitLabel.setText(String.format("%.0f $", newLimit));
                updateLimitInDatabase(item);
                budgetListView.refresh();
            } else {
                UtilityMethods.showPopupWarning("Amount must be greater than 0");
            }
        } catch (NumberFormatException e) {
            UtilityMethods.showPopupWarning("Invalid amount entered. Please enter a valid number.");
        } finally {
            limitLabel.setVisible(true);
            limitTextField.setVisible(false);
        }
    }

    private void populateMonthYearComboBox() {
        ObservableList<String> monthYearList = FXCollections.observableArrayList();
        LocalDate currentDate = LocalDate.now();
        for (int i = 0; i < 12; i++) {
            LocalDate date = currentDate.minusMonths(i);
            String monthYear = date.getMonth() + " " + date.getYear();
            monthYearList.add(monthYear);
        }
        monthYearComboBox.setItems(monthYearList);
        monthYearComboBox.getSelectionModel().selectFirst();
    }

    private void navigateToPreviousMonth() {
        currentDate = currentDate.minusMonths(1);
        updateData();
    }

    private void navigateToNextMonth() {
        currentDate = currentDate.plusMonths(1);
        updateData();
    }
    private void updateData() {
        String selectedMonthYear = currentDate.getMonth() + " " + currentDate.getYear();
        monthYearComboBox.getSelectionModel().select(selectedMonthYear);

        loadUserTotals();
        loadBudgetTableData();
    }

    private void updateDataBasedOnSelection() {
        String selectedMonthYear = monthYearComboBox.getSelectionModel().getSelectedItem();
        if (selectedMonthYear != null) {
            String[] parts = selectedMonthYear.split(" ");
            String monthName = parts[0];
            int year = Integer.parseInt(parts[1]);

            int monthValue = Month.valueOf(monthName.toUpperCase()).getValue();
            currentDate = LocalDate.of(year, monthValue, 1);

            loadUserTotals();
            loadBudgetTableData();
        }
    }

    private void initializeTotalsTable() {
        totalsCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        totalsAmountIncomeColumn.setCellValueFactory(new PropertyValueFactory<>("netAmount"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        totalsAmountIncomeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("income-cell", "expense-cell");
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item >= 0) {
                        setText(String.format("$+%.2f", item));
                        if (!getStyleClass().contains("income-cell")) {
                            getStyleClass().add("income-cell");
                        }
                    } else {
                        setText(String.format("$%.2f", item));
                        if (!getStyleClass().contains("expense-cell")) {
                            getStyleClass().add("expense-cell");
                        }
                    }
                }
            }
        });
        totalsTable.setItems(totalList);
    }

    private void setButtonActions() {
        budgetMenuItem.setOnAction(event ->
                UtilityMethods.switchToScene("AddBudget", () -> {
                    loadBudgetTableData();
                    loadUserTotals();
                })
        );
        transactionMenuItem.setOnAction(event ->
                UtilityMethods.switchToScene("AddTransaction", () -> {
                    loadUserBalance();
                    loadBudgetTableData();
                    loadUserTotals();
                })
        );
        transactionMenuItem2.setOnAction(event -> {
            loadBudgetTableData();
            loadUserTotals();
        });
        groupMenuItem.setOnAction(event -> UtilityMethods.switchToScene("AddGroup"));
        groupMenuItem2.setOnAction(event -> {
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            stage.setIconified(true);
            UtilityMethods.switchToScene("GroupSelection");
        });
        todoMenuItem.setOnAction(event -> UtilityMethods.switchToScene("ManageTodoList"));
        todoMenuItem2.setOnAction(event -> UtilityMethods.switchToScene("UserViewTodo"));
        profileMenuItem.setOnAction(event -> UtilityMethods.switchToScene("ViewProfile"));
        chartMenuItem.setOnAction(event -> UtilityMethods.switchToScene("UserDashboardChart"));
        scheduleMenuItem.setOnAction(event -> UtilityMethods.switchToScene("SchedulePlanner"));
        logoutMenuItem.setOnAction(event -> UtilityMethods.switchToScene(logoutBtn,"Login"));
        logoutBtn.setOnAction(event -> {
            UtilityMethods.switchToScene(logoutBtn,"Login");
            UtilityMethods.showPopup("Successfully logged out!");
        });
    }

    private void loadUserTotals() {
        Connection connection;
        PreparedStatement preparedStatement;
        ResultSet resultSet;

        try {
            connection = DBConnection.getConnection();

            int currentMonth = currentDate.getMonthValue();
            int currentYear = currentDate.getYear();

            String query = "SELECT " +
                    "C.name AS category, " +
                    "T.date AS date, " +
                    "SUM(CASE WHEN T.type = 'INCOME' THEN T.amount ELSE 0 END) - " +
                    "SUM(CASE WHEN T.type = 'EXPENSE' THEN T.amount ELSE 0 END) AS net_amount " +
                    "FROM TRANSACTION T " +
                    "LEFT JOIN CATEGORY C ON T.category_id = C.category_id " +
                    "WHERE T.user_id = ? AND MONTH(T.date) = ? AND YEAR(T.date) = ? AND T.group_id IS NULL " +
                    "GROUP BY T.category_id, T.date";
//                    "ORDER BY T.date DESC;";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, SessionManager.getCurrentUserId());
            preparedStatement.setInt(2, currentMonth);
            preparedStatement.setInt(3, currentYear);

            resultSet = preparedStatement.executeQuery();

            ObservableList<UserCategoryTotals> data = FXCollections.observableArrayList();

            if (!resultSet.isBeforeFirst()) {
                preparedStatement.close();
                resultSet.close();

                LocalDate previousMonthDate = currentDate.minusMonths(1);
                int previousMonth = previousMonthDate.getMonthValue();
                int previousYear = previousMonthDate.getYear();

                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, SessionManager.getCurrentUserId());
                preparedStatement.setInt(2, previousMonth);
                preparedStatement.setInt(3, previousYear);

                resultSet = preparedStatement.executeQuery();
            }

            while (resultSet.next()) {
                String category = resultSet.getString("category");
                String date = resultSet.getString("date");
                double netAmount = resultSet.getDouble("net_amount");

                data.add(new UserCategoryTotals(category, date,netAmount));
            }

            totalsCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
            dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
            totalsAmountIncomeColumn.setCellValueFactory(new PropertyValueFactory<>("netAmount"));
            totalsTable.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBudgetTableData() {
        Connection connection;
        PreparedStatement preparedStatement;
        ResultSet resultSet;

        try {
            connection = DBConnection.getConnection();

            int currentMonth = currentDate.getMonthValue();
            int currentYear = currentDate.getYear();

            String query = "SELECT " +
                    "B.budget_id, " +
                    "BI.budget_item_id, " +
                    "C.name AS category, " +
                    "BI.limit_amount AS budget_limit, " +
                    "COALESCE(SUM(CASE WHEN T.type = 'EXPENSE' THEN T.amount ELSE 0 END), 0) AS total_spend " +
                    "FROM BUDGET B " +
                    "INNER JOIN BUDGET_ITEM BI ON B.budget_id = BI.budget_id " +
                    "INNER JOIN CATEGORY C ON BI.category_id = C.category_id " +
                    "LEFT JOIN TRANSACTION T ON T.category_id = BI.category_id " +
                    "AND T.user_id = B.user_id " +
                    "AND MONTH(T.date) = B.month " +
                    "AND YEAR(T.date) = B.year " +
                    "AND (T.group_id IS NULL OR T.group_id = B.group_id) " +
                    "WHERE B.user_id = ? AND B.month = ? AND B.year = ? " +
                    "AND B.group_id IS NULL " +
                    "GROUP BY BI.budget_item_id, C.name, BI.limit_amount, B.budget_id";


            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, SessionManager.getCurrentUserId());
            preparedStatement.setInt(2, currentMonth);
            preparedStatement.setInt(3, currentYear);

            resultSet = preparedStatement.executeQuery();

            ObservableList<UserBudgetData> budgetData = FXCollections.observableArrayList();

            if (!resultSet.isBeforeFirst()) {
                preparedStatement.close();
                resultSet.close();

                LocalDate previousMonthDate = currentDate.minusMonths(1);
                int previousMonth = previousMonthDate.getMonthValue();
                int previousYear = previousMonthDate.getYear();

                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, SessionManager.getCurrentUserId());
                preparedStatement.setInt(2, previousMonth);
                preparedStatement.setInt(3, previousYear);

                resultSet = preparedStatement.executeQuery();
            }

            while (resultSet.next()) {
                int budgetId = resultSet.getInt("budget_id");
                int budgetItemId = resultSet.getInt("budget_item_id");
                String category = resultSet.getString("category");
                double budgetLimit = resultSet.getDouble("budget_limit");
                double totalSpend = resultSet.getDouble("total_spend");

                budgetData.add(new UserBudgetData(budgetId,budgetItemId,category, budgetLimit, totalSpend));
            }
            budgetList.clear();
            budgetList.addAll(budgetData);
            budgetListView.setItems(budgetList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLimitInDatabase(UserBudgetData budgetData) {
        try {Connection connection = DBConnection.getConnection();
            String updateQuery = "UPDATE BUDGET_ITEM SET limit_amount = ? WHERE budget_item_id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(updateQuery);
            preparedStatement.setDouble(1, budgetData.getLimit());
            preparedStatement.setInt(2, budgetData.getBudgetItemId());

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                UtilityMethods.showPopup("Limited updated successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}