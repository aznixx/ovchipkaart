# OV-chipkaart Project Documentation

## Version History

### Version 1 - Basic
- Simple card with balance, top up, check in/out
- Fare = €0.89 base + €0.19 per km
- Transactions stored as plain strings
- Single screen JavaFX UI

### Version 2 - Realistic Features
- Travel classes (1st/2nd), transport types (train/bus/tram/metro)
- Peak/off-peak pricing, subscriptions, missed checkout penalty
- Personal vs anonymous cards, card expiry, auto top-up
- Transaction class with timestamps
- Save/load to file

### Version 3 - Accounts
- Login and register screen
- Account system with data saved to files
- UI reorganized into tabs (Travel, Top Up, Transactions)

### Version 4 - Full System (current)
- Minimum top-up amounts (€5 machine, €10 online)
- Blocked cards (auto-blocks if balance drops below -€30)
- Transfer discount (no base fare if you check in within 35 min of last checkout)
- Day pass (€22.50, free travel for the rest of the day)
- Travel statistics (total trips, distance, spending)
- Monthly summary
- Balance warnings (low balance, negative, blocked)
- Refund last trip
- Multiple cards per account
- Card selector in the UI
- Account tab with stats and card management

---

## Current File Structure

```
ovchipkaart/
├── pom.xml
├── DOCUMENTATION.md
├── src/main/java/nl/ovchipkaart/
│   ├── Main.java                          (console demo)
│   ├── model/
│   │   ├── Account.java                  (name, email, password, multiple OVCards)
│   │   ├── OVCard.java                   (balance, travel, top-up, stats, day pass, blocking)
│   │   ├── Station.java                  (name, km marker, available transport types)
│   │   ├── Transaction.java             (date/time, type, amount, balance, description)
│   │   ├── Traveler.java                (name + card, used in console demo)
│   │   ├── TravelClass.java            (enum: FIRST, SECOND)
│   │   ├── TransportType.java          (enum: TRAIN, BUS, TRAM, METRO)
│   │   └── Subscription.java           (enum: NONE, DAL_VOORDEEL, WEEKEND_VRIJ, ALTIJD_VOORDEEL)
│   └── ui/
│       ├── OVChipkaartApp.java           (JavaFX app, switches between login and main screen)
│       ├── LoginController.java          (handles login and register)
│       └── MainController.java           (card operations, 4 tabs)
├── src/main/resources/nl/ovchipkaart/ui/
│   ├── login-view.fxml                    (login/register form)
│   ├── main-view.fxml                     (tabbed main interface)
│   └── style.css                          (NS-themed styling)
```

---

## How It Works

### Accounts
- Register with name, email, password
- Choose personal or anonymous card at registration
- Account data saved in `ovchipkaart_accounts/` folder
- Each account can have multiple cards
- New accounts start with €50 balance on their first card

### Card Types
- **Personal**: no maximum balance, linked to your name
- **Anonymous**: max €150 balance, no name attached
- All cards expire after 5 years
- Cards get **blocked** if balance drops below -€30
  - Blocked cards cannot check in or top up
  - Unblocks automatically when balance goes back to €0 or above via top-up

### Traveling
1. Select a card (if you have multiple)
2. Pick a station and transport type
3. Click CHECK IN (needs at least €20 balance, or active day pass)
4. Pick your destination station
5. Click CHECK OUT (fare is deducted)

### Fare Calculation
| Transport | Base fare | Per km  |
|-----------|-----------|---------|
| Train     | €0.89     | €0.19   |
| Metro     | €1.08     | €0.10   |
| Bus       | €1.08     | flat    |
| Tram      | €1.08     | flat    |

**Multipliers:**
- 1st class (train only): 1.5x
- Peak hours (weekdays 6:30-9:00, 16:00-18:30): 1.2x

### Transfers (Overstappen)
- If you check in within 35 minutes of your last checkout, it counts as a transfer
- Bus/tram transfers: completely free (no fare)
- Train/metro transfers: no base fare, only the per-km rate

### Subscriptions
| Name            | Discount                          |
|-----------------|-----------------------------------|
| Dal Voordeel    | 40% off during off-peak hours     |
| Weekend Vrij    | Free travel on weekends           |
| Altijd Voordeel | 20% off always                    |

### Day Pass
- Costs €22.50
- Free travel for the rest of the day
- All transport types included
- No minimum balance needed for check-in when active

### Top-Up
| Method  | Minimum | Maximum |
|---------|---------|---------|
| Machine | €5.00   | €150.00 |
| Online  | €10.00  | €150.00 |

- Anonymous cards cannot exceed €150 total balance

### Auto Top-Up
- Set a threshold and an amount
- When balance drops below threshold after checkout, it automatically adds the amount

### Missed Checkout
- If you forget to check out, a €20 penalty is deducted
- Can be triggered via the button in the Travel tab

### Refund
- You can refund the last trip
- The fare is added back to your balance
- Trip count and spending stats are adjusted

### Balance Warnings
| Balance      | Warning                                    |
|--------------|--------------------------------------------|
| Below -€30   | BLOCKED - top up to unblock                |
| Below €0     | WARNING - negative balance                 |
| Below €10    | Low balance - consider topping up          |
| Below €20    | Below check-in minimum                     |

### Statistics
- Total trips taken
- Total distance traveled (km)
- Total money spent on fares
- Monthly summary (trips, spent, topped up)

### Multiple Cards
- Add personal or anonymous cards from the Account tab
- Switch between cards using the dropdown at the top
- Remove cards (must keep at least 1)

### Data Persistence
- Accounts saved as text files in `ovchipkaart_accounts/`
- Each card saved separately (account_email_card0.txt, card1.txt, etc.)
- Auto-saves on every action and on logout

---

## JavaFX UI Screens

### Login Screen
- Name, email, password fields
- Personal card checkbox
- Login and Register buttons

### Main Screen (4 tabs)
- **Travel tab**: station/transport picker, check in/out, class/subscription, day pass, missed checkout
- **Top Up tab**: machine top-up (min €5), online top-up (min €10), auto top-up settings
- **Transactions tab**: full history, refund button, monthly summary
- **Account tab**: add/remove cards, travel statistics, card info
- Top bar: card selector dropdown, balance, check-in status, balance warnings

---

## Running

Console demo:
```
mvn compile exec:java -Dexec.mainClass="nl.ovchipkaart.Main"
```

JavaFX app:
```
mvn javafx:run
```
