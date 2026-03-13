package nl.ovchipkaart.model;

import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OVCard {

    private String cardNumber;
    private double balance;
    private boolean personal;
    private boolean blocked;
    private LocalDate expiryDate;
    private TravelClass travelClass;
    private Subscription subscription;
    private List<Transaction> transactions;

    private Station checkedInAt;
    private TransportType checkedInTransport;
    private LocalDateTime checkInTime;

    private boolean autoTopUpEnabled;
    private double autoTopUpThreshold;
    private double autoTopUpAmount;

    private double totalSpent;
    private double totalDistanceKm;
    private int totalTrips;

    private LocalDateTime lastCheckoutTime;
    private Station lastCheckoutStation;

    private boolean dayPassActive;
    private LocalDate dayPassDate;

    public OVCard(String cardNumber, boolean personal) {
        this.cardNumber = cardNumber;
        this.personal = personal;
        this.balance = 0;
        this.blocked = false;
        this.travelClass = TravelClass.SECOND;
        this.subscription = Subscription.NONE;
        this.transactions = new ArrayList<>();
        this.expiryDate = LocalDate.now().plusYears(5);
        this.autoTopUpEnabled = false;
        this.autoTopUpThreshold = 0;
        this.autoTopUpAmount = 0;
        this.totalSpent = 0;
        this.totalDistanceKm = 0;
        this.totalTrips = 0;
        this.dayPassActive = false;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public boolean isBlocked() {
        return blocked;
    }

    private void checkIfShouldBlock() {
        if (balance < -30) {
            blocked = true;
        }
    }

    public boolean unblock() {
        if (balance >= 0) {
            blocked = false;
            return true;
        }
        return false;
    }

    public boolean topUp(double amount, String source) {
        if (isExpired() || blocked) {
            return false;
        }
        if (amount <= 0 || amount > 150) {
            return false;
        }
        if (source.equals("machine") && amount < 5) {
            return false;
        }
        if (source.equals("online") && amount < 10) {
            return false;
        }
        if (!personal && (balance + amount) > 150) {
            return false;
        }
        this.balance = this.balance + amount;
        transactions.add(new Transaction("TOP_UP", amount, balance,
                "Top-up €" + String.format("%.2f", amount) + " (" + source + ")"));

        if (blocked && balance >= 0) {
            blocked = false;
        }

        return true;
    }

    public boolean topUp(double amount) {
        return topUp(amount, "machine");
    }

    public boolean buyDayPass() {
        double price = 22.50;
        if (isExpired() || blocked) {
            return false;
        }
        if (balance < price) {
            return false;
        }
        this.balance = this.balance - price;
        this.dayPassActive = true;
        this.dayPassDate = LocalDate.now();
        transactions.add(new Transaction("DAY_PASS", -price, balance, "Day pass purchased"));
        return true;
    }

    public boolean isDayPassValid() {
        return dayPassActive && dayPassDate != null && dayPassDate.equals(LocalDate.now());
    }

    public boolean checkIn(Station station, TransportType transport) {
        if (isExpired() || blocked) {
            return false;
        }
        if (checkedInAt != null) {
            return false;
        }
        if (!isDayPassValid() && balance < 20) {
            return false;
        }
        if (!station.hasTransportType(transport)) {
            return false;
        }
        this.checkedInAt = station;
        this.checkedInTransport = transport;
        this.checkInTime = LocalDateTime.now();
        return true;
    }

    public boolean checkInAt(Station station, TransportType transport, LocalDateTime time) {
        if (isExpired() || blocked) {
            return false;
        }
        if (checkedInAt != null) {
            return false;
        }
        if (!isDayPassValid() && balance < 20) {
            return false;
        }
        if (!station.hasTransportType(transport)) {
            return false;
        }
        this.checkedInAt = station;
        this.checkedInTransport = transport;
        this.checkInTime = time;
        return true;
    }

    public double checkOut(Station station) {
        if (checkedInAt == null) {
            return -1;
        }
        return checkOutAt(station, LocalDateTime.now());
    }

    public double checkOutAt(Station station, LocalDateTime time) {
        if (checkedInAt == null) {
            return -1;
        }

        double distance = Math.abs(checkedInAt.getKilometerMarker() - station.getKilometerMarker());
        double fare;

        if (isDayPassValid()) {
            fare = 0;
        } else {
            fare = calculateFare(checkedInAt, station, checkedInTransport, time);
        }

        this.balance = this.balance - fare;
        this.totalSpent = this.totalSpent + fare;
        this.totalDistanceKm = this.totalDistanceKm + distance;
        this.totalTrips = this.totalTrips + 1;

        String desc = checkedInAt.getName() + " → " + station.getName()
                + " (" + checkedInTransport + ", " + travelClass + ")";
        if (isDayPassValid()) {
            desc = desc + " [DAY PASS]";
        }
        transactions.add(new Transaction(time, "TRAVEL", -fare, balance, desc));

        this.lastCheckoutTime = time;
        this.lastCheckoutStation = station;
        this.checkedInAt = null;
        this.checkedInTransport = null;
        this.checkInTime = null;

        doAutoTopUp();
        checkIfShouldBlock();

        return fare;
    }

    public double applyMissedCheckoutPenalty() {
        if (checkedInAt == null) {
            return 0;
        }
        double penalty = 20.0;
        this.balance = this.balance - penalty;
        this.totalSpent = this.totalSpent + penalty;
        String desc = "Missed checkout penalty (boarded at " + checkedInAt.getName() + ")";
        transactions.add(new Transaction("PENALTY", -penalty, balance, desc));
        this.checkedInAt = null;
        this.checkedInTransport = null;
        this.checkInTime = null;

        doAutoTopUp();
        checkIfShouldBlock();

        return penalty;
    }

    public boolean refundLastTrip() {
        Transaction lastTravel = null;
        int lastIndex = -1;
        for (int i = transactions.size() - 1; i >= 0; i--) {
            if (transactions.get(i).getType().equals("TRAVEL")) {
                lastTravel = transactions.get(i);
                lastIndex = i;
                break;
            }
        }
        if (lastTravel == null) {
            return false;
        }
        double refundAmount = Math.abs(lastTravel.getAmount());
        this.balance = this.balance + refundAmount;
        this.totalSpent = this.totalSpent - refundAmount;
        if (totalSpent < 0) totalSpent = 0;
        this.totalTrips = this.totalTrips - 1;
        if (totalTrips < 0) totalTrips = 0;
        transactions.add(new Transaction("REFUND", refundAmount, balance,
                "Refund: " + lastTravel.getDescription()));
        return true;
    }

    private boolean isTransfer(LocalDateTime checkInTime) {
        if (lastCheckoutTime == null || lastCheckoutStation == null) {
            return false;
        }
        long minutesSinceLastCheckout = Duration.between(lastCheckoutTime, checkInTime).toMinutes();
        return minutesSinceLastCheckout >= 0 && minutesSinceLastCheckout <= 35;
    }

    private double calculateFare(Station from, Station to, TransportType transport, LocalDateTime time) {
        double fare;

        if (transport == TransportType.BUS || transport == TransportType.TRAM) {
            fare = 1.08;
        } else if (transport == TransportType.METRO) {
            double distance = Math.abs(from.getKilometerMarker() - to.getKilometerMarker());
            fare = 1.08 + 0.10 * distance;
        } else {
            double distance = Math.abs(from.getKilometerMarker() - to.getKilometerMarker());
            fare = 0.89 + 0.19 * distance;
        }

        if (isTransfer(checkInTime)) {
            if (transport == TransportType.BUS || transport == TransportType.TRAM) {
                fare = 0;
            } else {
                fare = fare - 0.89;
                if (fare < 0) fare = 0;
            }
        }

        if (travelClass == TravelClass.FIRST && transport == TransportType.TRAIN) {
            fare = fare * 1.5;
        }

        if (isPeakHour(time)) {
            fare = fare * 1.2;
        }

        fare = applySubscriptionDiscount(fare, time);

        return Math.round(fare * 100.0) / 100.0;
    }

    private boolean isPeakHour(LocalDateTime time) {
        DayOfWeek day = time.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime t = time.toLocalTime();
        boolean morningPeak = !t.isBefore(LocalTime.of(6, 30)) && t.isBefore(LocalTime.of(9, 0));
        boolean eveningPeak = !t.isBefore(LocalTime.of(16, 0)) && t.isBefore(LocalTime.of(18, 30));
        return morningPeak || eveningPeak;
    }

    private double applySubscriptionDiscount(double fare, LocalDateTime time) {
        if (subscription == Subscription.DAL_VOORDEEL) {
            if (!isPeakHour(time)) {
                return fare * 0.6;
            }
        } else if (subscription == Subscription.WEEKEND_VRIJ) {
            DayOfWeek day = time.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                return 0;
            }
        } else if (subscription == Subscription.ALTIJD_VOORDEEL) {
            return fare * 0.8;
        }
        return fare;
    }

    public void enableAutoTopUp(double threshold, double amount) {
        if (threshold >= 0 && amount > 0 && amount <= 150) {
            this.autoTopUpEnabled = true;
            this.autoTopUpThreshold = threshold;
            this.autoTopUpAmount = amount;
        }
    }

    public void disableAutoTopUp() {
        this.autoTopUpEnabled = false;
    }

    private void doAutoTopUp() {
        if (autoTopUpEnabled && balance < autoTopUpThreshold) {
            this.balance = this.balance + autoTopUpAmount;
            transactions.add(new Transaction("AUTO_TOP_UP", autoTopUpAmount, balance,
                    "Auto top-up (below €" + String.format("%.2f", autoTopUpThreshold) + ")"));
        }
    }

    public String getBalanceWarning() {
        if (blocked) {
            return "BLOCKED - balance too negative. Top up to unblock.";
        }
        if (balance < 0) {
            return "WARNING - negative balance!";
        }
        if (balance < 10) {
            return "Low balance - consider topping up.";
        }
        if (balance < 20) {
            return "Balance below check-in minimum (€20).";
        }
        return "";
    }

    public String getMonthSummary(int year, int month) {
        int trips = 0;
        double spent = 0;
        double topUps = 0;

        for (Transaction tx : transactions) {
            if (tx.getDateTime().getYear() == year && tx.getDateTime().getMonthValue() == month) {
                if (tx.getType().equals("TRAVEL")) {
                    trips++;
                    spent = spent + Math.abs(tx.getAmount());
                } else if (tx.getType().equals("TOP_UP") || tx.getType().equals("AUTO_TOP_UP")) {
                    topUps = topUps + tx.getAmount();
                }
            }
        }

        return "Month " + month + "/" + year + ": "
                + trips + " trips, €" + String.format("%.2f", spent) + " spent, €"
                + String.format("%.2f", topUps) + " topped up";
    }

    public void saveToFile(String filename) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(filename));
            writer.println(cardNumber);
            writer.println(personal);
            writer.println(balance);
            writer.println(blocked);
            writer.println(travelClass);
            writer.println(subscription);
            writer.println(expiryDate);
            writer.println(autoTopUpEnabled);
            writer.println(autoTopUpThreshold);
            writer.println(autoTopUpAmount);
            writer.println(totalSpent);
            writer.println(totalDistanceKm);
            writer.println(totalTrips);
            writer.println(dayPassActive);
            writer.println(dayPassDate != null ? dayPassDate : "null");
            writer.println(transactions.size());
            for (Transaction tx : transactions) {
                writer.println(tx.getDateTime() + ";" + tx.getType() + ";" + tx.getAmount()
                        + ";" + tx.getBalanceAfter() + ";" + tx.getDescription());
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Could not save card: " + e.getMessage());
        }
    }

    public static OVCard loadFromFile(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String cardNumber = reader.readLine();
            boolean personal = Boolean.parseBoolean(reader.readLine());
            double balance = Double.parseDouble(reader.readLine());
            boolean blocked = Boolean.parseBoolean(reader.readLine());
            TravelClass travelClass = TravelClass.valueOf(reader.readLine());
            Subscription subscription = Subscription.valueOf(reader.readLine());
            LocalDate expiryDate = LocalDate.parse(reader.readLine());
            boolean autoTopUp = Boolean.parseBoolean(reader.readLine());
            double autoThreshold = Double.parseDouble(reader.readLine());
            double autoAmount = Double.parseDouble(reader.readLine());
            double totalSpent = Double.parseDouble(reader.readLine());
            double totalDistanceKm = Double.parseDouble(reader.readLine());
            int totalTrips = Integer.parseInt(reader.readLine());
            boolean dayPassActive = Boolean.parseBoolean(reader.readLine());
            String dayPassStr = reader.readLine();
            LocalDate dayPassDate = dayPassStr.equals("null") ? null : LocalDate.parse(dayPassStr);
            int txCount = Integer.parseInt(reader.readLine());

            OVCard card = new OVCard(cardNumber, personal);
            card.balance = balance;
            card.blocked = blocked;
            card.travelClass = travelClass;
            card.subscription = subscription;
            card.expiryDate = expiryDate;
            card.autoTopUpEnabled = autoTopUp;
            card.autoTopUpThreshold = autoThreshold;
            card.autoTopUpAmount = autoAmount;
            card.totalSpent = totalSpent;
            card.totalDistanceKm = totalDistanceKm;
            card.totalTrips = totalTrips;
            card.dayPassActive = dayPassActive;
            card.dayPassDate = dayPassDate;

            for (int i = 0; i < txCount; i++) {
                String line = reader.readLine();
                String[] parts = line.split(";", 5);
                LocalDateTime dt = LocalDateTime.parse(parts[0]);
                String type = parts[1];
                double amount = Double.parseDouble(parts[2]);
                double balAfter = Double.parseDouble(parts[3]);
                String desc = parts[4];
                card.transactions.add(new Transaction(dt, type, amount, balAfter, desc));
            }

            reader.close();
            return card;
        } catch (IOException e) {
            return null;
        }
    }

    public void setTravelClass(TravelClass travelClass) {
        this.travelClass = travelClass;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public String getCardNumber() { return cardNumber; }
    public double getBalance() { return balance; }
    public boolean isPersonal() { return personal; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public TravelClass getTravelClass() { return travelClass; }
    public Subscription getSubscription() { return subscription; }
    public boolean isCheckedIn() { return checkedInAt != null; }
    public Station getCheckedInAt() { return checkedInAt; }
    public TransportType getCheckedInTransport() { return checkedInTransport; }
    public boolean isAutoTopUpEnabled() { return autoTopUpEnabled; }
    public List<Transaction> getTransactions() { return transactions; }
    public double getTotalSpent() { return totalSpent; }
    public double getTotalDistanceKm() { return totalDistanceKm; }
    public int getTotalTrips() { return totalTrips; }
    public boolean isDayPassActive() { return dayPassActive; }
}
