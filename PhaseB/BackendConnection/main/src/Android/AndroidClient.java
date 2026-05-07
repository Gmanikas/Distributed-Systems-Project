package Android;

import shared.models.*;

import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;

public class AndroidClient {
    
    private Socket masterSocket;
    private PrintWriter toMaster;
    private BufferedReader fromMaster;

    private String response = "";

    public AndroidClient(Socket socket, PrintWriter out, BufferedReader in) {
        this.masterSocket = socket;
        this.toMaster = out;
        this.fromMaster = in;
    }


    public String handleLogin(String playerId) {
        
        if (playerId.isEmpty()) {
            response = "ERROR|No playerId entered";
        } else {
            AndroidThread.setPlayerId(playerId);
            response = "OK|PlayerId sumbitted";
        }

        return response;
    }

    public String handleSearch(String payload) {
        // Το 

        return response;
    }

    public String handlePlay(String payload) {
        
        return response;
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

