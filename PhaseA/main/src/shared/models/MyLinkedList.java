package shared.models;

import java.util.LinkedList;


public class MyLinkedList<T> {
    
    private LinkedList<T> list;
    private final int maxSize;
    private boolean broken; // Αν broken = true, σημαίνει οτι χάθηκε η σύνδεση με τον SRG

    public MyLinkedList(int size) {
        this.list = new LinkedList<>();
        this.maxSize = size;
        setSRGConnectionStatus(false);
    }

    public synchronized void put(T data) throws InterruptedException {

        while (list.size() == maxSize) {
            wait(); // «Πετάει» InterruptedException, σταματόντας την μέθοδο, μέχρι η συνθήκη του while να βγει ψευδής
        }

        list.add(data);

        notifyAll(); // «Ξυπνάει» όλα τα threads, τα οποία περιμέναν να γίνει αυτή η μέθοδος, για να συνεχίσουν
    }

    public synchronized T get() throws InterruptedException {

        while (list.isEmpty() && !broken) { // Περιμένει, όσο η λίστα είναι άδεια και η σύνδεση ΔΕΝ ΕΊΝΑΙ χαλασμένη 
            wait(); //                    -//-
        }

        if (list.isEmpty() && broken) { // Αν η λίστα είναι άδεια και η σύνδεση ΕΊΝΑΙ χαλασμένη, τότε επιστρέφει null;
            return null;
        }

        T data = list.poll(); // Βγάζει την κεφαλή της λίστας, δηλαδή την πρώτη τιμή

        notifyAll(); //                  -//-

        return data;
    } 
    
    public synchronized void setSRGConnectionStatus(boolean flag) { // true -> Υπάρχει σύνδεση, false -> Δεν υπάρχει
        this.broken = !flag; // Αν το ConnectionStatus = true, τότε δεν είναι broken (broken = false)
        notifyAll(); // «Ξυπνάει» όλα τα συνδεδεμένα με της λίστα Threads, και αφού broken = false, δεν πράττει κάποια ενέργεια που μπορεί να προκαλέσει ERROR
    }

}
