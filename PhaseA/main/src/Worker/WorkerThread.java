package Worker;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import shared.models.*;
import SRG.*;

public class WorkerThread extends Thread {
    private final int bufferSize;
    private Socket socket;

    private Map<String, Game> games;

    private Map<String, Double> gameProfits;
    private Map<String, Double> playerProfits;
    private Map<String, Double> providerProfits;

    private Map<String, Map<String, Double>> gameRatings; // a map for the ratings. key is the game
    // the value is another map, in which the key is the player's Id, and the value is the rating

    private Map<String, MyLinkedList<Integer>> gameBuffer;

    Gson gson = new Gson();

    public WorkerThread(Socket socket, Map<String, Game> games, Map<String, Double> gameProfits, Map<String, Double> playerProfits, Map<String, Double> providerProfits, Map<String, MyLinkedList<Integer>> gameBuffer,Map<String, Map<String, Double>> gameRatings, int BUFFER_SIZE) {
        this.socket = socket;
        this.games = games;
        this.gameProfits = gameProfits;
        this.playerProfits = playerProfits;
        this.providerProfits = providerProfits;
        this.gameBuffer = gameBuffer;
        this.gameRatings=gameRatings;
        this.bufferSize=BUFFER_SIZE;
    }

    @Override
    public void run() {

        BufferedReader in = null;
        PrintWriter out = null;

        boolean toReducer = false;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Ανάγνωση δεδομένων από τον Master
            out = new PrintWriter(socket.getOutputStream(), true); // Γράψιμο δεδομένων προς τον Master 

            String inputLine = in.readLine();
            if (inputLine == null || inputLine.isEmpty()) {
                out.println("ERROR");
                return;
            }

            String[] data = inputLine.split("\\|"); // COMMAND|requestId|payload
            // Και στην περίπτωση που δεν υπάρχει | στο inputLine, δεν θα προκύψει error,
            // αλλά το περιεχόμενο του θα αντιστοιχηθεί στο data[0], και το data.length θα είναι 1,
            // άρα θα το καλύψει ο επόμενος έλεγχος που κάνουμε

            if (data.length != 3) { // Έλεγχος σωστής μορφής εισόδου από τον Master (To 4 είναι το RATE)
                out.println("ERROR|Invalid request format sent to Worker");
                return;
            }

            String command = data[0].trim().toUpperCase();
            String requestId = data[1].trim();
            String payload = data[2].trim();
            switch(command) {
                case "ADD_GAME":
                    try {
                        Game newGame = gson.fromJson(payload, Game.class);
                        String gameName = newGame.getName();

                        synchronized(games) {
                            if (games.containsKey(gameName)) {
                                Game existingGame = games.get(gameName);

                                // ΕΛΕΓΧΟΣ: Αν υπάρχει αλλά είναι ανενεργό, το επαναφέρουμε (Re-activate)
                                if (!existingGame.getStatus()) {
                                    existingGame.setStatus(true);
                                    existingGame.setRisk(newGame.getRisk()); // Ενημέρωση ρίσκου αν άλλαξε στο JSON
                                    existingGame.calcAutoFields();

                                    // Επανεκκίνηση SRG αν χρειάζεται (προαιρετικά, αν το thread είχε κλείσει)
                                    startSRGConnection(existingGame);

                                    out.println("OK|Game '" + gameName + "' was re-activated.");
                                    System.out.println("[WORKER] Game " + gameName + " was reactivated.");
                                } else {
                                    // Αν είναι ήδη ενεργό, τότε μόνο βγάζουμε σφάλμα
                                    out.println("ERROR|Game '" + gameName + "' is already active in the system.");
                                }
                            } else {
                                // Πρώτη φορά προσθήκη
                                newGame.setStatus(true);
                                newGame.calcAutoFields();
                                games.put(gameName, newGame);
                                startSRGConnection(newGame);
                                out.println("OK");
                                System.out.println("[WORKER] Game " + gameName + "  was added.");
                            }
                        }
                    } catch (Exception e) {
                        out.println("ERROR|" + e.getMessage());
                    }
                    break;

                case "REMOVE_GAME":
                    String gameNameToRemove = payload.trim(); // Καθαρισμός κενών
                    synchronized(games) {
                        if (games.containsKey(gameNameToRemove)) {
                            // Αν το παιχνίδι είναι ήδη status=false, ενημερώνουμε τον Manager
                            if (!games.get(gameNameToRemove).getStatus()) {
                                out.println("ERROR|Game '" + gameNameToRemove + "' is already inactive.");
                            } else {
                                games.get(gameNameToRemove).setStatus(false);
                                System.out.println("[WORKER] Game " + gameNameToRemove + " set to INACTIVE.");
                                out.println("OK|Game '" + gameNameToRemove + "' was removed successfully.");
                            }
                        } else {
                            // Αυτό το μήνυμα θα εμφανιστεί στον Manager αν το όνομα δεν υπάρχει στη RAM
                            out.println("ERROR|Game '" + gameNameToRemove + "' not found on this Worker node.");
                        }
                    }
                    break;

                case "GAME_EXISTS":
                    String gameToCheck = payload.trim();

                    synchronized (games) {
                        if (games.containsKey(gameToCheck)) {
                            out.println("YES");
                        } else {
                            out.println("NO");
                        }
                    }

                    break;
                
                case "PLAYER_EXISTS":
                    String playerToCheck = payload.trim();

                    synchronized (playerProfits) {
                        if (playerProfits.containsKey(playerToCheck)) {
                            out.println("YES");
                        } else {
                            out.println("NO");
                        }
                    }

                    break;
                
                case "RATE":
                    try {
                        GameRating gameRating = gson.fromJson(payload, GameRating.class);
                        String gameName = gameRating.getGameName();
                        String pId = gameRating.getPlayerId();
                        int stars = gameRating.getRating(); 


                        String result;
                        Game targetGame;
                        synchronized (games) {
                            targetGame = games.get(gameName);
                        }
                        if (targetGame == null){
                            System.out.println("The game was not found on this worker");
                            out.println("ERROR|Game not found");
                            break;
                        }

                        synchronized (gameRatings) {
                            if (!gameRatings.containsKey(gameName)){
                                gameRatings.put(gameName, new HashMap<>());
                                gameRatings.get(gameName).put("SYSTEM_INITIAL",(double) targetGame.getStars());
                            }

                            Map<String,Double> ratingsForGame = gameRatings.get(gameName);
                            //Add or Update the player's rating
                            // If pId already exists, it just replaces the value (no count increase)
                            ratingsForGame.put(pId,(double) stars);
                            

                            double total = 0;
                            for (double rating: ratingsForGame.values()){
                                total+=rating;
                            }
                            double avg = total/ratingsForGame.values().size();
                            result = String.format("{%s} now has an average rating of %.1f stars, out of total %d ratings",gameName,avg,ratingsForGame.values().size());
                            System.out.println("Rating submitted successfully");
                            out.println("SUCCESS|"+result);
                        }
                    } catch (JsonSyntaxException e) {
                        System.err.println("ERROR|Invalid JSON format");
                        out.println("ERROR|Invalid JSON format");
                    } catch (Exception e) {
                        System.err.println("ERROR|Something went wrong: " + e.getMessage());
                        out.println("ERROR|Something went wrong: " + e.getMessage());
                    }

                    break;

                case "PLAY":
                    Play play = gson.fromJson(payload, Play.class);
                    Game exists;
                    synchronized (games) {
                        exists = games.get(play.getGameName());
                    }
                    String message;
                    if (exists != null && exists.getStatus()) {
                        try {
                            double payout = executePlay(exists, play);
                            message = String.valueOf(payout);
                        } catch (Exception e) {
                            e.printStackTrace();
                            message = "[WORKER]" + (e.getMessage().equals("SRG_CONNECTION_LOST") ? "ERROR|SRG_LOST" : "ERROR");
                        }
                    } else {
                        message = "ERROR|GAME_NOT_FOUND";
                    }
                    out.println(message);
                    break;
                    
                case "SEARCH": 
                    SearchFilters search = gson.fromJson(payload, SearchFilters.class);
                    List<Game> filteredGames = filterGames(search);
                    String filteredJson = gson.toJson(filteredGames);
                    toReducer = sendToReducer(requestId, filteredJson);
                    out.println(toReducer ? "OK" : "ERROR");
                    break;

                case "GET_PLAYER_STATS": 
                    String pId = payload; // Χρησιμοποιούμε pId
                    Map<String, Double> playerProfitLoss = new HashMap<>();
                    synchronized (playerProfits) {
                        playerProfitLoss.put(pId, playerProfits.getOrDefault(pId, 0.0));
                    }
                    String jsonPlayerProfitLoss = gson.toJson(playerProfitLoss);
                    toReducer = sendToReducer(requestId, jsonPlayerProfitLoss);
                    out.println(toReducer ? "OK" : "ERROR");
                    break;

                case "UPDATE_RISK": 
                    try {
                        UpdateRisk update = gson.fromJson(payload, UpdateRisk.class);
                        String targetName = update.getGameName();
                        synchronized (games) {
                            if (games.containsKey(targetName)) {
                                Game g = games.get(targetName);
                                g.setRisk(update.getNewRisk());
                                g.calcAutoFields();
                                System.out.println("[WORKER] Risk updated: " + targetName);
                                out.println("OK");
                            } else {
                                out.println("ERROR|Game '" + targetName + "' not found in RAM.");
                            }
                        }
                    } catch (Exception e) {
                        out.println("ERROR|Update failed: " + e.getMessage());
                    }
                    break;
                
                case "GET_GAME_STATS": 
                    String gameStatsJson;
                    synchronized (gameProfits) {
                        gameStatsJson = gson.toJson(gameProfits);
                    }
                    toReducer = sendToReducer(requestId, gameStatsJson);
                    out.println(toReducer ? "OK" : "ERROR");
                    break;
                    
                case "GET_PROVIDER_STATS": 
                    String provName = payload;
                    SearchFilters providerFilter = new SearchFilters(provName);
                    List<Game> providerGames = filterGames(providerFilter);

                    Map<String, Double> provStatsMap = new HashMap<>();
                    synchronized (gameProfits) {
                        for (Game g : providerGames) {
                            provStatsMap.put(g.getName(), gameProfits.getOrDefault(g.getName(), 0.0));
                        }
                    }

                    String jsonProv = gson.toJson(provStatsMap);
                    toReducer = sendToReducer(requestId, jsonProv);
                    out.println(toReducer ? "OK" : "ERROR");
                    break;
                

                default:
                    out.println("ERROR|UNKNOWN_COMMAND");
                    break;
            } // Τέλος του switch(command)

        } catch (Exception e) {
            e.printStackTrace();
            if (out != null) {
                out.println("ERROR|" + e.getMessage());
            }
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private double executePlay(Game game, Play play) throws Exception {

        System.out.println("");
        Integer randomNumber = getRandomFromSRG(game.getName()); // Χρησιμοποιούμε Integer, επειδή μπορεί να πάρει και την τιμή null

        if (randomNumber == null) { // Η λίστα είναι άδεια και έχει χαθεί η σύνδεση με την SRG
            throw new Exception("SRG_CONNECTION_LOST"); // Σταματάει την executePlay()
        }

        double winFactor = 0.0;
        int jackpot = 0;
        double[] multipliers;

        String risk;
        synchronized (game) {
            risk = game.getRisk().toUpperCase();
        }

        switch (risk) { // Επιλογή των πολλαπλασιαστών και του jackpot του παιχνιδιού με βάσει το προκαθορισμένο riskLevel
            case "LOW":
                multipliers = new double[]{0.0, 0.0, 0.0, 0.1, 0.5, 1.0, 1.1, 1.3, 2.0, 2.5};
                jackpot = 10;
                break;

            case "MEDIUM":
                multipliers = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 1.0, 1.5, 2.5, 3.5};
                jackpot = 20;
                break;

            default: // Εδώ, αντί για case, βάζουμε default, ώστε σε περίπτωση που το riskLevel δεν ταιρίαζει με κανένα από τις προηγούμενες επιλογές, να βάζει πάντα ως επιλογή το HIGH. Αυτό ισχύει και αν riskLevel δεν είναι ούτε LOW,MEDIUM ή HIGH, κεφαλαία ή μικρά
                multipliers = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 6.5};
                jackpot = 40;
                break;
        }

        if (randomNumber % 100 == 0) {
            winFactor = jackpot;
        } else {
            int position = Math.abs(randomNumber % 10);
            winFactor = multipliers[position];
        }

        double payout = play.getBet() * winFactor;
        double profitOrLoss = payout - play.getBet(); // Θετικό για profit (payout > Bet), ή Αρνητικό για Loss (payout < Bet)

        synchronized(gameProfits) {
            gameProfits.put(game.getName(), gameProfits.getOrDefault(game.getName(), 0.0) - profitOrLoss); // Εδώ, όταν πάμε να προσθέσουμε το κέρδος/ζημία, ελέγχουμε αν το συγκεκριμένο παιχνίδι έχει ήδη αποθηκευμένο κέρδος/ζημία, στο οποίο πρέπει να προσθέσουμε.
        }                                                                                                               // Αυτο γίνεται με το getOrDefault(game.getGame, 0.0), το οποίο, αν το παιχνίδι δεν υπάρχει, αντιστοιχεί σε αυτό τη τιμή 0.0. Αν υπάρχει, δεν το πειράζει.
        synchronized(playerProfits) {
            playerProfits.put(play.getPlayerId(), playerProfits.getOrDefault(play.getPlayerId(), 0.0) + profitOrLoss); // Προσθέτουμε, γιατί η ζημία του game, είναι το κέρδος του player
        }
        synchronized(providerProfits) {
            providerProfits.put(game.getProvider(), providerProfits.getOrDefault(game.getProvider(), 0.0) - profitOrLoss); // Είναι ανάλογο με τη περίπτωση του game
        }

        return payout;
    }

    private void startSRGConnection(Game game) {
        MyLinkedList<Integer> list = new MyLinkedList<>(this.bufferSize);

        //MyLinkedList<Integer> list = new MyLinkedList<>(WorkerServer.getBufferSize());

        new SRGProducerThread(game, list, "127.0.0.1", 5000).start(); // Ξεκινάει το thread της παραγωγής των τυχαίων αριθμών

        System.out.println("SRG connection to "+ game.getName() + " started");

        synchronized(gameBuffer) {
            gameBuffer.put(game.getName(), list);
            gameBuffer.notifyAll(); // Ξυπνάει όλα τα threads που «κοιμόντουσαν», στοχεύοντας συγκεκριμένα σε αυτό που έψαχνε τη συγκεκριμένη λίστα
        }
    }

    private Integer getRandomFromSRG(String name) throws InterruptedException { // Χρησιμοποιούμε Integer, επειδή μπορεί να πάρει και την τιμή null

        MyLinkedList<Integer> random;

        synchronized(gameBuffer) {
            while (!gameBuffer.containsKey(name)) { // Έλεγχος για το αν έχει αρχικοποιηθεί η λίστα με τους τυχαίους αριθμούς του συγκεκριμένου αριθμού
                gameBuffer.wait(); // Το wait σταματάει το WorkerThread, μέχρι να αρχικοποιηθεί το MyLinkedList από το startSRGConnection
                // Επίσης, όσο αυτό το thread «κοιμάται», το gameBuffer δεν είναι απεκλεισμένο (synchronized()). Όταν, όμως «ξυπνήσει», επιστρέφει στην προηγούμενη του κατάσταση, αυτή όπου η πρόσβαση στο gameBuffer από άλλα threads έιχε αποκλειστεί
            }

            random = gameBuffer.get(name);
        }

        return random.get();
    }

    private List<Game> filterGames(SearchFilters search) {

        List<Game> filtered = new ArrayList<>();

        synchronized(games) {
            for (Game g : games.values()) {

                // Προϋποθέσεις για το φιλτράρισμα των παιχνιδιών
                String providerName = search.getProviderName();
                if (providerName != null) { // Αν το χρησιμοποιούμε για να φιλτράρουμε Providers
                    boolean name = g.getProvider().equalsIgnoreCase(providerName);

                    if (name) {
                        filtered.add(g);
                    }

                } else { // Αν το χρησιμοποιούμαι για να φιλτράρουμε Games
                    boolean status = g.getStatus();
                    boolean stars =  g.getStars() >= search.getMinStars();
                    boolean risk = g.getRisk().equalsIgnoreCase(search.getRiskLevel());
                    boolean bet = g.getBetCategory().equalsIgnoreCase(search.getBetCategory());

                    if (status && stars && risk && bet) {
                        filtered.add(g);
                    }
                }
            }
        }

        return filtered;
    }

    private boolean sendToReducer(String requestId, String jsonData) {

        String reducerHost = "127.0.0.1"; // Πρέπει να είναι αντίστοιχο με το Server του Reducer
        int reducerPort = 8000; //                      -//-

        Socket reducerSocket = null;
        // BufferedReader FromReducer = null;
        PrintWriter ToReducer = null;

        try {
            reducerSocket = new Socket(reducerHost, reducerPort);
            // FromReducer = new BufferedReader(new InputStreamReader(reducerSocket.getInputStream())); // Το input απο το Reducer
            ToReducer = new PrintWriter(reducerSocket.getOutputStream(), true); // Το output προς το Reducer

            ToReducer.println("SEND_DATA" + "|" + requestId + "|" + jsonData);
            return true;

        } catch (Exception e) {
            return false;

        } finally {
            if (reducerSocket != null) {
                try {
                    reducerSocket.close();
                } catch (IOException ex) { }
            }
        }
    }


}
