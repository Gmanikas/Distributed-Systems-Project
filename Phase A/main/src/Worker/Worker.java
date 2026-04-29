package Worker;

import java.io.IOException;

/**
 * Worker: Η κλάση εκκίνησης του Worker Node.
 * Κάθε Worker εκτελείται ως αυτόνομη διεργασία (Process) στη RAM.
 */
public class Worker {

    public static void main (String[] args) throws IOException {

        // ΔΥΝΑΜΙΚΟΣ ΟΡΙΣΜΟΣ: Η θύρα λαμβάνεται από τα ορίσματα (args).
        // Αυτό επιτρέπει στον Master να σηκώνει πολλούς Workers στο ίδιο μηχάνημα
        // σε διαφορετικές θύρες (π.χ. 6001, 6002).
        if (args.length < 1) {
            System.err.println("Usage: java Worker <port>");
            return;
        }

        System.out.println("\n=== Worker System ===");

        // Δημιουργούμε τους Worker servers
        for (String i: args) {
            int port = Integer.parseInt(i);

            // Βάζουμε τη δημιουργία του server, μέσα σε Thread, ώστε να μπορούμε να δημιουργήσουμε πολλούς, με τη χρήση ενός τερματικού
            Thread t = new Thread(() -> {
                try {
                    WorkerServer server = new WorkerServer(port);
                    server.start();
                } catch (IOException e) {
                    System.err.println("ERROR on port " + port + ": " + e.getMessage());
                }        
            }); 
            
            t.start(); // Το ξεκινάμε
            System.out.println("New Worker started at port " + i);


        }   

        // int port = Integer.parseInt(args[0]);

        // // Αρχικοποίηση και εκκίνηση του Worker Server στη RAM.
        // // Ο Server θα αναλάβει το Business Logic και το MapReduce step.
        // WorkerServer server = new WorkerServer(port);
        // server.start();

    }

}

