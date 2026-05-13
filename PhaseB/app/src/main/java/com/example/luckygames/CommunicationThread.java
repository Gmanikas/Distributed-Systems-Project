package com.example.luckygames;

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
import com.example.luckygames.activities.MainActivity;
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
                                case "No games found matching the criteria": // SEARCH
                                    // Pame sto activity_results
                                    makeToast("No games matching the criteria exists");
                                    break;

                                case "LOST": // PLAY
                                    // Deixnoume to apotelesma se neo activity
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
                                        if (UI instanceof MainActivity) {
                                            ((MainActivity) UI).proceed();
                                        } else if (UI instanceof ChangePlayerActivity){
                                            ((ChangePlayerActivity) UI).proceed();
                                        }

                                    } else if (typeOfResult.startsWith("[")) { // SEARCH //O monos tropos na elenxoume an yparxei -
                                        // Pame sto activity_results
                                        if (UI instanceof SearchActivity) {
                                            ((SearchActivity) UI).proceed(ResultsActivity.class, typeOfResult);
                                        }

                                    } else if (typeOfResult.startsWith("WON")) { // PLAY // Analogws
                                        String winAmount = typeOfResult.replace("WON,", "");
                                        // Deixnoume to apotelesma se neo activity

                                    } else if (typeOfResult.contains("star rating")) { // RATE // Analogws
                                        makeToast(typeOfResult);
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
                                case "Something went wrong": // PLAY
                                    makeToast("Something went wrong. Try again");
                                    break;
                                case "Invalid bet amount format": // PLAY
                                    makeToast("Bet amount needs to be a number");
                                    break;

                                case "Amount needs to be positive": // ADD_BALANCE
                                    makeToast("Amount needs to be positive");
                                    break;
                                case "Server limits reached or connection was lost": // ADD_BALANCE
                                    makeToast("Connection error. Try again");
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
                                    makeToast("Invalid stars rating format");
                                    break;
                                case "Rating failed to be submitted": // RATE
                                    makeToast("Conection error. Try again");
                                    break;
                                case "Something failed while trying to fetch average rating for game": // RATE
                                    makeToast("Connection error. Try again");
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
