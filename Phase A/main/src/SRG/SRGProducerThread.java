package SRG;

import java.net.Socket;

import java.nio.charset.StandardCharsets; // UTF_8

import java.security.MessageDigest; // SHA-256
import java.security.NoSuchAlgorithmException;

import shared.models.Game;
import shared.models.MyLinkedList;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;

//import java.util.HexFormat;

public class SRGProducerThread extends Thread {

    private Game game;
    private MyLinkedList<Integer> buffer;
    private final String host;
    private final int port;

    public SRGProducerThread(Game game, MyLinkedList<Integer> buffer, String host, int port) {
        this.game = game;
        this.buffer = buffer;
        this.host = host;
        this.port = port;
    }

    private Socket socket = null;

    @Override
    public void run() {

        while (game.getStatus()) { // 1o loop // Με αυτό το loop, προσπαθούμε να ανακτήσουμε τη σύνδεση με τον SRG, που μπορεί να χάθηκε, επανελλημένα, μέχρι να το καταφέρουμε, ή μέχρι το συγκεκριμένο game να τεθεί inactive

            try {
                socket = new Socket(host, port); // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                PrintWriter toSRG = new PrintWriter(socket.getOutputStream(), true); // Έξοδος προς τον SRG
                BufferedReader fromSRG = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Είσοδος από τον SRG

                buffer.setSRGConnectionStatus(true); // Ενημερώνουμε τη λίστα, στην περίπτωση που είχε σπάσει η σύνδεση και ξαναδημιουργήθηκε

                toSRG.println((game.getHashKey())); // Στέλνουμε το κλειδί(secret) του game, για να μπορεί ο SRG να εφαρμόσει την κρυπτογραφία

                while (game.getStatus()) { // Η λίστα θα είναι πάντα γεμάτη, όσο το game της «υπάρχει» (status = true) // 2ο loop

                    String input = fromSRG.readLine();

                    if (input == null) { // Σημαίνει ότι έχει αποτύχει κάτι με τον SRG, άρα κάνουμε break και ξαναπροσπαθούμε
                        System.err.println("[SRG-Client] Connection lost");
                        break;
                    }

                    if (!input.contains("|")) { // Σημαίνει ότι δεν δέχτηκε το secret ο SRG
                        System.err.println("[SRG-Client] Server: " + input);
                        break;
                    }

                    String[] data = input.split("\\|");

                    int number = Integer.parseInt(data[0].trim());
                    String hashKey = data[1].trim(); // Το αποτέλεσμα που πρέπει να βγαίνει, όταν εφαρμόσουμε τον αλγόριθμο sha256 στον αριθμό

                    if (verification(number, game.getHashKey(), hashKey)) { // Εφαρμογή του αλγορίθμου sha256, με τον οποίο παράχθηκε ο αριθμός, ώστε να επιβεβαιώσουμε την εγκυρότητα του
                        buffer.put(number); // Όταν γεμίσει η ουρά, μπλοκάρεται η εξής λειτουργία από μόνη της, μέχρι να αδείασει από κάποιο στοχείο
                    }
                }

            } catch (Exception e) {
                buffer.setSRGConnectionStatus(false); // Χάθηκε η σύνδεση με τον SRG
                try {
                    synchronized (this) {
                        this.wait(5000); // Το Thread «κοιμάται» για 5 δευτερόλεπτα και ξαναπροσπαθεί
                    }
                } catch (InterruptedException exc) {
                    Thread.currentThread().interrupt(); // Αν το Thread διακοπεί όσο περιμένει στο wait(), το σταματάμε
                    break; // Βγαίνουμε απο το 1ο loop
                }

            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private boolean verification(int number, String secret, String hashKey) {

        try {
            String data = number + secret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Δημιουργεί μια hash class, η οποία χρησιμοποιεί τον αλγόριθμο "SHA_256"
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8)); // Εδώ, αρχικά, μετατρέπουμε το String dataσ σε bytes, και μετά εφαρμόζουμε τον αλγόριθμο "SHA-256" σε αυτά. Αυτό παράγει hash μεγέθους 256 bits/32 bytes (32 bytes = String)

            StringBuilder byteString = new StringBuilder();

            for (byte b : hashBytes) {
                byteString.append(String.format("%02x", b)); // Χτίζει το String από τα 32 bytes (hashBytes[]). Το "%02x" μασ επιτρέπει να διαβάσουμε αυτά τα bits, για να το μετατρέψουμε σε String πάλι
            }

            return byteString.toString().equals(hashKey);

            /* Ποιο γρήγρορος τρόπος για να δημιουργηθει το String απο τα bytes, απλά απαιτείται νεότερο μοντέλο της Java
            String result = HexFormat.of().formatHex(hashBytes);
            return result.equals(hashKey);
            */



        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not known", e);
        }
    }

}
