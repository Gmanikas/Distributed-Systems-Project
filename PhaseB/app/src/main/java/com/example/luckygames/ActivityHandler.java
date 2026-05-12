package com.example.luckygames;

import com.example.luckygames.shared.models.MyLinkedList;

public class ActivityHandler {

    private static ActivityHandler instance;
    private final MyLinkedList<String> toDoList;
    private final CommunicationThread communicationThread;
    private final String IP = "10.0.2.2";
    private final int PORT = 8080;
    private String playerId;
    private double overallBalance = 0.0;

    private String message;

    private ActivityHandler() {
        this.toDoList = new MyLinkedList<>(100);
        communicationThread = new CommunicationThread(IP, PORT, toDoList);
        communicationThread.start();
    }

    public static synchronized ActivityHandler getInstance() {
        if (instance == null) {
            instance = new ActivityHandler();
        }
        return instance;
    }

    public MyLinkedList<String> getToDoList() {
        return toDoList;
    }

    public CommunicationThread getCommunicationThread() {
        return communicationThread;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return this.playerId;
    }

    public void setOverallBalance(double balance) {
        this.overallBalance += balance;
    }

    public double getOverallBalance() {
        return this.overallBalance;
    }

    public void resetOverallBalance() {
        // Prepei na brw tropo na pernw to balance apo to database
        overallBalance = 1000.0;
    }


}
