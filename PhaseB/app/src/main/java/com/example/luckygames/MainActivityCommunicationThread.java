package com.example.luckygames;

import android.util.Log;

import java.io.InputStreamReader;
import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;

import java.io.IOException;

import com.example.luckygames.shared.models.MyLinkedList;
public class MainActivityCommunicationThread extends Thread {

    private MyLinkedList<String> toDoList; // Lista opou tha sugkentrwnoume ta request tou app

    private final String IP;
    private final int PORT;

    String response;

    public MainActivityCommunicationThread(String ip, int port, MyLinkedList<String> list) {
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
                                case "":
//                                    runOnUiThread(() -> {
//
//                                    });
                            }
                        case "ERROR":
                            switch (typeOfResult) {
                                case "No username entered":
                                    // ...
                            }
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
