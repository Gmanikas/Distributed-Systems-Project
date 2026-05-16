package Android;

import shared.models.*;

import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;

import java.lang.NumberFormatException;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;


public class AndroidClient {

    private AndroidThread androidThread;
   
    private static final Gson gson = new Gson();


    public AndroidClient(AndroidThread thread) {
        this.androidThread = thread;
    }


    public String handleLogin(String playerId) {
        String response;

        if (playerId == null || playerId.isEmpty()) {
            response = "ERROR|No playerId entered";
        } else {
            androidThread.setCurrentPlayerId(playerId);
            double balance = getBalance();
            if (balance < 0.0) {
                response = "ERROR|Balance was not retrieved";
            } else {
                response = "OK|PlayerId submitted, " + Double.toString(balance);
            }
        }

        return response;
    }



    public String handleSearch(String payload) { // payload = 4,$$$,high
        String response;

        if (payload == null || payload.isEmpty()) {
            System.err.println("No filters where received.");
            response = "ERROR|No search filters entered";

        } else if (!payload.contains(",")) {            
            String gameName = payload.trim();
            SearchFilters searchFilters = new SearchFilters(gameName, true);

            System.out.println("MapReduce Search in progress for " + androidThread.getCurrentPlayerId() + "...");

            String searchResults = sendSearch(searchFilters);

            if (searchResults.equals("[]")) {
                    System.out.println("\n--- No " + gameName + " game found ---\n");
                    response = "OK|No game found";
                } else {
                    System.out.println("\n--- [Search Results] ---\n" + searchResults + "\n");
                    response = "OK|" + searchResults;
                }

        } else {
            String[] data = payload.split(",", -1);
            
            if (data.length != 3) {
                System.err.println("Wrong filters format was received.");
                response = "ERROR|Filters sent don't match the correct format";
            } else {
                try {
                    int stars = Integer.parseInt(data[0].trim());
                    String category = data[1].trim();
                    String risk = data[2].trim();
                    SearchFilters searchFilters = new SearchFilters(stars, risk, category);

                    System.out.println("MapReduce Search in progress for " + androidThread.getCurrentPlayerId() + "...");

                    String searchResults = sendSearch(searchFilters);

                    if (searchResults.equals("[]")) {
                        System.out.println("\n--- No games found matching the criteria ---\n");
                        response = "OK|No games found matching the criteria";
                    } else {
                        System.out.println("\n--- [Search Results] ---\n" + searchResults + "\n");
                        response = "OK|" + searchResults;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid stars format");
                    response = "ERROR|Invalid stars format";
                }
            }
        }

        return response;
    }

    private String sendSearch(SearchFilters searchFilters) {
        String request = "SEARCH|" + gson.toJson(searchFilters);
        String reply;

        try (Socket masterSocket = new Socket(androidThread.getMasterHost(), androidThread.getMasterPort());
             PrintWriter outToMaster = new PrintWriter(new OutputStreamWriter(masterSocket.getOutputStream()), true);
             BufferedReader inFromMaster = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))
            ){
            System.out.println("New connection to Master established: " + masterSocket.getInetAddress());
             
            outToMaster.println(request);

            reply = inFromMaster.readLine();

            if (reply == null || reply.startsWith("ERROR")) {
                System.err.println("ERROR while executing the Search. Received " + reply + " response from Master.");
                return "ERROR|Failed to complete the Search request";
            }

            return reply;


        } catch (IOException e) {
            System.err.println("ERROR while sending SEARCH request to Master. Details: " + e.getMessage() + "\n");
            e.printStackTrace();
            return "ERROR|Failed to complete the Search request";
        }
    }



    public String handlePlay(String payload) { // payload = GameName,BetAmount
        String response;

        if (payload == null || payload.isEmpty()) {
            response = "ERROR|No game name and bet amount was entered";
        } else {
            String[] data = payload.split(",", -1);

            if (data.length != 2) {
                response = "ERROR|Data sent does not match with game name and bet amount format";
            } else {
                String gameName = data[0].trim();
                boolean gameExists = gameExists(gameName);

                if (!gameExists) {
                    response = "ERROR|Game doesn't exist";
                } else {   
                    try {
                        double betAmount = Double.parseDouble(data[1].trim());

                        if (betAmount <= 0) {
                            response = "ERROR|Bet amount needs to be positive";
                        } else {
                            String currentPlayer = androidThread.getCurrentPlayerId();
                            double currentBalance = androidThread.getCurrentPlayerBalance(); // Το synchronized είναι στο getter
                            
                            synchronized (androidThread.balanceLock) {
                                if (betAmount > currentBalance) {
                                    response = "ERROR|Insufficient balance";
                                } else {
                                    Play p = new Play(currentPlayer, gameName, betAmount);
                                    
                                    double winAmount = sendPlay(p);
                                    
                                    if (winAmount < 0) { // Πέρνει την αρνητική τιμή -1.0 σε περίπτωση κάποιου ERROR
                                        response = "ERROR|Something went wrong";
                                    } else {
                                        currentBalance = (currentBalance - betAmount) + winAmount;
                                        androidThread.setCurrentPlayerBalance(currentBalance);
                                        
                                        if (winAmount > 0) {
                                            System.out.printf("Result: WIN! Payout: %.2f\n", winAmount);
                                            response = "OK|WON," + String.valueOf(winAmount);
                                        } else {
                                            System.out.println("Result: No win.");
                                            response = "OK|LOST";
                                        }
                                    }
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid bet amount format");
                        response = "ERROR|Invalid bet amount format";
                    }
                }
            }
        }
        return response;
    } 

    private double sendPlay(Play p) {
        String request = "PLAY|" + gson.toJson(p);
        String reply;

        try (Socket masterSocket = new Socket(androidThread.getMasterHost(), androidThread.getMasterPort());
             PrintWriter outToMaster = new PrintWriter(new OutputStreamWriter(masterSocket.getOutputStream()), true);
             BufferedReader inFromMaster = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))
            ){
            System.out.println("New connection to Master established:" + masterSocket.getInetAddress());

            outToMaster.println(request);

            reply = inFromMaster.readLine();

            if (reply == null || reply.startsWith("ERROR")) {
                System.err.println("ERROR while executing the Play. Received " + (reply != null ? reply : "no response" ) + " from Master.");
                return -1.0;
            }
        
            return Double.parseDouble(reply);

        } catch (IOException e) {
            System.err.println("ERROR while sending PLAY request to Master. Details: " + e.getMessage());
            return -1.0;
        } catch (NumberFormatException ex) {
            System.err.println("ERROR with different number formats. Details: " + ex.getMessage());
            return -1.0;
        }

    }
    


    public String handleAddBalance(String payload) { // payload = 100
        String response;

        try{
            double amountToAdd = Double.parseDouble(payload);

            if (amountToAdd <= 0) {
                System.err.println("Deposit amount needs to be a positive number.");
                response = "ERROR|Amount needs to be positive";
            } else {
                String currentPlayer = androidThread.getCurrentPlayerId();
                String temp = sendAddBalance(currentPlayer, amountToAdd);

                if (temp.startsWith("OK")) {
                    synchronized (androidThread.balanceLock) {
                        double currentBalance = androidThread.getCurrentPlayerBalance();
                        currentBalance += amountToAdd;
                        androidThread.setCurrentPlayerBalance(currentBalance);
                    }
                    System.out.println("Balance updated successfully.");
                } else {
                    System.err.println("Update didn't go through. Check server limits (Max 5000) or connection.");
                }
                response = temp;
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid amount format.");
            response = "ERROR|Invalid amount format";
        }
        return response;
    }

    private String sendAddBalance(String playerId, double amount) {
        String request = "ADD_BALANCE|" + playerId + "|" + amount;
        String reply;

        try (Socket masterSocket = new Socket(androidThread.getMasterHost(), androidThread.getMasterPort());
             PrintWriter outToMaster = new PrintWriter(new OutputStreamWriter(masterSocket.getOutputStream()), true);
             BufferedReader inFromMaster = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))
            ){
            System.out.println("New connection to Master established:" + masterSocket.getInetAddress());

            outToMaster.println(request);

            reply = inFromMaster.readLine();
            
            if (reply == null) {
                return "ERROR|Something went wrong";
            }
                
            return reply;
            
        } catch (IOException e) {
            System.err.println("ERROR while sending ADD_BALANCE request to Master. Details: " + e.getMessage());
            return "ERROR|Something went wrong";
        }
    }

    public double getBalance() {
        String request = "GET_BALANCE|" + androidThread.getCurrentPlayerId() + "|0";
        String reply;

        try (Socket masterSocket = new Socket(androidThread.getMasterHost(), androidThread.getMasterPort());
             PrintWriter outToMaster = new PrintWriter(new OutputStreamWriter(masterSocket.getOutputStream()), true);
             BufferedReader inFromMaster = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))
            ){
            System.out.println("New connection to Master established:" + masterSocket.getInetAddress() + "\n");

            outToMaster.println(request);

            reply = inFromMaster.readLine();

            if (reply != null && reply.startsWith("SUCCESS")) {
                String[] parts = reply.split("\\|");
                return Double.parseDouble(parts[1]);
            }
        } catch (IOException e) {
            System.err.println("ERROR while sending GET_BALANCE request to Master. Details: " + e.getMessage());
        }
        return -1.0;
    }

    

    public String handleRating(String payload) { // payload = GameName,4
        String response;

        if (payload == null || payload.isEmpty()) {
            System.err.println("No game and rating where received.");
            response = "ERROR|No game and rating entered";
        } else {
            String[] data = payload.split(",", -1);

            if (data.length == 1) {
                System.err.println("Invalid payload format");
                response = "ERROR|Invalid payload format";
            } else {
                try {
                    String gameName = data[0].trim();
                    int stars = Integer.parseInt(data[1].trim());

                    if (stars >= 1 && stars <=5) {
                        boolean gameExists = gameExists(gameName);

                        if (gameExists) {
                            String currentPlayer = androidThread.getCurrentPlayerId();

                            GameRating gameRating = new GameRating(currentPlayer, gameName, stars);

                            String result = sendRating(gameRating);
                            String[] info = result.split("\\|");

                            if (result.startsWith("OK|")) {
                                String averageGameRating = info[1];

                                String message = stars + " star rating submitted for " + gameName + "\n" + averageGameRating;
                                System.out.println(message);

                                response = "OK|" + message;
                            } else {
                                System.err.println(info[1]);
                                response = result;
                            }
                        } else {
                            System.out.println(gameName + " game does not exist");
                            response = "ERROR|Game not found";
                        }
                    } else {
                        System.err.println("Stars must range from 1 to 5");
                        response = "ERROR|Stars must range from 1 to 5";
                    }

                } catch (NumberFormatException e) {
                    System.err.println("Invalid stars rating format");
                    response = "ERROR|Invalid stars rating format";
                }
            }
        }
        return response;
    } 

    private String sendRating(GameRating gR) {
        String request = "RATE|" + gson.toJson(gR);
        String reply;

        try (Socket masterSocket = new Socket(androidThread.getMasterHost(), androidThread.getMasterPort());
             PrintWriter outToMaster = new PrintWriter(new OutputStreamWriter(masterSocket.getOutputStream()), true);
             BufferedReader inFromMaster = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))
            ){
            System.out.println("New connection to Master established:" + masterSocket.getInetAddress());

            outToMaster.println(request);

            reply = inFromMaster.readLine();

            if (reply.startsWith("SUCCESS")) {
                String[] parts = reply.split("\\|");
                reply = "OK|" + parts[1];
            } else {
                reply = "ERROR|Rating failed to be submitted";
            }
        } catch (IOException e) {
            System.err.println("ERROR while sending RATE request to Master. Details: " + e.getMessage());
            reply = "ERROR|Something failed while trying to fetch average rating for game";
        }
        return reply;
    }



    private boolean gameExists(String gameName) {
        String request = "GAME_EXISTS|" + gameName;
        String reply;

        try (Socket masterSocket = new Socket(androidThread.getMasterHost(), androidThread.getMasterPort());
             PrintWriter outToMaster = new PrintWriter(new OutputStreamWriter(masterSocket.getOutputStream()), true);
             BufferedReader inFromMaster = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))
            ){
            outToMaster.println(request);

            reply = inFromMaster.readLine();
        
            return reply.equals("YES");
        } catch (IOException e) {
            System.err.println("ERROR while sending GAME_EXISTS request to Master. Details: " + e.getMessage());
        }
        return false;
    }

}

