package Android;

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

    private static String currentPlayerBalance = "";
    private static double currentPlayerId = 0.0;
    private static final Object balanceLock = new Object();

    public AndroidThread(Socket s) {
        this.androidSocket = s;
    }

    @Override
    public void run() {
        
        try (PrintWriter outToApp = new PrintWriter(new OutputStreamWriter(androidSocket.getOutputStream()), true);
             BufferedReader inFromApp = new BufferedReader(new InputStreamReader(androidSocket.getInputStream()));
            ){
            
            System.out.println("\nNew connection to App established: " + androidSocket.getInetAddress() + "\n");
            
            try (Socket masterSocket = new Socket(MASTERHOST, MASTERPORT);
                 PrintWriter outToMaster = new PrintWriter(new OutputStreamWriter(masterSocket.getOutputStream()), true);
                 BufferedReader inFromMaster = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))
                ){   
                
                System.out.println("New connection to Master established:" + masterSocket.getInetAddress() + "\n");

                while (true) {
                    message = inFromApp.readLine();

                    if (message != null) {
                        System.out.println("Received message from the app: " + message);
                    } else {
                        System.out.println("Received no message from the app");
                        return;
                    }

                    String[] data = message.split("\\|");

                    String command = data[0].trim().toUpperCase();
                    String payload = data[1].trim();

                    System.out.println("command: " + command + ", payload: " + payload);

                    switch (command) {
                        case "LOGIN": // To payload θα έχει τη μορφή PlayerId
                            if (payload.isEmpty()) {
                                response = "ERROR|No playerId entered";
                            } else {
                                response = "OK|PlayerId sumbitted";
                            }

                        case "SEARCH":
                            // ... Στελνουμε στον Master
                        
                    }


                    System.out.println("Sent response to the App:" + response + "\n");
                    outToApp.println(response);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
