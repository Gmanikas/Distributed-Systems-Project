package Worker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import shared.models.*;


public class WorkerServer {
    
    private Map<String, Game> games;

    private Map<String, Double> gameProfits;
    private Map<String, Double> playerProfits;
    private Map<String, Double> providerProfits;
    private Map<String, Map<String, Double>> gameRatings;
    
    private Map<String, MyLinkedList<Integer>> gameBuffer; // Χρησιμοποιούμε MyLinkedList, η οποία ειναι φτιαγμένη με σκοπό να αντιμετωπίζει προβλήματα συγχρονισμού
    private static final int BUFFER_SIZE = 100; // Το μέγεθος της MyLinkedList. *Άμα αυτή αδειάζει πολύ γρήγορα (ο παίκτης περιμένει για να πάρει τον τυχαίο του αριθμό), μπορώ να μεγαλώσω το μέγεθος της
    
    private int port;

    public WorkerServer(int port) {
        this.port = port;
        this.games = new HashMap<>();
        this.gameProfits = new HashMap<>();
        this.playerProfits = new HashMap<>();
        this.providerProfits = new HashMap<>();
        this.gameBuffer = new  HashMap<>();
        this.gameRatings = new HashMap<>();
    }

    @SuppressWarnings("resource") // Το χρησιμοποιούμε για να «κρύψουμε» το warning του server για το γεγονός ότι δεν το κλείνουμε ποτέ (resource leak) 
    public void start() throws IOException {
        System.out.println("Worker listening on port "+port);
        ServerSocket server = new ServerSocket(port); // Περιμένει να συνδεθεί ο Master, και οταν συνδεθεί, δημιουργεί το Socket επικοινωνίας
                                                      // Ο server πρέπει να κλείσει μόνο οταν τελείωσει το πρόγραμμα
        while (true) {
            Socket masterSocket = server.accept();
            new WorkerThread(masterSocket, games, gameProfits, playerProfits, providerProfits, gameBuffer,gameRatings, BUFFER_SIZE).start();
        }

    }

    public static int getBufferSize() {
        return BUFFER_SIZE;
    }

}
