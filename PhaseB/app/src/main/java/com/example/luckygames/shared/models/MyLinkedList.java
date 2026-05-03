package com.example.luckygames.shared.models;

import java.util.LinkedList;


public class MyLinkedList<T> {
    
    private LinkedList<T> list;
    private final int maxSize;
    private boolean broken;

    public MyLinkedList(int size) {
        this.list = new LinkedList<>();
        this.maxSize = size;
        setConnectionStatus(false);
    }

    public synchronized void put(T data) throws InterruptedException {

        while (list.size() == maxSize) {
            wait();
        }

        list.add(data);

        notifyAll();
    }

    public synchronized T get() throws InterruptedException {

        while (list.isEmpty() && !broken) {
            wait();
        }

        if (list.isEmpty() && broken) {
            return null;
        }

        T data = list.poll();

        notifyAll();

        return data;
    } 
    
    public synchronized void setConnectionStatus(boolean flag) { // true -> Υπάρχει σύνδεση, false -> Δεν υπάρχει
        this.broken = !flag; // Αν το ConnectionStatus = true, τότε δεν είναι broken (broken = false)
        notifyAll(); // «Ξυπνάει» όλα τα συνδεδεμένα με της λίστα Threads, και αφού broken = false, δεν πράττει κάποια ενέργεια που μπορεί να προκαλέσει ERROR
    }

}
