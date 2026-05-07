package Android;

import java.io.IOException;

import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class AndroidThread extends Thread {
    
    private Socket androidSocket;

    private static final String MASTERHOST = "localhost";
    private static final int MASTERPORT = 7000;
    
    private String message;
    private String response;

    private static String currentPlayerId = "";
    private static double currentPlayerBalance = 0.0;
    private static final Object balanceLock = new Object();

    public AndroidThread(Socket s) {
        this.androidSocket = s;
    }

    @Override
    public void run() {
        
        try (PrintWriter outToApp = new PrintWriter(new OutputStreamWriter(androidSocket.getOutputStream()), true);
             BufferedReader inFromApp = new BufferedReader(new InputStreamReader(androidSocket.getInputStream()));
            ){
            
            System.out.println("\n\nNew connection to App established: " + androidSocket.getInetAddress() + "\n");
            
            try (Socket masterSocket = new Socket(MASTERHOST, MASTERPORT);
                 PrintWriter outToMaster = new PrintWriter(new OutputStreamWriter(masterSocket.getOutputStream()), true);
                 BufferedReader inFromMaster = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))
                ){   
                
                AndroidClient androidClient = new AndroidClient(masterSocket, outToMaster, inFromMaster);
                System.out.println("New androidClient created.\n");

                System.out.println("New connection to Master established:" + masterSocket.getInetAddress() + "\n");

                while (true) {

                    message = inFromApp.readLine();

                    if (message != null) {
                        System.out.println("Received message from the App: " + message);
                    } else {
                        System.out.println("Lost connection to App."); // Το message γίνεται null μόνο αν πέσει η γραμμή, ή αν στείλουμε εμείς το null
                        return;
                    }

                    String[] data = message.split("\\|");

                    String command = data[0].trim().toUpperCase();
                    String payload = data[1].trim();

                    System.out.println("command: " + command + ", payload: " + payload);

                    switch (command) {
                        case "LOGIN"       -> { response = androidClient.handleLogin(payload); syncBalanceWithMasterServer(androidClient); }
                        case "PLAY"        -> response = androidClient.handlePlay(payload);
                        case "SEARCH"      -> response = androidClient.handleSearch(payload);
                        case "ADD_BALANCE" -> response = androidClient.handleAddBalance(payload);
                        case "RATE"        -> response = androidClient.handleRating(payload);
                    }


                    System.out.println("Sent response to the App: " + response + "\n");
                    outToApp.println(response);

                }
            } catch (IOException e) {
                System.err.println("Connection to Master was refused. Details:");
                e.printStackTrace();
            }


        } catch (IOException e) {
            System.err.println("Connection to App was refused. Details:");
            e.printStackTrace();
        }

    }

    public static void setPlayerId(String playerId) {
        currentPlayerId = playerId;
    }

    public static void syncBalanceWithMasterServer(AndroidClient androidClient) {

        synchronized(balanceLock) {
            double balanceFromServer = androidClient.getBalance(currentPlayerId);
            currentPlayerBalance = balanceFromServer;
            System.out.println("Balance synced for " + currentPlayerId);
        }
    }

    

    

}
