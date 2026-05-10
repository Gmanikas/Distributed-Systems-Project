package com.example.luckygames;

import android.util.Log;

import java.io.InputStreamReader;
import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;

import java.io.IOException;

import com.example.luckygames.shared.models.MyLinkedList;
public class MainActivityCommunicationThread extends Thread {

    private final MainActivity UI;
    private final String IP;
    private final int PORT;
    private final MyLinkedList<String> toDoList; // Lista opou tha sugkentrwnoume ta request tou app

    String response;

    public MainActivityCommunicationThread(MainActivity ui, String ip, int port, MyLinkedList<String> list) {
        this.UI = ui;
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
                                case "PlayerId submitted": // LOGIN
                                    UI.makeToast("Logged in successfully");
                                    UI.proceed();
                                    break;
                                case "No games found matching the criteria": // SEARCH
                                    // Pame sto activity_results
                                    break;

                                case "LOST": // PLAY
                                    // Deixnoume to apotelesma se neo activity
                                    break;

                                default:
                                    if (typeOfResult.contains("-")) { // SEARCH //O monos tropos na elenxoume an yparxei -
                                        // Pame sto activity_results
                                    } else if (typeOfResult.startsWith("WON")) { // PLAY // Analoga
                                        String winAmount = typeOfResult.replace("WON,", "");
                                        // Deixnoume to apotelesma se neo activity
                                    }
                                    break;
                            }
                            break;
                        case "ERROR":
                            switch (typeOfResult) {
                                case "No playerId entered": // LOGIN
                                    UI.makeToast("Failed to login");
                                    break;

                                case "No search filters entered": // SEARCH
                                    UI.makeToast("No search filters entered");
                                    break;
                                case "Filters sent don't match the correct format": // SEARCH
                                    UI.makeToast("Filters sent don't match the correct format");
                                    break;
                                case "Invalid stars format": // SEARCH
                                    UI.makeToast("Invalid stars format");
                                    break;
                                case "Failed to complete the Search request": // SEARCH
                                    UI.makeToast("Failed to complete your request. Try again");
                                    // reset to activity_search
                                    break;

                                case "No game name and bet amount was entered": // PLAY
                                    UI.makeToast("No game name and bet amount were entered");
                                    break;
                                case "Data sent does not match with game name and bet amount format": // PLAY
                                    UI.makeToast("Wrong data format");
                                    break;
                                case "Game doesn't exist": // PLAY
                                    UI.makeToast("Game doesn't exist");
                                    break;
                                case "Bet amount needs to be positive": // PLAY
                                    UI.makeToast("Bet amount needs to be positive");
                                    break;
                                case "Insufficient balance": // PLAY
                                    UI.makeToast("Insufficient Balance");
                                    break;
                                case "Something went wrong": // PLAY
                                    UI.makeToast("Something went wrong. Try again");
                                    break;
                                case "Invalid bet amount format": // PLAY
                                    UI.makeToast("Bet amount needs to be a number");
                                    break;
                            }
                            break;
                    }


//                    runOnUiThread(() -> {
//                        // This part jumps back to the Main Thread safely
//                        Toast.makeText(context, "Data Sent!", Toast.LENGTH_SHORT).show();
//                    });

                } catch (InterruptedException e) {
                    Log.d("ERROR when extracting from toDoList", e.getMessage());
                }

            }


        } catch (IOException e) {
            System.err.println("Connection failed. Details: " + e.getMessage());
        }

    }
}
