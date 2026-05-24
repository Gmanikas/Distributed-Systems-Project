package Player;

import shared.models.Game;
import shared.models.GameRating;
import shared.models.Play;
import shared.models.SearchFilters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * PlayerClient: Διαχειρίζεται την επικοινωνία με τον Master Server μέσω TCP Sockets.
 * Περιλαμβάνει διορθώσεις για τον έλεγχο ορίων και τη διαχείριση σφαλμάτων.
 */
public class PlayerClient {
    // Στοιχεία σύνδεσης με τον Master
    private static final String HOST = "localhost";
    private static final int PORT = 7000;

    private static final Gson gson = new Gson();

    /**
     * Αποστέλλει αίτημα αναζήτησης διαθέσιμων παιχνιδιών.
     * Χρησιμοποιεί MapReduce στον Master/Reducer.
     */
    public List<Game> sendSearch(SearchFilters sf) {
        String request = "SEARCH|" + gson.toJson(sf);

        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(request);

            String response = in.readLine();

            // Έλεγχος αν ο Master επέστρεψε σφάλμα ή κενό αποτέλεσμα
            if (response == null || response.startsWith("ERROR")) {
                System.err.println("[CLIENT] Search Error or No Results: " + response);
                return new ArrayList<>();
            }

            // Μετατροπή της JSON λίστας σε αντικείμενα Game
            java.lang.reflect.Type listType = new TypeToken<ArrayList<Game>>(){}.getType();
            return gson.fromJson(response, listType);

        } catch (IOException e) {
            System.err.println("[Search Error]: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Αποστέλλει αίτημα πονταρίσματος.
     * Επιστρέφει το ποσό κέρδους ή -1.0 αν το ποντάρισμα ακυρώθηκε (π.χ. Worker Offline).
     */
    public double sendPlay(Play p) {
        if (p == null || p.getBet() <= 0) return -1.0;

        String request = "PLAY|" + gson.toJson(p);

        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(request);

            String response = in.readLine();

            // Αν ο Master στείλει ERROR (π.χ. ο Worker είναι κάτω), το ποντάρισμα ακυρώνεται στον Master
            if (response == null || response.startsWith("ERROR")) {
                System.err.println("[PLAY DENIED]: " + (response != null ? response : "No Response"));
                return -1.0;
            }

            return Double.parseDouble(response);

        } catch (IOException | NumberFormatException e) {
            System.err.println("[Play Connection Error]: " + e.getMessage());
            return -1.0;
        }
    }

    /**
     * Προσθήκη υπολοίπου με απομακρυσμένο έλεγχο ορίου (Max 5000 FUN).
     */
    public boolean sendAddBalance(String playerId, double amount) {
        String request = "ADD_BALANCE|" + playerId + "|" + amount;

        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      
            out.println(request);
            String response = in.readLine();

            // Η απάντηση είναι πλέον SUCCESS|Balance:X ή ERROR|Limit exceeded
            if (response != null && response.startsWith("OK")) {
                System.out.println("[BALANCE UPDATED]: " + response.replace("OK|", ""));
                return true;
            } else {
                if (response != null) {
                    System.err.println("[DEPOSIT ERROR]: " + response.replace("ERROR|", ""));
                }
                return false;
            }
        } catch (IOException e) {
            System.err.println("[AddBalance Error]: " + e.getMessage());
            return false;
        }
    }

    public double getBalance(String playerId) {
        // Στέλνουμε το ID στον Master.
        String request = "GET_BALANCE|" + playerId + "|0";

        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(request);
            String response = in.readLine();

            // Αναμένουμε π.χ. "SUCCESS|1250.5"
            if (response != null && response.startsWith("SUCCESS")) {
                String[] parts = response.split("\\|");
                return Double.parseDouble(parts[1]);
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[Sync Error]: Cannot fetch balance from server. Using 0.0");
        }
        return 0.0;
    }

    public String sendRating(GameRating gR){
        String request = "RATE|" + gson.toJson(gR);
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(request);
            String response = in.readLine();

            // Αναμένουμε π.χ. "SUCCESS|1250.5"
            if (response.startsWith("SUCCESS")) {
                String[] parts = response.split("\\|");
                return parts[1];
            } else {
                return "Something failed while trying to fetch average rating for game";
            }
            
        } catch (IOException | NumberFormatException e) {
            System.err.println("[Sync Error]: Cannot fetch balance from server. Using 0.0");
            return "ERROR, something failed";
        }
        
    }

    public boolean gameExists(String gameName) {
        String request = "GAME_EXISTS|" + gameName;
       
        try (Socket socket = new Socket(HOST, PORT);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
        
            out.println(request);
            String response = in.readLine();

            return response.equals("YES");

        } catch (IOException e) {
            System.err.println("[Sync Error]: Cannot fetch balance from server. Using 0.0");
        }
        return false;

    }


}
