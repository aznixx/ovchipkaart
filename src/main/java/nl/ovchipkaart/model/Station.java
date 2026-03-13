package nl.ovchipkaart.model;

import java.util.ArrayList;
import java.util.List;

public class Station {

    private String name;
    private double kilometerMarker;
    private List<TransportType> availableTransport;

    public Station(String name, double kilometerMarker) {
        this.name = name;
        this.kilometerMarker = kilometerMarker;
        this.availableTransport = new ArrayList<>();
        this.availableTransport.add(TransportType.TRAIN);
    }

    public Station(String name, double kilometerMarker, List<TransportType> availableTransport) {
        this.name = name;
        this.kilometerMarker = kilometerMarker;
        this.availableTransport = availableTransport;
    }

    public boolean hasTransportType(TransportType type) {
        return availableTransport.contains(type);
    }

    public String getName() {
        return name;
    }

    public double getKilometerMarker() {
        return kilometerMarker;
    }

    public List<TransportType> getAvailableTransport() {
        return availableTransport;
    }

    @Override
    public String toString() {
        return name;
    }
}
