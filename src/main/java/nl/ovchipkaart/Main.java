package nl.ovchipkaart;

import nl.ovchipkaart.model.*;

import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        Station amsterdam = new Station("Amsterdam Centraal", 0.0,
                List.of(TransportType.TRAIN, TransportType.METRO, TransportType.TRAM));
        Station schiphol = new Station("Schiphol Airport", 15.2);
        Station leiden = new Station("Leiden Centraal", 36.5);
        Station rotterdam = new Station("Rotterdam Centraal", 78.1,
                List.of(TransportType.TRAIN, TransportType.METRO, TransportType.TRAM, TransportType.BUS));

        System.out.println("=== OV-chipkaart Demo ===\n");

        // Jan: personal card with subscription
        OVCard jansCard = new OVCard("3528-1234-5678-9012", true);
        Traveler jan = new Traveler("Jan de Vries", jansCard);
        System.out.println("--- " + jan.getName() + " (Personal Card) ---");

        jansCard.topUp(100, "machine");
        jansCard.setSubscription(Subscription.DAL_VOORDEEL);
        jansCard.enableAutoTopUp(10, 30);
        System.out.println("Balance: €" + String.format("%.2f", jansCard.getBalance()));
        System.out.println("Subscription: " + jansCard.getSubscription());
        System.out.println();

        // Off-peak trip with discount
        LocalDateTime offPeak = LocalDateTime.of(2026, 3, 13, 11, 0);
        jansCard.checkInAt(amsterdam, TransportType.TRAIN, offPeak);
        double fare1 = jansCard.checkOutAt(rotterdam, offPeak.plusMinutes(40));
        System.out.println("Off-peak Amsterdam → Rotterdam: €" + String.format("%.2f", fare1));

        // Transfer within 35 min (no base fare)
        LocalDateTime transfer = offPeak.plusMinutes(45);
        jansCard.checkInAt(rotterdam, TransportType.METRO, transfer);
        double fare2 = jansCard.checkOutAt(amsterdam, transfer.plusMinutes(30));
        System.out.println("Transfer (metro, within 35 min): €" + String.format("%.2f", fare2));
        System.out.println("Balance: €" + String.format("%.2f", jansCard.getBalance()));
        System.out.println();

        // 1st class trip
        jansCard.setTravelClass(TravelClass.FIRST);
        jansCard.checkInAt(amsterdam, TransportType.TRAIN, offPeak);
        double fare3 = jansCard.checkOutAt(schiphol, offPeak.plusMinutes(15));
        System.out.println("1st class Amsterdam → Schiphol: €" + String.format("%.2f", fare3));
        jansCard.setTravelClass(TravelClass.SECOND);
        System.out.println();

        // Refund last trip
        jansCard.refundLastTrip();
        System.out.println("Last trip refunded!");
        System.out.println("Balance: €" + String.format("%.2f", jansCard.getBalance()));
        System.out.println();

        // Missed checkout
        jansCard.checkInAt(leiden, TransportType.TRAIN, offPeak);
        double penalty = jansCard.applyMissedCheckoutPenalty();
        System.out.println("Missed checkout penalty: €" + String.format("%.2f", penalty));
        System.out.println("Balance: €" + String.format("%.2f", jansCard.getBalance()));
        System.out.println(jansCard.getBalanceWarning());
        System.out.println();

        // Tourist: anonymous card with day pass
        OVCard anonCard = new OVCard("3528-0000-0000-0001", false);
        Traveler tourist = new Traveler("Tourist", anonCard);
        System.out.println("--- " + tourist.getName() + " (Anonymous Card) ---");

        anonCard.topUp(50, "machine");
        anonCard.buyDayPass();
        System.out.println("Day pass purchased! Balance: €" + String.format("%.2f", anonCard.getBalance()));

        anonCard.checkIn(amsterdam, TransportType.TRAIN);
        double fare4 = anonCard.checkOut(rotterdam);
        System.out.println("Amsterdam → Rotterdam with day pass: €" + String.format("%.2f", fare4) + " (free!)");

        anonCard.checkIn(rotterdam, TransportType.BUS);
        double fare5 = anonCard.checkOut(rotterdam);
        System.out.println("Bus trip with day pass: €" + String.format("%.2f", fare5) + " (free!)");
        System.out.println("Balance: €" + String.format("%.2f", anonCard.getBalance()));
        System.out.println();

        // Stats
        System.out.println("--- Jan's Stats ---");
        System.out.println("Total trips: " + jansCard.getTotalTrips());
        System.out.println("Total distance: " + String.format("%.1f", jansCard.getTotalDistanceKm()) + " km");
        System.out.println("Total spent: €" + String.format("%.2f", jansCard.getTotalSpent()));
        System.out.println(jansCard.getMonthSummary(2026, 3));
        System.out.println();

        // Transaction history
        System.out.println("--- Jan's Transactions ---");
        for (Transaction tx : jansCard.getTransactions()) {
            System.out.println("  " + tx);
        }

        // Save and load
        System.out.println();
        jansCard.saveToFile("jan_card.txt");
        OVCard loaded = OVCard.loadFromFile("jan_card.txt");
        if (loaded != null) {
            System.out.println("Loaded card: " + loaded.getCardNumber()
                    + ", balance: €" + String.format("%.2f", loaded.getBalance())
                    + ", trips: " + loaded.getTotalTrips());
        }

        System.out.println("\n=== Done ===");
    }
}
