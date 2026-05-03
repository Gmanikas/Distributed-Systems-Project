package Android;

import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AndroidThread extends Thread {
    
    private Socket socket;

    public AndroidThread(Socket s) {
        this.socket = s;
    }

    @Override
    public void run() {
        
        try (PrintWriter out = new PrintWriter(socket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ){
            
            System.out.println("New connection established: " + socket.getInetAddress());





        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
