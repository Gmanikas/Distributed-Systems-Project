package Android;

import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;


public class AndroidThread extends Thread {
    
    private Socket androidSocket;

    private static final String MASTERHOST = "localhost";
    private static final int MASTERPORT = 7000;
    
    private String message;
    private String response;

    private String currentPlayerId = "";
    private double currentPlayerBalance = 0.0;
    public static final Object balanceLock = new Object();

    public AndroidThread(Socket s) {
        this.androidSocket = s;
    }

    @Override
    public void run() {
        
        try (PrintWriter outToApp = new PrintWriter(new OutputStreamWriter(androidSocket.getOutputStream()), true);
             BufferedReader inFromApp = new BufferedReader(new InputStreamReader(androidSocket.getInputStream()));
            ){
            
            System.out.println("\n\n---New connection to App established: " + androidSocket.getInetAddress() + "---\n");   
                
            AndroidClient androidClient = new AndroidClient(this);
            System.out.println("New androidClient created.\n");
            
            while (true) {

                message = inFromApp.readLine();

                System.out.println("--------------------------------------------\n");

                if (message != null) {
                    System.out.println("->Received message from the App: " + message);
                } else {
                    System.out.println("---Lost connection to App---"); // Το message γίνεται null μόνο αν πέσει η γραμμή, ή αν στείλουμε εμείς το null
                    return;
                }
                
                String[] data = message.split("\\|", -1); // LOGIN|playerId ή SEARCH|stars,risk,category
                                                          // Το -1 κάνει το split να μην πετάει ERROR όταν το δεν υπάρχει |, θέτοντας απλά data[0] = message
                if (data.length == 1) {
                    System.err.println("Message received from the App does not contain |. Moving on...");
                    System.out.println("->Sent the following response to the App: No \"|\" detected inside the message\n");
                    outToApp.println("No \"|\" detected inside the message");
                    continue;
                }

                String command = data[0].trim().toUpperCase();
                String payload = data[1].trim();
                System.out.println("command: " + command + ", payload: " + payload);
                
                switch (command) {
                    case "LOGIN"       -> { response = androidClient.handleLogin(payload); syncBalanceWithMasterServer(androidClient); System.out.println("Player: " + currentPlayerId + ", Balance: " + currentPlayerBalance); }
                    case "SEARCH"      -> response = androidClient.handleSearch(payload);
                    case "PLAY"        -> response = androidClient.handlePlay(payload);
                    case "ADD_BALANCE" -> { response = androidClient.handleAddBalance(payload); System.out.println("Player: " + currentPlayerId + ", Balance: " + currentPlayerBalance); }
                    case "RATE"        -> response = androidClient.handleRating(payload);
                }
                System.out.println("->Sent the following response to the App: " + response + "\n");
                outToApp.println(response);
            }
        } catch (IOException e) {
            System.err.println("Connection to App was refused. Details:");
            e.printStackTrace();
        }
    }

    public String getMasterHost() {
        return MASTERHOST;
    }

    public int getMasterPort() {
        return MASTERPORT;
    }


    public void setCurrentPlayerId(String playerId) {
        synchronized (balanceLock) {
            currentPlayerId = playerId;
        }
    }

    public String getCurrentPlayerId() {
        synchronized (balanceLock) {
            return currentPlayerId;
        }
    }


    public void setCurrentPlayerBalance(double balance) {
        synchronized (balanceLock) {
            currentPlayerBalance = balance;
        }
    }

    public double getCurrentPlayerBalance() {
        synchronized (balanceLock) {
            return currentPlayerBalance;
        }
    }


    public void syncBalanceWithMasterServer(AndroidClient androidClient) {

        synchronized(balanceLock) {
            double balanceFromServer = androidClient.getBalance(currentPlayerId);
            currentPlayerBalance = balanceFromServer;
            System.out.println("Balance synced for " + currentPlayerId);
        }
    }

    

    

}
