package Master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import shared.models.Play;

import com.google.gson.*;

public class MasterServer {

    static Gson gson = new Gson();
    private List<WorkerConnection> workers;

    // RAM STORAGE: Υπόλοιπα παικτών
    private final Map<String, Double> playerBalance = new HashMap<>();

    public static final int REDUCER_PORT = 8000;
    public static final int PORT = 7000;
    private String configPath;

    public static final String defaultRequestId = "DEFAULT_ID";

    // ΑΠΟΜΑΚΡΥΣΜΕΝΟΣ ΕΛΕΓΧΟΣ: Μέγιστο επιτρεπτό όριο tokens στη RAM
    private static final double MAX_BALANCE_LIMIT = 5000.0;

    public MasterServer(String path) {
        this.configPath = path;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java MasterServer <config_path>");
            return;
        }
        try {
            MasterServer server = new MasterServer(args[0]);
            server.start();
        } catch (Exception e) {
            System.out.println("[FATAL] " + e.getMessage());
        }
    }

    private void start() throws IOException {
        initializeServer(); // Φόρτωση workers και σύνδεση με Reducer
        startServerLoop();  // Εκκίνηση αναμονής για Client αιτήματα
    }

    private void initializeServer() throws IOException {
        workers = new ArrayList<>();
        try {
            this.workers = loadFromConfig(Path.of(configPath));
        } catch (IOException e) {
            throw new IOException("Could not load workers config, server shutting down");
        }

        // Προσπάθεια ενημέρωσης του Reducer για το πλήθος των Workers (Barrier setup)
        int retries = 5;
        while (retries-- > 0) {
            try {
                notifyReducerOfCount("localhost", REDUCER_PORT, defaultRequestId, getActive());
                System.out.println("\n=== Master System ===");
                System.out.println("Successfully contacted Reducer at port " + REDUCER_PORT);
                return;
            } catch (IOException e) {
                System.err.println("Failed to notify Reducer. Retrying... (" + retries + " left)");
                try {
                    // «Πίανουμε» το instance του MasterServer με το this, και έτσι μπορούμε να χρησιμοποιήσουμε το this.wait(4000), για να το «παγώσουμε» για 4 δευτερόλεπτα
                    synchronized (this) {
                        this.wait(4000);
                    }


                } catch (InterruptedException ignored) {
                }
            }
        }
        throw new IOException("Reducer unavailable on startup, server shutdown.");
    }

    private void startServerLoop() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Master is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] Connection accepted from: " + socket.getRemoteSocketAddress());

                // Πολυνηματική εξυπηρέτηση: Κάθε αίτημα επεξεργάζεται στη μέθοδο process()
                new Thread(() -> handleClient(socket)).start();
            }
        }
    }


    /**
     * handleClient: Διαχειρίζεται τον κύκλο ζωής μιας σύνδεσης (Request-Response).
     * Διασφαλίζει ότι ο πελάτης θα λάβει απάντηση ακόμα και σε περίπτωση σφάλματος στη RAM.
     */
    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            // 1) ΛΗΨΗ αιτήματος (one line)
            String request = in.readLine();

            // Έλεγχος αν η σύνδεση έκλεισε πρόωρα
            if (request == null || request.isEmpty()) {
                return;
            }

            System.out.println("[SERVER] Received request: " + request);

            // 2) ΕΠΕΞΕΡΓΑΣΙΑ (εδώ εκτελούνται οι έλεγχοι Hashing και Tokens)
            String response;
            try {
                response = process(request);
            } catch (Exception e) {
                // ΔΙΟΡΘΩΣΗ: Προσθήκη pipe (|) για συμβατότητα με το split του Client
                response = "ERROR|" + e.getMessage();
            }

            // 3) ΑΠΟΣΤΟΛΗ απάντησης
            out.println(response);
            System.out.println("[SERVER] Sent response: " + response);

        } catch (IOException e) {
            // Σφάλμα στην επικοινωνία Socket
            System.err.println("[SERVER] Socket Connection Error: " + e.getMessage());
        } catch (Exception e) {
            // Γενικό σφάλμα συστήματος
            System.err.println("[SERVER] Unexpected Error: " + e.getMessage());
        }
    }


    private String process(String request) throws IOException {

        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8); // Μοναδικό ID

        if (request == null) {
            throw new IOException("ERROR no request");
        }

        // ΔΙΟΡΘΩΣΗ: Χρήση \\| για να μην "σκάει" το split στο pipe
        String[] data = request.split("\\|");

        String cmd = data[0].trim();
        String playerId = "";
        String payload = "";


        if (data.length == 2) {
            cmd = data[0].trim();
            payload = data[1].trim();
        } else if (data.length == 3) {
            cmd = data[0].trim();
            playerId = data[1].trim();
            payload = data[2].trim();
        }

        // Δημιουργία του εσωτερικού request για τους Workers
        String workerRequest = cmd + "|" + requestId + "|" + payload;

        // --- 1. Εντολή ADD_BALANCE με απομακρυσμένο έλεγχο ορίου (5000 FUN) ---
        if (cmd.equals("ADD_BALANCE")) {
            try {
                double amount = Double.parseDouble(payload);

                synchronized (playerBalance) {
                    double current = playerBalance.getOrDefault(playerId, 0.0);

                    // Έλεγχος ώστε να μην υπερβαίνει το όριο
                    if (current + amount > MAX_BALANCE_LIMIT) {
                        return "ERROR|Deposit denied. Max limit is " + MAX_BALANCE_LIMIT + " FUN.";
                    }

                    playerBalance.put(playerId, amount + current);
                    return "OK|Balance updated";
                }
            } catch (NumberFormatException e) {
                return "ERROR|Invalid amount format";
            }
        }

        // --- 2. Εντολές Hashing (ADD_GAME, REMOVE_GAME, UPDATE_RISK, ..._EXISTS, RATE) ---
        else if (cmd.equals("ADD_GAME") || cmd.equals("REMOVE_GAME") || cmd.equals("UPDATE_RISK") 
                 || cmd.equals("GAME_EXISTS") || cmd.equals("RATE")) {
            if (payload.isEmpty()) return "ERROR|No payload received";

            // ΔΙΟΡΘΩΣΗ: Χρήση της μεθόδου hashing για να βρεθεί ο σωστός Worker στη RAM
            //o replica του Worker1 είναι ο Worker2, δηλαδή ο αμέσως επόμενος.
            //Tου τελευταίου Worker, το replica είναι ο πρώτος, κάνει wrap around σαν κύκλος
            int primaryIdx = calculateWorkerFromPayload(payload);
            int replicaIdx = (primaryIdx + 1) % workers.size();
            //DEBUG
            System.out.println("Primary worker: Worker "+primaryIdx+1);
            System.out.println("Replica of worker "+ primaryIdx+" : Worker "+replicaIdx+1);
            //
            WorkerConnection wCon = workers.get(primaryIdx);
            WorkerConnection replica = workers.get(replicaIdx);

            // Προώθηση στον Worker (Ο Worker θα απαντήσει αν το βρήκε στη RAM του)
            String primaryResult;
            boolean sentToPrimary = false;
            boolean sentToReplica = false;
            try {
                primaryResult = forwardToWorker(wCon, workerRequest);
                sentToPrimary = true;
            } catch (IOException e){
                primaryResult = e.getMessage();
            }
            String replicaResult="";
            try {
                replicaResult = forwardToWorker(replica,workerRequest);
                sentToReplica = true;
            } catch (IOException e){
                replicaResult = e.getMessage();
            }

            if (!sentToPrimary && !sentToReplica) {
                System.err.println("[CRITICAL] Both Primary (" + primaryIdx + ") and Replica (" + replicaIdx + ") are down.");
                throw new IOException ( "ERROR Service Unavailable. Both primary and replica workers are offline.");
            }

            if (sentToPrimary) {
                if (!sentToReplica) {
                    System.out.println("[WARN] Primary worked, but Replica was down. Data not mirrored.");
                }
                return primaryResult;
            }


            System.out.println("[FAILOVER] Primary down. Returning result from Replica.");
            return replicaResult;


        }

        // Κάνουμε τον έλεγχο μέσω του playerBalance
        else if (cmd.equals("PLAYER_EXISTS")) {
            if (payload.isEmpty()) return "ERROR|No payload received";

            synchronized (playerBalance) {
                if (playerBalance.containsKey(payload)) {
                    return "YES";
                } else {
                    return "NO";
                }
            }
        }

        // --- 3. Εντολή PLAY (με λογική Refund) ---
        else if (cmd.equals("PLAY")) {
            if (payload.isEmpty()) throw new IllegalArgumentException("ERROR|No payload");

            Play play = gson.fromJson(payload, Play.class);
            String pid = play.getPlayerId();
            double bet = play.getBet();

            synchronized (playerBalance) {
                double current = playerBalance.getOrDefault(pid, 0.0);
                if (current < bet) return "ERROR|Insufficient funds";
                playerBalance.put(pid, current - bet); // Προσωρινή αφαίρεση
            }

            int idx = calculateWorkerFromPayload(payload);
            WorkerConnection primary = workers.get(idx);
            int replicaIdx = (idx + 1) % workers.size();
            WorkerConnection replica = workers.get(replicaIdx);

            String response="";
            try {
                response  = forwardToWorker(primary, workerRequest);
                System.out.println(response);
                System.out.println("Successfully reached primary Worker "+idx);

            } catch (IOException e){
                System.out.println("Failed to reach primary Worker "+idx+". Trying replica...");
                try {
                    response = forwardToWorker(replica, workerRequest);
                    System.out.println(response);
                    System.out.println("Successfully reached replica Worker "+replicaIdx);

                } catch (IOException ex){
                    System.out.println("Failed to reach both primary Worker "+idx+" and replica Worker " +
                            +replicaIdx+".");
                }
            }

            // ΔΙΟΡΘΩΣΗ: Αν ο Worker αποτύχει, επιστρέφουμε τα χρήματα (Refund)
            if (response.isEmpty()) {
                synchronized (playerBalance) {
                    playerBalance.put(pid, playerBalance.get(pid) + bet);
                }
                return response;
            }

            try {
                double winAmount = Double.parseDouble(response);
                if (winAmount > 0) {
                    synchronized (playerBalance) {
                        playerBalance.put(pid, playerBalance.get(pid) + winAmount);
                    }
                }
                return response;
            } catch (NumberFormatException e) {
                // Refund σε περίπτωση κακού format απάντησης
                synchronized (playerBalance) {
                    playerBalance.put(pid, playerBalance.get(pid) + bet);
                }
                return "ERROR|Invalid payout format from worker";
            }
        }

        // --- 4. Εντολές MapReduce (SEARCH, STATS) ---
        else if (cmd.equals("SEARCH") || cmd.equals("GET_GAME_STATS") 
                 || cmd.equals("GET_PROVIDER_STATS") || cmd.equals("GET_PLAYER_STATS")) {
            // Ειδοποίηση Reducer για το πλήθος των Workers (Barrier Setup)
            int activeWorkers = getActive();
            if (activeWorkers==0){
                throw new IOException("ALL WORKERS OFFLINE");
            }
            notifyReducerOfCount("localhost", REDUCER_PORT, requestId, getActive());

            for (int i=0;i<workers.size();i++) {
                WorkerConnection primary = workers.get(i);
                WorkerConnection replica = workers.get((i+1)%workers.size());
                boolean segmentSuccess = false; //if either of the primary or the replica reply, this becomes true
                try {
                    forwardToWorker(primary,workerRequest);
                    segmentSuccess = true;
                    System.out.printf("Successfully reached primary worker [Worker %d].",i);

                } catch (IOException e){
                    System.out.printf("Failed to contact primary worker [Worker %d], trying replica [Worker %d]%n",i,(i+1)%workers.size());
                    try {
                        forwardToWorker(replica,workerRequest);
                        segmentSuccess = true;
                        System.out.printf("Successfully reached replica [Worker %d]",(i+1)%workers.size());
                    } catch (IOException ioe){
                        System.out.println("Both primary and replica worker are down.");
                        System.out.println("Games saved in these workers will not show in results.");

                    }
                }

            }

            // Λήψη τελικού αποτελέσματος (Reduce Step)
            return askReducer("localhost", REDUCER_PORT, requestId);
        }
        //
        if (cmd.equals("GET_BALANCE")) {
            synchronized (playerBalance) {
                // Αν ο παίκτης μπαίνει για πρώτη φορά, του δίνουμε 1000 FUN
                if (!playerBalance.containsKey(playerId)) {
                    playerBalance.put(playerId, 1000.0);
                    System.out.println("[MASTER] New account created for: " + playerId);
                }
                return "SUCCESS|" + playerBalance.get(playerId);
            }
        }

        throw new IllegalArgumentException("ERROR unknown command " + cmd);
    }

    private String forwardToWorker(WorkerConnection info, String request) throws IOException {

        try (Socket s = new Socket(info.getHost(), info.getPort());
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            s.setSoTimeout(500);

            System.out.println("Sent request to worker at port " + info.getPort());

            out.println(request);

            String response = in.readLine();
            System.out.println("Sent data to worker at " + info.getPort());

            if (response == null) {
                throw new IOException("Worker at " + info.getPort() + " gave empty response or disconnected");
            }

            return response;
        } catch (SocketTimeoutException e) {
            System.err.println("Worker " + info.getPort() + " timed out! Moving to next...");
            throw new IOException("TIMEOUT");
        } catch (IOException e) {
            System.err.println("[FATAL] Worker at " + info.getPort() + " is unreachable");
            throw new IOException("OFFLINE");
        }

    }

    private static List<WorkerConnection> loadFromConfig(Path path) throws IOException {

        if (!Files.exists(path)) {
            throw new IOException("Configuration file not found");
        }

        List<String> workerInfo = Files.readAllLines(path);
        List<WorkerConnection> result = new ArrayList<>();

        for (String line : workerInfo) {
            String[] parts = line.split(":");
            WorkerConnection connection = new WorkerConnection(parts[0], Integer.parseInt(parts[1]));
            result.add(connection);
        }

        return Collections.unmodifiableList(result);
    }

    private int calculateWorkerFromPayload(String payload) {
        String name = "";
        try {
            JsonObject obj = gson.fromJson(payload, JsonObject.class);

            if (obj.has("gameName")) {
                // Περίπτωση UPDATE_RISK και GameRating (το DTO σου έχει πεδίο gameName)
                name = obj.get("gameName").getAsString();
            } else if (obj.has("name")) {
                // Περίπτωση ADD_GAME (το JSON έχει πεδίο name)
                name = obj.get("name").getAsString();
            } else if (obj.has("GameName")) {
                // Περίπτωση ADD_GAME (εναλλακτικό κλειδί)
                name = obj.get("GameName").getAsString();
            } else {
                name = payload.trim(); // Για REMOVE_GAME και EXISTS που στέλνεις σκέτο String
            }
        } catch (Exception e) {
            name = payload.trim();
        }

        // Το hash πρέπει να βασίζεται ΜΟΝΟ στο καθαρό όνομα "CyberPoker"
        return (name.hashCode() & Integer.MAX_VALUE) % workers.size();
    }

    private String askReducer(String host, int port, String requestId) throws IOException {
        // Ανοίγουμε το socket προς τον Reducer
        try (Socket socket = new Socket(host, port)) {

            // ΔΙΟΡΘΩΣΗ: Το timeout πρέπει να είναι >10 δευτερόλεπτα (όσο το wait του Reducer)
            socket.setSoTimeout(12000);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Ζητάμε το αποτέλεσμα για το συγκεκριμένο requestId
            out.println("GET_RESULT|" + requestId);

            // Ο Master περιμένει παθητικά εδώ (Blocking I/O) μέχρι ο Reducer
            // να κάνει notifyAll() και να στείλει το JSON.
            String result = in.readLine();

            if (result == null) {
                // Αν ο Reducer κλείσει τη σύνδεση χωρίς να στείλει τίποτα
                return "[]";
            }

            return result;

        } catch (SocketTimeoutException e) {
            // Αν οι Workers άργησαν πολύ και ο Reducer δεν απάντησε ποτέ
            return "ERROR|Reducer Timeout: Workers not responding";
        } catch (IOException e) {
            // Αν ο Reducer Server είναι κλειστός
            return "ERROR|Reducer Connection Failed";
        }
    }

    private String notifyReducerOfCount(String host, int port, String requestId, int workersCount) throws IOException {
        try (Socket s = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true)) {

            s.setSoTimeout(3000); // Μικρότερο timeout για το Barrier setup

            out.println("SET_WORKER_COUNT|" + requestId + "|" + workersCount);
            String ack = in.readLine();

            if (ack == null) throw new IOException("ERROR: Reducer disconnected during config");
            return ack;

        } catch (SocketTimeoutException e) {
            throw new IOException("ERROR: Reducer took too long to acknowledge worker count");
        } catch (IOException e) {
            throw new IOException("ERROR: Could not notify Reducer of " + workersCount + " workers");
        }
    }

//    private int getActiveWorkers(){
//        int activeWorkers = 0;
//
//        for (WorkerConnection con : workers) {
//            boolean connected = false;
//            int attempts = 0;
//
//            // Ο Master θα επιμείνει μέχρι να βρει τον Worker ή να εξαντλήσει 100 προσπάθειες
//            while (!connected && attempts < 100) {
//                try (Socket s = new Socket()) {
//                    // Το timeout των 1500ms λειτουργεί ως "φρένο" χωρίς sleep.
//                    // Αν ο Worker είναι κλειστός, η connect θα περιμένει 1.5 δευτερόλεπτο.
//                    s.connect(new java.net.InetSocketAddress(con.getHost(), con.getPort()), 1500);
//
//                    // Αν φτάσει εδώ, η σύνδεση πέτυχε
//                    activeWorkers++;
//                    connected = true;
//                    System.out.println("[MASTER] Found Worker at port: " + con.getPort());
//                } catch (IOException e) {
//                    attempts++;
//                    // Τυπώνουμε ανά 5 προσπάθειες για να ξέρεις ότι ο Master "ζει"
//                    if (attempts % 5 == 0) {
//                        System.err.println("[MASTER] Still waiting for Worker " + con.getPort() + "... (Attempt " + attempts + ")");
//                    }
//                }
//            }
//        }
//        return activeWorkers;
//    }

    private boolean isAlive(WorkerConnection con) {
        try (Socket s = new Socket()) {
            s.connect(
                    new InetSocketAddress(con.getHost(), con.getPort()),
                    1000
            );
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private int getActive(){
        int active =0;

        for (WorkerConnection con: workers){
            if (isAlive(con)) active++ ;
        }



        return active;
    }



}