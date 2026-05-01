package Reducer;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;
// import com.google.gson.Gson;

/**
 * ReducerHandler: Διαχειρίζεται την επικοινωνία με Master και Workers.
 * Υλοποιεί το Reduction σε επίπεδο συνεδρίας (Session-based Aggregation).
 */
public class ReducerHandler extends Thread {
    private Socket connection;
    // private static final Gson gson = new Gson();

    /**
     * IN-MEMORY SESSION MANAGEMENT:
     * Κάθε requestId έχει το δικό του "κουβά" (StatsAggregator) στη RAM.
     */
    private static final Map<String, StatsAggregator> sessionMap = new HashMap<>();

    /**
     * LOW-LEVEL SYNCHRONIZATION:
     * Χρήση sessionLock αντί για έτοιμες thread-safe βιβλιοθήκες.
     */
    private static final Object sessionLock = new Object();

    public ReducerHandler(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()), true)) {

            String request = in.readLine();
            if (request == null) return;

            String[] parts = request.split("\\|");
            String command = parts[0].trim().toUpperCase();

            switch (command) {
                case "SET_WORKER_COUNT":
                    // Φάση Initialization: Ο Master ορίζει πόσους Workers περιμένουμε
                    handleMasterConfig(parts[1], parts[2], out);
                    break;

                case "SEND_DATA":
                    // Φάση Shuffle: Οι Workers στέλνουν τα ενδιάμεσα (Partial) αποτελέσματα (JSON String)
                    handleWorkerData(parts[1], parts[2]);
                    break;

                case "GET_RESULT":
                    // Φάση Reduce: Ο Master ζητάει το τελικό αποτέλεσμα
                    handleMasterRequest(parts[1], out);
                    break;
            }

        } catch (IOException e) {
            System.err.println("[Reducer Error]: " + e.getMessage());
        } finally {
            try { connection.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    /**
     * Δημιουργεί έναν νέο Aggregator στη RAM για ένα συγκεκριμένο requestId.
     */
    private void handleMasterConfig(String requestId, String countStr, PrintWriter out) {
        int workers = Integer.parseInt(countStr.trim());
        synchronized (sessionLock) {
            sessionMap.put(requestId, new StatsAggregator(workers));
        }
        System.out.println("[Reducer] Session " + requestId + " initialized for " + workers + " workers.");
        out.println("ACK_COUNT_SET");
    }

    /**
     * Λαμβάνει τα δεδομένα (JSON String) από έναν Worker και τα προωθεί στον Aggregator.
     * Ο Aggregator αναλαμβάνει εσωτερικά το parsing σε List ή Map.
     */
    private void handleWorkerData(String requestId, String jsonData) {
        StatsAggregator bucket;
        synchronized (sessionLock) {
            bucket = sessionMap.get(requestId);
        }

        if (bucket != null) {
            bucket.addPartialResults(jsonData);
        } else {
            System.err.println("[Reducer] Received data for expired or unknown session: " + requestId);
        }
    }

    /**
     * Επιστρέφει το τελικό αποτέλεσμα στον Master και καθαρίζει τη μνήμη RAM.
     *
     */
    private void handleMasterRequest(String requestId, PrintWriter out) {
        StatsAggregator bucket;
        synchronized (sessionLock) {
            bucket = sessionMap.get(requestId);
        }

        if (bucket == null) {
            out.println("{}");
            return;
        }

        // Η getFinalResults() μπλοκάρει (wait) μέχρι να έρθουν όλα τα δεδομένα
        // και επιστρέφει πλέον String (JSON).
        String finalResultJson = bucket.getFinalResults();
        out.println(finalResultJson);

        // Cleanup: Αφαίρεση της συνεδρίας από τη RAM μετά την ολοκλήρωση της εργασίας
        synchronized (sessionLock) {
            sessionMap.remove(requestId);
        }
    }
}

