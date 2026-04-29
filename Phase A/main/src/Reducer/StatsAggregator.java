package Reducer;

import java.util.*;
import shared.models.Game;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * StatsAggregator: Ο Monitor του Reducer στη RAM.
 * Διορθωμένος για να υποστηρίζει και Στατιστικά (Map) και Αναζήτηση (List<Game>).
 */
public class StatsAggregator {

    private final int expectedWorkers;
    private int receivedCount = 0;
    private static final Gson gson = new Gson();

    // Δομή για Στατιστικά (π.χ. PlayerID -> [10.0, -5.0, 20.0])
    private final Map<String, List<Double>> intermediateStats = new HashMap<>();

    // Δομή για Αναζήτηση (Συγκεντρωτική λίστα παιχνιδιών από όλους τους Workers)
    private final List<Game> mergedGames = new ArrayList<>();

    public StatsAggregator(int expectedWorkers) {
        this.expectedWorkers = expectedWorkers;
    }

    /**
     * Η κεντρική μέθοδος λήψης δεδομένων.
     * Καταλαβαίνει αυτόματα αν το JSON αφορά λίστα παιχνιδιών ή χάρτη στατιστικών.
     */
    public synchronized void addPartialResults(String jsonData) {
        try {
            if (jsonData.trim().startsWith("[")) {
                // Περίπτωση SEARCH: Λαμβάνουμε List<Game>
                java.lang.reflect.Type listType = new TypeToken<List<Game>>() {}.getType();
                List<Game> gamesFromWorker = gson.fromJson(jsonData, listType);
                if (gamesFromWorker != null) {
                    mergedGames.addAll(gamesFromWorker);
                }
            } else {
                // Περίπτωση STATS: Λαμβάνουμε Map<String, Double>
                java.lang.reflect.Type mapType = new TypeToken<Map<String, Double>>() {}.getType();
                Map<String, Double> statsFromWorker = gson.fromJson(jsonData, mapType);
                if (statsFromWorker != null) {
                    for (Map.Entry<String, Double> entry : statsFromWorker.entrySet()) {
                        intermediateStats.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                                .add(entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Aggregator] Error parsing partial data: " + e.getMessage());
        }

        receivedCount++;
        // Μόλις φτάσουμε τον αριθμό των workers, ειδοποιούμε τον Master που περιμένει στη getFinalResults
        if (receivedCount >= expectedWorkers) {
            this.notifyAll();
        }
    }

    /**
     * Επιστρέφει το τελικό αποτέλεσμα (είτε Map είτε List) σε μορφή JSON String.
     * Μπλοκάρει (wait) μέχρι να ολοκληρωθεί η συλλογή από όλους τους Workers.
     */
    public synchronized String getFinalResults() {
        // 1. Barrier: Αναμονή για όλους τους Workers
        while (receivedCount < expectedWorkers) {
            try {
                this.wait(); // Θα ξυπνήσει μόλις έρθουν όλα τα δεδομένα από τους Workers (addPartialResult)
                if (receivedCount < expectedWorkers) break;
            } catch (InterruptedException e) {
                return "[]";
            }
        }

        // 2. Επιστροφή αποτελέσματος βάσει τύπου

        // Αν η intermediateStats (τα στατιστικά) είναι άδεια,
        // σημαίνει ότι η εργασία ήταν SEARCH.
        // Πρέπει να επιστρέψουμε ΠΑΝΤΑ Array, ακόμα και αν είναι άδειο ([]).
        if (intermediateStats.isEmpty()) {
            return gson.toJson(mergedGames); // Αυτό θα επιστρέψει π.χ. [{}, {}] ή []
        }

        // 3. Αν είναι STATS, τότε μόνο επιστρέφουμε Object {}
        Map<String, Double> finalStats = new HashMap<>();
        double totalSum = 0;
        for (Map.Entry<String, List<Double>> entry : intermediateStats.entrySet()) {
            double sum = 0;
            for (Double val : entry.getValue()) sum += val;
            finalStats.put(entry.getKey(), sum);
            if (!entry.getKey().equalsIgnoreCase("Total")) totalSum += sum;
        }
        if (!finalStats.isEmpty()) finalStats.put("Total", totalSum);

        return gson.toJson(finalStats);
    }

}

