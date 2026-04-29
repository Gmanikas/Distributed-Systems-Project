package Reducer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Η κλάση ReducerServer αποτελεί την κεντρική μονάδα του Reducer συστήματος.
 * Υλοποιεί έναν TCP Server που ακούει στη θύρα 8000 και διαχειρίζεται
 * πολυνηματικά τις εισερχόμενες συνδέσεις από τον Master και τους Workers.
 */
public class ReducerServer {

    public static void main(String[] args) {
        // Δημιουργία στιγμοτύπου και εκκίνηση του εξυπηρετητή
        new ReducerServer().openServer();
    }

    /**
     * Ανοίγει το Socket επικοινωνίας και διαχειρίζεται τον κύκλο ζωής των συνδέσεων.
     * Πραγματοποιείται παράλληλη εξυπηρέτηση αιτημάτων.
     */
    void openServer() {
        /*
         * Δημιουργία ServerSocket στη θύρα 8000.
         * Παράμετρος backlog (50): Ορίζει το μέγιστο μέγεθος της ουράς αναμονής για
         * εισερχόμενες συνδέσεις, διασφαλίζοντας τη σταθερότητα κατά τη μαζική
         * αποστολή δεδομένων από πολλαπλούς Workers (MapReduce shuffling phase).
         */
        try (ServerSocket providerSocket = new ServerSocket(8000, 50)) {

            System.out.println("\n=== Reducer System ===");
            System.out.println("Server started at port 8000");
            System.out.println("Waiting for Master or Workers requests...");

            // Βρόχος αποδοχής συνδέσεων (Infinite loop) για τη συνεχή λειτουργία του κόμβου
            while (true) {
                try {
                    /*
                     * Η μέθοδος accept() αναστέλλει την εκτέλεση (blocking) μέχρι την
                     * εμφάνιση νέου αιτήματος σύνδεσης.
                     */
                    Socket connection = providerSocket.accept();
                    System.out.println("New connection accepted: " + connection.getInetAddress());

                    /*
                     * Ανάθεση της σύνδεσης σε ένα νέο νήμα (Thread) τύπου ReducerHandler.
                     * Η χρήση της start() επιτρέπει την άμεση επιστροφή του Server στον
                     * βρόχο αποδοχής, επιτυγχάνοντας υψηλή διαθεσιμότητα.
                     */
                    Thread t = new ReducerHandler(connection);
                    t.start();

                } catch (IOException e) {
                    // Διαχείριση σφαλμάτων σε επίπεδο μεμονωμένης σύνδεσης
                    // ώστε να μην επηρεάζεται η συνολική λειτουργία του Server.
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }

        } catch (IOException ioException) {
            // Διαχείριση κρίσιμων εξαιρέσεων κατά τη δέσμευση της θύρας (π.χ. Port Conflict).
            System.err.println("CRITICAL: Server failed to start on port 8000.");
            ioException.printStackTrace();
        }
    }
}
