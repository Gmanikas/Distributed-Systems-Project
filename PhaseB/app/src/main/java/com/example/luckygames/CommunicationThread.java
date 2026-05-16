package com.example.luckygames;

import com.example.luckygames.activities.FindGameActivity;
import com.example.luckygames.activities.LossActivity;
import com.example.luckygames.activities.PlayActivity;
import com.example.luckygames.activities.RateActivity;
import com.example.luckygames.activities.ResultsActivity;
import com.example.luckygames.activities.SearchActivity;


import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStreamReader;
import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;

import java.io.IOException;

import com.example.luckygames.activities.AddTokensActivity;
import com.example.luckygames.activities.ChangePlayerActivity;
import com.example.luckygames.activities.LoginActivity;
import com.example.luckygames.activities.WinActivity;
import com.example.luckygames.shared.models.MyLinkedList;


public class CommunicationThread extends Thread {

    private AppCompatActivity UI;
    private final String IP;
    private final int PORT;
    private final MyLinkedList<String> toDoList; // Lista opou tha sugkentrwnoume ta request tou app


    String response;

    public CommunicationThread(String ip, int port, MyLinkedList<String> list) {
        this.UI = null;
        this.IP = ip;
        this.PORT = port;
        this.toDoList = list;
    }

    @Override
    public void run() {

        try (Socket socket = new Socket(IP, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) {

            toDoList.setConnectionStatus(true);

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    String command = toDoList.get();

                    if (command == null) {
                        break;
                    }

                    out.println(command);
                    Log.d("Success", "Sent command!");

                    response = in.readLine();
                    Log.d("Success", "Received response: " + response);

                    if (response == null || response.isEmpty()) {
                        Log.d("ERROR", "No response received from Android Server for the outcome of the command that was sent");
                        return;
                    }
                    String[] data = response.split("\\|");

                    if (data.length != 2) {
                        Log.d("ERROR", "Wrong response format was sent from Android Server");
                        return;
                    }
                    String result = data[0].trim().toUpperCase();
                    String typeOfResult = data[1].trim();

                    switch (result) {
                        case "OK":
                            switch (typeOfResult) {
                                case "No game found": // SEARCH
                                    makeToast("Game entered doesn't exist");
                                    break;
                                case "No games found matching the criteria": // SEARCH
                                    if (UI instanceof SearchActivity) {
                                        ((SearchActivity) UI).setLoadingStatus(false);
                                    }
                                    makeToast("No games found matching the criteria");
                                    break;

                                case "Balance updated": // ADD_BALANCE
                                    makeToast("Balance updated");
                                    // Epistrefoume sto activity_main
                                    if (UI instanceof AddTokensActivity) {
                                        ((AddTokensActivity) UI).proceed();
                                    }
                                    break;

                                default:
                                    if (typeOfResult.contains("PlayerId submitted")) { // LOGIN
                                        makeToast("Logged in successfully");

                                        String[] info = typeOfResult.split(",");
                                        double balance = Double.parseDouble(info[1].trim());
                                        ActivityHandler.getInstance().setOverallBalance(balance);

                                        // Allagh othonhs
                                        if (UI instanceof LoginActivity) {
                                            ((LoginActivity) UI).proceed();
                                        } else if (UI instanceof ChangePlayerActivity){
                                            ((ChangePlayerActivity) UI).proceed();
                                        }

                                    } else if (typeOfResult.startsWith("[")) { // SEARCH
                                        // Pame sto activity_results
                                        if (UI instanceof SearchActivity) { // Periptwsh pou eimaste sto SearchActivity
                                            ((SearchActivity) UI).proceed(ResultsActivity.class, typeOfResult);
                                        } else if (UI instanceof FindGameActivity) { // Periptvsh pou eimaste sto FindGameActivity
                                            ((FindGameActivity) UI).proceed(PlayActivity.class, typeOfResult);
                                        }

                                    } else if (typeOfResult.startsWith("WON")) { // PLAY
                                        String winAmount = typeOfResult.replace("WON,", "");
                                        // Deixnoume to apotelesma se neo activity
                                        if (UI instanceof PlayActivity) {
                                            ((PlayActivity) UI).proceed(WinActivity.class, winAmount);
                                        }
                                    } else if (typeOfResult.startsWith("LOST")) { // PLAY
                                        // Deixnoume to apotelesma se neo activity
                                        if (UI instanceof PlayActivity) {
                                            // Xrhsimopoioume to command, to opoio periexei to betAmount
                                            String betAmount = command.substring(command.lastIndexOf(",") + 1);
                                            ((PlayActivity) UI).proceed(LossActivity.class, betAmount);
                                        }

                                    } else if (typeOfResult.contains("star rating")) { // RATE
                                        makeToast(typeOfResult);
                                        if (UI instanceof RateActivity) {
                                            ((RateActivity) UI).proceed();
                                        }
                                    }
                                    break;
                            }
                            break;
                        case "ERROR":
                            switch (typeOfResult) {
                                case "No playerId entered": // LOGIN
                                    makeToast("Failed to login");
                                    break;
                                case "Balance was not retrieved": // LOGIN
                                    makeToast("Balance was not retrieved");
                                    break;

                                case "No search filters entered": // SEARCH
                                    makeToast("No search filters entered");
                                    break;
                                case "Filters sent don't match the correct format": // SEARCH
                                    makeToast("Filters sent don't match the correct format");
                                    break;
                                case "Invalid stars format": // SEARCH
                                    makeToast("Invalid stars format");
                                    break;
                                case "Failed to complete the Search request": // SEARCH
                                    makeToast("Failed to complete your request. Try again");
                                    // reset to activity_search
                                    break;

                                case "No game name and bet amount was entered": // PLAY
                                    makeToast("No game name and bet amount were entered");
                                    break;
                                case "Data sent does not match with game name and bet amount format": // PLAY
                                    makeToast("Wrong data format");
                                    break;
                                case "Game doesn't exist": // PLAY
                                    makeToast("Game doesn't exist");
                                    break;
                                case "Bet amount needs to be positive": // PLAY
                                    makeToast("Bet amount needs to be positive");
                                    break;
                                case "Insufficient balance": // PLAY
                                    makeToast("Insufficient Balance");
                                    break;
                                case "Invalid bet amount format": // PLAY
                                    makeToast("Bet amount needs to be a number");
                                    break;

                                case "Something went wrong": // PLAY & ADD_BALANCE
                                    makeToast("Something went wrong. Try again");
                                    break;

                                case "Amount needs to be positive": // ADD_BALANCE
                                    makeToast("Amount needs to be positive");
                                    break;
                                case "Invalid amount format": // ADD_BALANCE
                                    makeToast("Invalid amount format");
                                    break;

                                case "No game and rating entered": // RATE
                                    makeToast("No game and rating entered");
                                    break;
                                case "Invalid payload format": // RATE
                                    makeToast("Invalid format");
                                    break;
                                case "Game not found": // RATE
                                    makeToast("Game entered does not exist");
                                    break;
                                case "Invalid stars rating format": // RATE
                                    makeToast("Invalid rating format");
                                    break;
                                case "Rating failed to be submitted": // RATE
                                    makeToast("Connection error. Try again");
                                    break;
                                case "Something failed while trying to fetch average rating for game": // RATE
                                    makeToast("Connection error. Try again");
                                    break;
                                case "Stars must range from 1 to 5":
                                    makeToast("Stars must range from 1 to 5");

                                default:
                                    if (typeOfResult.startsWith("Deposit denied. Max limit is")) { // ADD_BALANCE
                                        makeToast("Deposit denied. Limit is 5000");
                                    }
                                    break;
                            }
                            break;
                    }
                } catch (InterruptedException e) {
                    Log.d("ERROR when extracting from toDoList", e.getMessage());
                }

            }
        } catch (IOException e) {
            System.err.println("Connection failed. Details: " + e.getMessage());
        }

    }

    public void setCurrentUI(AppCompatActivity activity) {
        this.UI = activity;
    }

    public void makeToast(String output) {
        if (UI != null) {
            UI.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UI, output, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
