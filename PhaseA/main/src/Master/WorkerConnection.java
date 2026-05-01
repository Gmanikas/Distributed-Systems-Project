package Master;

/**
 * WorkerConnection: Αναπαριστά έναν Worker Node στη μνήμη του Master.
 * Είναι Immutable (αμετάβλητη) για να διασφαλίζει Thread Safety,
 * καθώς πολλές διεργασίες του Master διαβάζουν ταυτόχρονα τις διευθύνσεις των Workers.
 */
public class WorkerConnection {

    private final String host; // Διεύθυνση IP ή localhost
    private final int port;    // TCP Port του Worker

    /**
     * Constructor: Κατά το initialization του Master,
     * οι Workers φορτώνονται στη RAM από το config file.
     */
    WorkerConnection(String host, int port){
        this.host = host;
        this.port = port;
    }

    // --- Getters: Χρησιμοποιούνται από τον Master για το Forwarding των requests ---

    public String getHost(){
        return this.host;
    }

    public int getPort(){
        return this.port;
    }
}
