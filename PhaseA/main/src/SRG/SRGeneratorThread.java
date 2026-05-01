package SRG;

import java.io.IOException;

import java.net.Socket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SRGeneratorThread extends Thread{

    Socket client;
    java.security.SecureRandom rand;
    String secret;

    public SRGeneratorThread(Socket client) {
        this.client = client;
        rand = new SecureRandom();
    }

    @Override
    public void run(){

        BufferedReader in;
        PrintWriter out;

        try{

            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(),true);

            String input = in.readLine();

            if (input == null || input.isEmpty()) {
                out.println("No secret received");
                return;
            }

            secret = input.trim(); // Δεν το κάνουμε πριν, με το readLine(), γιατί αν το in δώσει null, όταν εφαρμοστεί το trim() στο null, θα προκύψει error

            while (!client.isClosed()){
                int number=rand.nextInt(); //generate random num

                String hash=sha256(number+secret);
                out.println(number+"|"+hash);

            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }





    private String sha256(String input) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();


        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not known", e);
        }
    }
}