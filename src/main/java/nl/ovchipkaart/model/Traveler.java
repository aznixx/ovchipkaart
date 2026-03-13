package nl.ovchipkaart.model;

public class Traveler {

    private String name;
    private OVCard card;

    public Traveler(String name, OVCard card) {
        this.name = name;
        this.card = card;
    }

    public String getName() {
        return name;
    }

    public OVCard getCard() {
        return card;
    }

    @Override
    public String toString() {
        return name + " (card: " + card.getCardNumber() + ")";
    }
}
