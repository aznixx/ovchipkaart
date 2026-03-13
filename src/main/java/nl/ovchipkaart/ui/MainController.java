package nl.ovchipkaart.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import nl.ovchipkaart.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    private static final String ACCOUNTS_FOLDER = "ovchipkaart_accounts";

    @FXML private Label welcomeLabel;
    @FXML private ComboBox<String> cardSelectorComboBox;
    @FXML private Label balanceLabel;
    @FXML private Label statusLabel;
    @FXML private Label cardTypeLabel;
    @FXML private Label warningLabel;
    @FXML private Label dayPassLabel;
    @FXML private ComboBox<String> stationComboBox;
    @FXML private ComboBox<String> transportComboBox;
    @FXML private ComboBox<String> travelClassComboBox;
    @FXML private ComboBox<String> subscriptionComboBox;
    @FXML private Button tapButton;
    @FXML private TextField topUpAmountField;
    @FXML private CheckBox autoTopUpCheckBox;
    @FXML private TextField autoTopUpThresholdField;
    @FXML private TextField autoTopUpAmountField;
    @FXML private ListView<String> transactionList;
    @FXML private Label messageLabel;
    @FXML private Label monthSummaryLabel;
    @FXML private Label statsLabel;
    @FXML private Label cardInfoLabel;
    @FXML private TabPane tabPane;

    private Account account;
    private OVCard card;
    private int selectedCardIndex;
    private List<Station> stations;

    @FXML
    public void initialize() {
        stations = List.of(
                new Station("Amsterdam Centraal", 0.0,
                        List.of(TransportType.TRAIN, TransportType.METRO, TransportType.TRAM)),
                new Station("Schiphol Airport", 15.2,
                        List.of(TransportType.TRAIN, TransportType.BUS)),
                new Station("Leiden Centraal", 36.5,
                        List.of(TransportType.TRAIN, TransportType.BUS)),
                new Station("Den Haag Centraal", 56.4,
                        List.of(TransportType.TRAIN, TransportType.TRAM, TransportType.BUS)),
                new Station("Rotterdam Centraal", 78.1,
                        List.of(TransportType.TRAIN, TransportType.METRO, TransportType.TRAM, TransportType.BUS))
        );

        List<String> stationNames = stations.stream().map(Station::getName).toList();
        stationComboBox.setItems(FXCollections.observableArrayList(stationNames));
        stationComboBox.getSelectionModel().selectFirst();

        transportComboBox.setItems(FXCollections.observableArrayList("TRAIN", "BUS", "TRAM", "METRO"));
        transportComboBox.getSelectionModel().selectFirst();

        travelClassComboBox.setItems(FXCollections.observableArrayList("SECOND", "FIRST"));
        travelClassComboBox.getSelectionModel().selectFirst();
        travelClassComboBox.setOnAction(e -> {
            if (card != null) {
                card.setTravelClass(TravelClass.valueOf(travelClassComboBox.getValue()));
                showMessage("Travel class set to " + travelClassComboBox.getValue());
            }
        });

        subscriptionComboBox.setItems(FXCollections.observableArrayList(
                "NONE", "DAL_VOORDEEL", "WEEKEND_VRIJ", "ALTIJD_VOORDEEL"));
        subscriptionComboBox.getSelectionModel().selectFirst();
        subscriptionComboBox.setOnAction(e -> {
            if (card != null) {
                card.setSubscription(Subscription.valueOf(subscriptionComboBox.getValue()));
                showMessage("Subscription set to " + subscriptionComboBox.getValue());
            }
        });

        cardSelectorComboBox.setOnAction(e -> {
            int index = cardSelectorComboBox.getSelectionModel().getSelectedIndex();
            if (index >= 0 && account != null) {
                selectedCardIndex = index;
                card = account.getCard(index);
                travelClassComboBox.setValue(card.getTravelClass().name());
                subscriptionComboBox.setValue(card.getSubscription().name());
                autoTopUpCheckBox.setSelected(card.isAutoTopUpEnabled());
                refreshUI();
            }
        });
    }

    public void setAccount(Account account) {
        this.account = account;
        this.selectedCardIndex = 0;
        this.card = account.getCard(0);
        welcomeLabel.setText("Welcome, " + account.getName());
        updateCardSelector();
        travelClassComboBox.setValue(card.getTravelClass().name());
        subscriptionComboBox.setValue(card.getSubscription().name());
        autoTopUpCheckBox.setSelected(card.isAutoTopUpEnabled());
        refreshUI();
        showMessage("Logged in. Balance: €" + String.format("%.2f", card.getBalance()));
    }

    private void updateCardSelector() {
        List<String> cardNames = new ArrayList<>();
        for (int i = 0; i < account.getCards().size(); i++) {
            OVCard c = account.getCard(i);
            String type = c.isPersonal() ? "Personal" : "Anonymous";
            cardNames.add(c.getCardNumber() + " (" + type + ")");
        }
        cardSelectorComboBox.setItems(FXCollections.observableArrayList(cardNames));
        cardSelectorComboBox.getSelectionModel().select(selectedCardIndex);
    }

    @FXML
    private void onTap() {
        int stationIndex = stationComboBox.getSelectionModel().getSelectedIndex();
        if (stationIndex < 0) {
            showMessage("Select a station first.");
            return;
        }
        Station selected = stations.get(stationIndex);
        String transportName = transportComboBox.getValue();
        TransportType transport = TransportType.valueOf(transportName);

        if (card.isCheckedIn()) {
            double fare = card.checkOut(selected);
            if (fare < 0) {
                showMessage("Could not check out.");
            } else {
                showMessage("Checked out at " + selected.getName() + ". Fare: €" + String.format("%.2f", fare));
            }
        } else {
            if (!selected.hasTransportType(transport)) {
                showMessage(selected.getName() + " does not have " + transportName);
                return;
            }
            if (card.isBlocked()) {
                showMessage("Card is BLOCKED. Top up to unblock.");
                return;
            }
            if (card.checkIn(selected, transport)) {
                showMessage("Checked in at " + selected.getName() + " (" + transportName + ")");
            } else if (card.isExpired()) {
                showMessage("Card is expired!");
            } else if (card.getBalance() < 20 && !card.isDayPassValid()) {
                showMessage("Not enough balance. You need at least €20.");
            } else {
                showMessage("Could not check in.");
            }
        }
        saveAccount();
        refreshUI();
    }

    @FXML
    private void onMachineTopUp() {
        doTopUp("machine", 5);
    }

    @FXML
    private void onOnlineTopUp() {
        doTopUp("online", 10);
    }

    private void doTopUp(String source, double minimum) {
        String text = topUpAmountField.getText().trim();
        if (text.isEmpty()) {
            showMessage("Enter a top-up amount.");
            return;
        }
        try {
            double amount = Double.parseDouble(text);
            if (amount < minimum) {
                showMessage("Minimum " + source + " top-up is €" + String.format("%.2f", minimum));
                return;
            }
            if (card.topUp(amount, source)) {
                showMessage("Topped up €" + String.format("%.2f", amount) + " via " + source);
                topUpAmountField.clear();
            } else if (card.isExpired()) {
                showMessage("Card is expired!");
            } else if (card.isBlocked()) {
                showMessage("Card is blocked!");
            } else if (!card.isPersonal() && card.getBalance() + amount > 150) {
                showMessage("Anonymous card cannot exceed €150 balance.");
            } else {
                showMessage("Invalid amount (must be €" + String.format("%.2f", minimum) + " - €150).");
            }
        } catch (NumberFormatException e) {
            showMessage("Invalid number.");
        }
        saveAccount();
        refreshUI();
    }

    @FXML
    private void onAutoTopUpApply() {
        if (autoTopUpCheckBox.isSelected()) {
            try {
                double threshold = Double.parseDouble(autoTopUpThresholdField.getText().trim());
                double amount = Double.parseDouble(autoTopUpAmountField.getText().trim());
                card.enableAutoTopUp(threshold, amount);
                showMessage("Auto top-up enabled.");
            } catch (NumberFormatException e) {
                showMessage("Invalid auto top-up values.");
            }
        } else {
            card.disableAutoTopUp();
            showMessage("Auto top-up disabled.");
        }
        saveAccount();
        refreshUI();
    }

    @FXML
    private void onBuyDayPass() {
        if (card.buyDayPass()) {
            showMessage("Day pass purchased! Free travel for today.");
        } else if (card.getBalance() < 22.50) {
            showMessage("Not enough balance for day pass (€22.50).");
        } else {
            showMessage("Could not buy day pass.");
        }
        saveAccount();
        refreshUI();
    }

    @FXML
    private void onMissedCheckout() {
        if (!card.isCheckedIn()) {
            showMessage("Not checked in, no penalty to apply.");
            return;
        }
        double penalty = card.applyMissedCheckoutPenalty();
        showMessage("Penalty of €" + String.format("%.2f", penalty) + " applied.");
        saveAccount();
        refreshUI();
    }

    @FXML
    private void onRefund() {
        if (card.refundLastTrip()) {
            showMessage("Last trip refunded.");
        } else {
            showMessage("No trips to refund.");
        }
        saveAccount();
        refreshUI();
    }

    @FXML
    private void onAddPersonalCard() {
        OVCard newCard = account.addCard(true);
        newCard.topUp(0.01);
        showMessage("New personal card added: " + newCard.getCardNumber());
        updateCardSelector();
        saveAccount();
        refreshUI();
    }

    @FXML
    private void onAddAnonCard() {
        OVCard newCard = account.addCard(false);
        newCard.topUp(0.01);
        showMessage("New anonymous card added: " + newCard.getCardNumber());
        updateCardSelector();
        saveAccount();
        refreshUI();
    }

    @FXML
    private void onRemoveCard() {
        if (account.getCards().size() <= 1) {
            showMessage("Can't remove your only card.");
            return;
        }
        if (account.removeCard(selectedCardIndex)) {
            selectedCardIndex = 0;
            card = account.getCard(0);
            showMessage("Card removed.");
            updateCardSelector();
            saveAccount();
            refreshUI();
        }
    }

    @FXML
    private void onLogout() {
        saveAccount();
        try {
            OVChipkaartApp.showLoginScreen();
        } catch (Exception e) {
            showMessage("Could not logout: " + e.getMessage());
        }
    }

    private void saveAccount() {
        account.saveToFile(ACCOUNTS_FOLDER);
    }

    private void refreshUI() {
        balanceLabel.setText("€" + String.format("%.2f", card.getBalance()));
        cardTypeLabel.setText(card.isPersonal() ? "Personal" : "Anonymous");

        String warning = card.getBalanceWarning();
        warningLabel.setText(warning);
        warningLabel.setVisible(!warning.isEmpty());

        if (card.isCheckedIn()) {
            statusLabel.setText("Checked in at " + card.getCheckedInAt().getName()
                    + " (" + card.getCheckedInTransport() + ")");
            tapButton.setText("CHECK OUT");
        } else {
            statusLabel.setText("Not checked in");
            tapButton.setText("CHECK IN");
        }

        if (card.isDayPassValid()) {
            dayPassLabel.setText("Day pass ACTIVE");
        } else {
            dayPassLabel.setText("");
        }

        LocalDate now = LocalDate.now();
        monthSummaryLabel.setText(card.getMonthSummary(now.getYear(), now.getMonthValue()));

        List<String> txStrings = card.getTransactions().stream()
                .map(Transaction::toString)
                .toList();
        transactionList.setItems(FXCollections.observableArrayList(txStrings));

        statsLabel.setText(
                "Total trips: " + card.getTotalTrips()
                + "\nTotal distance: " + String.format("%.1f", card.getTotalDistanceKm()) + " km"
                + "\nTotal spent: €" + String.format("%.2f", card.getTotalSpent())
        );

        cardInfoLabel.setText(
                "Card number: " + card.getCardNumber()
                + "\nType: " + (card.isPersonal() ? "Personal" : "Anonymous")
                + "\nExpiry: " + card.getExpiryDate()
                + "\nTravel class: " + card.getTravelClass()
                + "\nSubscription: " + card.getSubscription()
                + "\nBlocked: " + (card.isBlocked() ? "YES" : "No")
                + "\nAuto top-up: " + (card.isAutoTopUpEnabled() ? "Enabled" : "Disabled")
        );
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }
}
