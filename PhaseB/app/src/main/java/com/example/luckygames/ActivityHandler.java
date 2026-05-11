package com.example.luckygames;

import com.example.luckygames.shared.models.MyLinkedList;

public class ActivityHandler {

    private static ActivityHandler instance;
    private final MyLinkedList<String> toDoList;
    private final CommunicationThread communicationThread;
    private final String IP = "10.0.2.2";
    private final int PORT = 8080;

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

    public void setPendingMessage(String message) {
        this.message = message;
    }

    public String getPendingMessage() {
        String temp = message;
        message = null;
        return temp;
    }

}
