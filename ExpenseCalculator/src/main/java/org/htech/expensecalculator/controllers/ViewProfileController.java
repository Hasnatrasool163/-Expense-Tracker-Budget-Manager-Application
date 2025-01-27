package org.htech.expensecalculator.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.htech.expensecalculator.database.DBConnection;
import org.htech.expensecalculator.modal.User;
import org.htech.expensecalculator.utilities.SessionManager;
import org.htech.expensecalculator.utilities.UtilityMethods;

import java.sql.*;

public class ViewProfileController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField oldPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    private User currentUser;

    @FXML
    public void initialize() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        currentUser = getCurrentUser();
        usernameField.setText(currentUser.getName());
        emailField.setText(currentUser.getEmail());
        emailField.setEditable(false);
        String password = verifyCurrentUserPassword(SessionManager.getCurrentUserId());
        if(password!=null) currentUser.setPassword(password);
    }

    private User getCurrentUser() {
        return new User(SessionManager.getCurrentUserName(), SessionManager.getCurrentUserEmail());
    }

    public void handleUpdateProfile(ActionEvent event) {
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!oldPassword.equals(currentUser.getPassword())) {
            showAlert("Incorrect old password", "The old password is incorrect.", AlertType.ERROR);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showAlert("Password mismatch", "The new password and confirm password do not match.", AlertType.ERROR);
            return;
        }

        updatePasswordInDatabase(newPassword);

        String updatedUsername = usernameField.getText();
        if (!updatedUsername.equals(currentUser.getName())) {
            updateUsernameInDatabase(updatedUsername);
        }

        UtilityMethods.showPopup("Profile updated successfully.");
        oldPasswordField.setText("");
        confirmPasswordField.setText("");
        newPasswordField.setText("");
    }

    private void updatePasswordInDatabase(String newPassword) {
        String query = "UPDATE USERS SET password = ? WHERE name = ?";
        try {Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query);

            ps.setString(1, newPassword);
            ps.setString(2, currentUser.getName());

            ps.executeUpdate();
        } catch (SQLException e) {
            UtilityMethods.showPopupWarning("Error updating password: " + e.getMessage());
        }
    }

    private void updateUsernameInDatabase(String newUsername) {
        String query = "UPDATE USERS SET name = ? WHERE name = ?";
        try {Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query);

            ps.setString(1, newUsername);
            ps.setString(2, currentUser.getName());

            ps.executeUpdate();
        } catch (SQLException e) {
            UtilityMethods.showPopupWarning("Error updating username: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message, AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    public void handleCancel(ActionEvent event) {
        UtilityMethods.closeCurrentWindow(usernameField);
    }

    private String verifyCurrentUserPassword(int userId){
        String password = null;
        try{
            Connection connection = DBConnection.getConnection();
            String sql = "Select password from USERS where user_id =?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1,userId);
            ResultSet rs = statement.executeQuery();
            if(rs.next()){
                password = rs.getString("password");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return password;
    }
}
