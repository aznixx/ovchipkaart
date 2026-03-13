package nl.ovchipkaart.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import nl.ovchipkaart.model.Account;

public class LoginController {

    private static final String ACCOUNTS_FOLDER = "ovchipkaart_accounts";

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox personalCheckBox;
    @FXML private Label messageLabel;

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Fill in your email and password.");
            return;
        }

        Account account = Account.loadFromFile(ACCOUNTS_FOLDER, email);
        if (account == null) {
            showMessage("Account not found. Register first.");
            return;
        }

        if (!account.checkPassword(password)) {
            showMessage("Wrong password.");
            return;
        }

        openMainScreen(account);
    }

    @FXML
    private void onRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Fill in all fields.");
            return;
        }

        if (Account.exists(ACCOUNTS_FOLDER, email)) {
            showMessage("Account already exists. Just log in.");
            return;
        }

        boolean personal = personalCheckBox.isSelected();
        Account account = new Account(name, email, password, personal);
        account.getCard().topUp(50);
        account.saveToFile(ACCOUNTS_FOLDER);

        showMessage("Account created! You can now log in.");
    }

    private void openMainScreen(Account account) {
        try {
            OVChipkaartApp.showMainScreen(account);
        } catch (Exception e) {
            showMessage("Could not open main screen: " + e.getMessage());
        }
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }
}
