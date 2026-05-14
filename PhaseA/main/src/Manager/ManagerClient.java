package Manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import com.google.gson.*;

import shared.models.Game;
import shared.models.UpdateRisk;

/**
 * ManagerClient: Η κονσόλα διαχείρισης.
 * Λειτουργεί ως Entry Point για την εισαγωγή δεδομένων (JSON) στο σύστημα.
 */
public class ManagerClient {
    private static final String MASTER_HOST = "localhost";
    private static final int MASTER_PORT = 7000;

    // Ο χρήστης δίνει το όνομα αρχείου
    private static final String DATA_DIR = "data/games/";
    static Scanner scanner = new Scanner(System.in);
    static Gson gson = new Gson();

    public static void main(String[] args) {
        while (true) {
            printMenu();
            System.out.print("Select an option: ");
            // Η readInt καθαρίζει το buffer για να μην "κολλάει" η επόμενη είσοδος
            int choice = readInt(scanner, 0, 6);

            switch (choice){
                case 1 -> execute("ADD_GAME", true);
                case 2 -> execute("REMOVE_GAME", true);
                case 3 -> executeUpdateRisk();
                case 4 -> execute("GET_GAME_STATS", false); // Διόρθωση σειράς μενού
                case 5 -> execute("GET_PLAYER_STATS", false);
                case 6 -> execute("GET_PROVIDER_STATS", false);
                case 0 -> {
                    System.out.println("Exiting...");
                    return;
                }
            }
        }
    }

    private static void execute(String command, boolean needsFile) {
        try {
            String request;
            if (needsFile) {
                System.out.print("Provide the game name: "); // Πληκτρολογείς π.χ. CyberPoker
                String inputName = scanner.nextLine().trim();

                // Καθαρίζουμε το όνομα, αν έχει .json, για την αναζήτηση
                String clearName;
                if (inputName.contains(".json")) {
                    String[] parts = inputName.split("\\."); // Με το [0], παίρνουμε το όνομα σκέτο
                    clearName = parts[0];
                } else {
                    clearName = inputName;
                }

                // Έλεγχος στην περίπτωση REMOVE_GAME, για το αν υπάρχει αυτό ή όχι
                if (command.equals("REMOVE_GAME") && !gameExists(clearName)) {
                    System.out.println("\"" + clearName + "\" is not in the system.");
                    return;
                }

                // ΑΥΤΟΜΑΤΗ ΔΙΟΡΘΩΣΗ: Προσθήκη .json αν λείπει
                String fileName = inputName.toLowerCase().endsWith(".json") ? inputName : inputName + ".json";

                Path path;
                if (fileName.contains(":") || fileName.startsWith("/")) {
                    path = Path.of(fileName);
                } else {
                    path = Path.of(DATA_DIR + fileName);
                }

                // Ανάγνωση του αρχείου (π.χ. data/games/CyberPoker.json)
                String json = readGameFromFile(path);

                if (command.equals("REMOVE_GAME")) {
                    Game g = gson.fromJson(json, Game.class);
                    request = command + "|" + g.getName();
                } else {
                    request = command + "|" + json;
                }
            } else {
                System.out.print("Enter ID/Name: ");
                String id = scanner.nextLine();
                request = command + "|" + id;

                // Έλεγχος για τα GET_PLAYER_STATS και GET_PROVIDER_STATS, για το αν υπάρχουν ή όχι
                if (command.equals("GET_GAME_STATS") && !gameExists(id)) {
                    System.out.println("\"" + id + "\" game does not exist.");
                    return;
                } else if (command.equals("GET_PLAYER_STATS") && !playerExists(id)) {
                    System.out.println("\"" + id + "\" player does not exist.");
                    return;
                } // else if (command.equals("GET_PROVIDER_STATS") && !providerExists(id)) {
                //     System.out.println("\"" + id + "\" provider does not exist.");
                //     return;
                // }
            
            }
            

            System.out.println("[CLIENT] Sending request to Master...");
            String response = sendRequest(request);
            System.out.println("Response: " + response);

        } catch (IOException e){
            System.err.println("ERROR: " + e.getMessage());
        }
    }




    private static void executeUpdateRisk() {
        try {
            System.out.print("Provide the file name of the game: ");
            String fileName = scanner.nextLine().trim();

            // Καθαρίζουμε το όνομα, αν έχει .json, για την αναζήτηση
            String clearName;
            if (fileName.contains(".json")) {
                String[] parts = fileName.split("\\.");
                clearName = parts[0];
            } else {
                clearName = fileName;
            }

            if (!gameExists(clearName)) {
                System.out.println("\"" + clearName + "\" game doesn't exist.");
                return;
            }

            fileName = fileName.toLowerCase().endsWith(fileName) ? fileName : fileName + ".json";
            Path path = fileName.contains(":") ? Path.of(fileName) : Path.of(DATA_DIR + fileName);

            // 1. Διάβασμα του ονόματος από το αρχείο
            String json = readGameFromFile(path);
            Game g = gson.fromJson(json, Game.class);
            String nameInRAM = g.getName(); // Το κλειδί που ψάχνουμε στη RAM

            // 2. Επιλογή νέου ρίσκου
            System.out.println("Select new risk level for '" + nameInRAM + "': \n0=LOW, 1=MEDIUM, 2=HIGH");
            int riskChoice = readInt(scanner, 0, 2);
            String riskString = switch (riskChoice) {
                case 0 -> "LOW";
                case 1 -> "MEDIUM";
                default -> "HIGH";
            };

            // 3. Δημιουργία του DTO
            UpdateRisk updateDto = new UpdateRisk(nameInRAM, riskString);
            String jsonUpdate = gson.toJson(updateDto);

            // 4. Αποστολή
            System.out.println("[CLIENT] Requesting risk update for: " + nameInRAM);
            String response = sendRequest("UPDATE_RISK|" + jsonUpdate);
            System.out.println("Response: " + response);

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }


    private static void printMenu() {
        System.out.println("\n=== MANAGER MENU ===");
        System.out.println("1. Add Game");
        System.out.println("2. Remove Game (Soft Delete)");
        System.out.println("3. Update Risk");
        System.out.println("4. Get Game Stats (MapReduce)");
        System.out.println("5. Get Player Stats (MapReduce)");
        System.out.println("6. Get Provider Stats (MapReduce)");
        System.out.println("0. Exit");
    }

    public static int readInt(Scanner in, int minVal, int maxVal){
        while (true){
            try {
                // Χρήση nextLine & parseInt για πλήρη καθαρισμό του input buffer
                String input = in.nextLine();
                int choice = Integer.parseInt(input);
                if (choice < minVal || choice > maxVal){
                    System.out.print("Invalid range. Choice: ");
                    continue;
                }
                return choice;
            } catch (NumberFormatException e ){
                System.out.print("Please enter a number: ");
            }
        }
    }

    private static String sendRequest(String request){
        // TCP επικοινωνία με τον Master (Port 7000)
        try (Socket socket = new Socket(MASTER_HOST, MASTER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            out.println(request); // Αποστολή εντολής
            return in.readLine(); // Αναμονή για OK/ERROR

        } catch (IOException e){
            return "CONNECTION ERROR: " + e.getMessage();
        }
    }

    private static String readGameFromFile(Path p) throws IOException {
        if (!Files.exists(p)){
            throw new IOException("File not found at: " + p.toAbsolutePath());
        }
        String rawJson = Files.readString(p);
        try {
            // Validation μέσω Deserialization πριν την αποστολή
            Game g = gson.fromJson(rawJson, Game.class);
            if (g == null || g.getName() == null){
                throw new IOException("Game name is missing in JSON");
            }
            return gson.toJson(g); // Επιστρέφουμε το καθαρό JSON
        } catch (JsonSyntaxException e){
            throw new IOException("Invalid JSON syntax: " + e.getMessage());
        }
    }

    private static boolean gameExists(String gameName) {
        String request = "GAME_EXISTS|" + gameName;
       
        try (Socket socket = new Socket(MASTER_HOST, MASTER_PORT);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
        
            out.println(request);
            String response = in.readLine();

            return response.equals("YES");

        } catch (IOException e) {
            System.err.println("[Sync Error]: Cannot fetch game information from server.");
        }
        return false;
    }

    private static boolean playerExists(String gameName) {
        String request = "PLAYER_EXISTS|" + gameName;
       
        try (Socket socket = new Socket(MASTER_HOST, MASTER_PORT);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
        
            out.println(request);
            String response = in.readLine();

            return response.equals("YES");

        } catch (IOException e) {
            System.err.println("[Sync Error]: Cannot fetch player information from server.");
        }
        return false;

    }

}

