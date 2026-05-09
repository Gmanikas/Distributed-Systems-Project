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
            response = "OK|PlayerId sumbitted";
        }

        return response;
    }



    public String handleSearch(String payload) { // Το Payload θα ΄ρχει τη μορφή 4,$$$,high
        String response;

        if (payload == null || payload.isEmpty()) {
            System.err.println("No filters where received.");
            response = "ERROR|No search filters entered";
        } else {
            String[] data = payload.split(",");
            
            if (data.length != 3) {
                System.err.println("Wrong filters format was received.");
                response = "ERROR|Filters sent don't match the correct format";
            } else {
                int stars = Integer.parseInt(data[0].trim());
                String category = data[1].trim();
                String risk = data[2].trim();
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
                return "";
            }

            // Μετατροπή της JSON λίστας σε αντικείμενα Game
            //java.lang.reflect.Type listType = new TypeToken<ArrayList<Game>>(){}.getType();
            //return gson.fromJson(response, listType);
            return reply;


        } catch (IOException e) {
            System.err.println("ERROR while sending the Search request to Master. Details: " + e.getMessage() + "\n");
            e.printStackTrace();
            return "";
        }
    }



    public String handlePlay(String payload) { // GameName,BetAmount
        String response;

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
        String reply;

        try (Socket masterSocket = new Socket(androidThread.getMasterHost(), androidThread.getMasterPort());
             PrintWriter outToMaster = new PrintWriter(new OutputStreamWriter(masterSocket.getOutputStream()), true);
             BufferedReader inFromMaster = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))
            ){
            System.out.println("New connection to Master established:" + masterSocket.getInetAddress() + "\n");

            outToMaster.println(request);

            reply = inFromMaster.readLine();

            if (reply == null || reply.startsWith("ERROR")) {
                System.err.println("ERROR while executing the Play. Received " + (reply != null ? reply : "no response" ) + " from Master.");
                return -1.0;
            }
        
            return Double.parseDouble(reply);

        } catch (IOException e) {
            System.err.println("ERROR while sending the Play request to Master. Details: " + e.getMessage());
            return -1.0;
        } catch (NumberFormatException ex) {
            System.err.println("ERROR with different number formats. Details: " + ex.getMessage());
            return -1.0;
        }

    }
    


    public String handleAddBalance(String payload) {
        String response;

        try{
            double amountToAdd = Double.parseDouble(payload);

            if (amountToAdd <= 0) {
                System.err.println("Deposit amount needs to be a positive number.");
                response = "ERROR|Amount needs to be positive";
            } else {
                String currentPlayer = androidThread.getCurrentPlayerId();
                boolean jobDone = sendAddBalance(currentPlayer, amountToAdd);

                if (jobDone) {
                    synchronized (androidThread.balanceLock) {
                        double currentBalance = androidThread.getCurrentPlayerBalance();
                        currentBalance += amountToAdd;
                        androidThread.setCurrentPlayerBalance(currentBalance);
                    }
                    System.out.println("Balance updated successfully.");
                    response = "OK|Balance updated";
                } else {
                    System.err.println("Update failed. Check server limits (Max 5000) or connection.");
                    response = "ERROR|Server limits reached or connection was lost";
                }
            }
        } catch (Exception e) {
            System.err.println("Invalid amount format.");
            response = "ERROR|Invalid amount format";
        }
        return response;
    }

    private boolean sendAddBalance(String playerId, double amount) {
        String request = "ADD_BALANCE|" + playerId + "|" + amount;
        String reply;

        try (Socket masterSocket = new Socket(androidThread.getMasterHost(), androidThread.getMasterPort());
             PrintWriter outToMaster = new PrintWriter(new OutputStreamWriter(masterSocket.getOutputStream()), true);
             BufferedReader inFromMaster = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()))
            ){
            System.out.println("New connection to Master established:" + masterSocket.getInetAddress() + "\n");

            outToMaster.println(request);

            reply = inFromMaster.readLine();
            
            if (reply != null && reply.startsWith("SUCCESS")) {
                //System.out.println(reply.replace("SUCCESS|", ""));
                return true;
            } else {
                //if (reply != null) {
                    //System.err.println(reply.replace("ERROR|", ""));
                //}
                return false;
            }
        } catch (IOException e) {
            System.err.println("ERROR while sending Add Balance request to Master. Details: " + e.getMessage());
            return false;
        }
    }

    public double getBalance(String payload) {

        return 0.0;
    }

    

    public String handleRating(String payload) {

        return "";
    } 

    public boolean gameExists(String gameName) {

        return true;
    }

}

