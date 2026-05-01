package SRG;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SRGServer {

    public static void main(String args[]){

        try (ServerSocket serverSocket = new ServerSocket(5000);) { // Το serverSocket θα κλείσει αυτόματα

            System.out.println("\n=== SRG System === \n"
                            + "SRG server started at port 5000"
            );

            while(true) {
                Socket client = serverSocket.accept();
                new SRGeneratorThread(client).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
