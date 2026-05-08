package Android;

import shared.models.*;

import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;

import java.lang.NumberFormatException;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;


public class AndroidClient {

    private AndroidThread androidThread; 
    
    private Socket masterSocket;
    
    private PrintWriter toMaster;
    private BufferedReader fromMaster;
    
    private static final Gson gson = new Gson();

    private String response = "";

    public AndroidClient(AndroidThread thread, Socket socket, PrintWriter out, BufferedReader in) {
        this.androidThread = thread;
        this.masterSocket = socket;
        this.toMaster = out;
        this.fromMaster = in;
    }


    public String handleLogin(String playerId) {
        
        if (playerId == null || playerId.isEmpty()) {
            response = "ERROR|No playerId entered";
        } else {
            androidThread.setCurrentPlayerId(playerId);
            response = "OK|PlayerId sumbitted";
        }

        return response;
    }



    public String handleSearch(String payload) { // Το Payload θα ΄ρχει τη μορφή 4,$$$,high
        
        if (payload == null || payload.isEmpty()) {
            response = "ERROR|No search filters entered";
        } else {
            String[] data = payload.split(",");
            
            if (data.length != 3) {
                response = "ERROR|Filters sent don't match the correct format";
            } else {
                int stars = Integer.parseInt(data[0].trim());
                String risk = data[1].trim();
                String category = data[2].trim();
                SearchFilters searchFilters = new SearchFilters(stars, risk, category);

                System.out.println("MapReduce Search in progress for " + androidThread.getCurrentPlayerId() + "...");
                
                String searchResults = sendSearch(searchFilters);

                if (searchResults == null || searchResults.isEmpty()) {
                    System.out.println("\n--- No games found matching the criteria ---\n");
                    response = "OK|No games found matching the criteria";
                } else {
                    System.out.println("\n--- [Search Results] ---\n" + searchResults);
                    response = "OK|" + searchResults;
                }
            }

        }

        return response;
    }

    private String sendSearch(SearchFilters searchFilters) {
        String request = "SEARCH|" + gson.toJson(searchFilters);

        try {
            toMaster.println(request);

            String response = fromMaster.readLine();

            if (response == null || response.startsWith("ERROR")) {
                System.err.println("ERROR while executing the Search. Received " + response + " response from Master.");
                return "";
            }

            // Μετατροπή της JSON λίστας σε αντικείμενα Game
            //java.lang.reflect.Type listType = new TypeToken<ArrayList<Game>>(){}.getType();
            //return gson.fromJson(response, listType);
            return response;


        } catch (IOException e) {
            System.err.println("ERROR while sending the Search request to Master. Details: " + e.getMessage());
            return "";
        }
    }



    public String handlePlay(String payload) { // GameName,BetAmount
        
        if (payload == null || payload.isEmpty()) {
            response = "ERROR|No game name and bet amount was entered";
        } else {
            String[] data = payload.split(",");

            if (data.length != 2) {
                response = "ERROR|Data sent does not match with game name and bet amount format";
            } else {
                String gameName = data[0].trim();
                boolean gameExists = gameExists(gameName);

                if (!gameExists) {
                    response = "ERROR|Game doesn't exist";
                } else {   
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
                }
            }
        }
        return response;
    } 

    private double sendPlay(Play p) {
        String request = "PLAY|" + gson.toJson(p);

        try {
            toMaster.println(request);

            String response = fromMaster.readLine();

            if (response == null || response.startsWith("ERROR")) {
                System.err.println("ERROR while executing the Play. Received " + (response != null ? response : "no response" ) + " from Master.");
                return -1.0;
            }
        
            return Double.parseDouble(response);

        } catch (IOException e) {
            System.err.println("ERROR while sending the Play request to Master. Details: " + e.getMessage());
            return -1.0;
        } catch (NumberFormatException ex) {
            System.err.println("ERROR with different number formats. Details: " + ex.getMessage());
            return -1.0;
        }

    }
    


    public String handleAddBalance(String payload) {

        return response;
    }

    public double getBalance(String payload) {

        return 0.0;
    }
    
    public String handleRating(String payload) {

        return response;
    } 

    public boolean gameExists(String gameName) {

        return true;
    }

}

