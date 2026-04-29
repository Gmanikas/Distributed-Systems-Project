package Player;

import java.util.*;
import shared.models.*;

/**
 * PlayerMain: Υποστηρίζει δυναμική είσοδο Player ID και συγχρονισμό υπολοίπου.
 */
public class PlayerMain {

    //Στατικές μεταβλητές
    private static double currentBalance = 0.0;
    private static String currentPlayerId = "";
    private static final Object balanceLock = new Object();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PlayerClient client = new PlayerClient();

        System.out.println("=== CASINO PLAYER TERMINAL ===");

        // 1. Είσοδος του παίκτη με το δικό του ID
        System.out.print("Enter your Player ID to login: ");
        currentPlayerId = sc.nextLine().trim();

        if (currentPlayerId.isEmpty()) {
            System.out.println("Invalid ID. Exiting...");
            return;
        }

        // 2. Αρχικοποίηση υπολοίπου από τον Master (Sync με τη RAM του Master)
        // Στέλνουμε μια "κενή" κατάθεση 0 για να λάβουμε το τρέχον SUCCESS|Balance από τον Master
        // Ή αν ο Master σου έχει REGISTER_PLAYER, καλείς εκείνο.
        System.out.println("Syncing with server...");
        syncBalanceWithServer(client);

        // 3. Κύριο Μενού (Εκτυπώνεται τη πρώτη φορά από το syncBalanceWithServer())
        while (true) {
            

            String choice = sc.nextLine();
            switch (choice) {
                case "1" -> handleSearch(sc, client);
                case "2" -> handlePlay(sc, client);
                case "3" -> handleAddBalance(sc, client);
                case "4" -> handleRate(sc, client);
                case "5" -> {
                    System.out.print("Enter new Player ID: ");
                    currentPlayerId = sc.nextLine().trim();
                    syncBalanceWithServer(client);
                }
                case "0" -> System.exit(0);
                default ->  {System.out.println("Invalid option. Please try again."); 
                             printMenu();}
            }           
        }
    }

    /**
     * Βοηθητική μέθοδος για να συγχρονίζουμε το τοπικό balance με τη RAM του Master.
     */
    /**
     * Συγχρονίζει το τοπικό balance με τη RAM του Master.
     * Αν ο παίκτης είναι νέος, ο Master θα του δώσει αυτόματα 1000 FUN.
     */
    private static void syncBalanceWithServer(PlayerClient client) {
        synchronized (balanceLock) {
            double balanceFromServer = client.getBalance(currentPlayerId);
            currentBalance = balanceFromServer;
            System.out.println("[System]: Balance synced for " + currentPlayerId);
            printMenu();
        }
    }


    private static void handleSearch(Scanner sc, PlayerClient client) {
        try {
            System.out.print("Min Stars (1-5): ");
            int stars = Integer.parseInt(sc.nextLine());
            if (stars <= 0 || stars > 5) { // Έλεγχος
                System.out.println("Min Stars must range between 1 to 5.");
                printMenu();
                return;
            }

            System.out.print("Category ($, $$, $$$): ");
            String cat = sc.nextLine();
            if (!cat.trim().equals("$") && !cat.trim().equals("$$") && !cat.trim().equals("$$$")) {
                System.out.println("No such choice available. Category is either $, $ or $$$.");
                printMenu();
                return;
            }


            System.out.print("Risk (low, medium, high): ");
            String risk = sc.nextLine();
            if (!risk.toLowerCase().trim().equals("low") && !risk.toLowerCase().trim().equals("medium")
                && !risk.toLowerCase().trim().equals("high")) {
                System.out.println("Risk level ranges between low, medium, high.");
                printMenu();
                return;
            }

            SearchFilters sf = new SearchFilters(stars, risk, cat);
            
            new Thread(() -> {
                System.out.println("\n[System]: MapReduce search in progress for " + currentPlayerId + "...");
                List<Game> results = client.sendSearch(sf);

                if (results == null || results.isEmpty()) {
                    System.out.println("\n--- No games found matching criteria ---");
                } else {
                    System.out.println("\n--- [Search Results] ---");
                    for (Game g : results) {
                        System.out.println(g.getName() + " | Stars: " + g.getStars() +
                                " | Category: " + g.getBetCategory() +
                                " | Risk: " + g.getRisk());
                    }
                }
                printMenu();
            }).start();
        } catch (Exception e) {
            System.out.println("Error in search input.");
            printMenu();
        }
    }

    private static void handlePlay(Scanner sc, PlayerClient client) {
        try {
            System.out.print("Game Name: ");
            String name = sc.nextLine();

            boolean exists = client.gameExists(name.trim());

            if (exists) {
                System.out.print("Bet Amount: ");
                double bet = Double.parseDouble(sc.nextLine());

                if (bet <= 0) {
                    System.out.println("Your bet amount needs to be positive.");
                    printMenu();
                    return;
                }

                synchronized (balanceLock) {
                    if (bet > currentBalance) {
                        System.out.println("Error: Insufficient Balance!");
                        return;
                    }

                    // Χρήση του δυναμικού currentPlayerId
                    Play p = new Play(currentPlayerId, name, bet);
                    double winAmount = client.sendPlay(p);

                    if (winAmount >= 0) {
                        currentBalance = (currentBalance - bet) + winAmount;
                        if (winAmount > 0) {
                            System.out.printf("Result: WIN! Payout: %.2f FUN\n", winAmount);
                        } else {
                            System.out.println("Result: No win.");
                        }
                    } else {
                        System.out.println("System: Bet cancelled or Error. Tokens remained in your account.");
                    }
                }
            } else {
                System.out.println(name.trim() + " game does not exist.");
            }
        } catch (Exception e) {
            System.out.println("Invalid input for play.");
        }
        printMenu();
    }

    private static void handleAddBalance(Scanner sc, PlayerClient client) {
        try {
            System.out.print("Amount to add: ");
            double amount = Double.parseDouble(sc.nextLine());

            if (amount <= 0) {
                System.out.println("\n[ERROR]: Deposit amount must be a positive number!");
                printMenu(); // Εμφάνιση του μενού αμέσως μετά το σφάλμα
                return;
            }

            // Χρήση του δυναμικού currentPlayerId
            if (client.sendAddBalance(currentPlayerId, amount)) {
                synchronized (balanceLock) {
                    currentBalance += amount;
                }
                System.out.println("Balance updated successfully.");
            } else {
                System.out.println("Update failed: Check server limits (Max 5000) or connection.");
            }
        } catch (Exception e) {
            System.out.println("Invalid amount format.");
        }
        printMenu();
    }

    private static void handleRate(Scanner sc, PlayerClient client) {
        System.out.print("Game Name: ");
        String name = sc.nextLine();
        
        boolean exists = client.gameExists(name.trim());
        try {
            if (exists) {
                System.out.print("Rating (1-5): ");
                int stars = Integer.parseInt(sc.nextLine());
                while (stars < 1 || stars > 5){
                    System.out.print("Invalid rating. Enter a number in the 1-5 range: ");
                    stars = Integer.parseInt(sc.nextLine());
                }
                GameRating gameRating = new GameRating(currentPlayerId,name, stars);
                String response = client.sendRating(gameRating);
                System.out.println(stars + " star rating submitted for game: " + name + "\n" + response);
            } else {
                System.out.println(name.trim() + " game does not exist.");
            }
        } catch (NumberFormatException e){
            System.out.println("Invalid rating. Enter a number (1-5)");
        }
        printMenu();
    }

    private static void printMenu(){
        synchronized (balanceLock) {
                System.out.printf("\n[Player: %s | Balance: %.2f FUN]\n", currentPlayerId, currentBalance);
            }
            System.out.println("1. Search Games (Async MapReduce)");
            System.out.println("2. Play Game");
            System.out.println("3. Add Tokens (Add Balance)");
            System.out.println("4. Rate Game (1-5 stars)");
            System.out.println("5. Logout / Change Player");
            System.out.println("0. Exit");
            System.out.print("Choice: ");
    }




}