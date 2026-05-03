package Android;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.Base64;
import java.util.ArrayList;



public class AndroidServer {
    
    private static String getImageAsBase64() {

        System.out.println("\nLooking for app logo in path: app/data/logo/");
        File image = new File("app/data/logo/appLogo.png");
        //System.out.println("\nLooking for app logo in path: " + image.getAbsolutePath());
        System.out.println(image.exists() ? "It exists!" : "It doesn't exist.");

        try (FileInputStream fis = new FileInputStream(image);) {
            
            byte[] imageBytes = fis.readAllBytes();

            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            
            return base64;
            //System.out.println(base64);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    
    }

    public static void main(String[] args) {

        ArrayList<String> items = new ArrayList<>();
        items.add("Item 1");
        items.add("Item 2");
        items.add("Item 3");
        items.add("Item 4");

        String image = getImageAsBase64();

        try (ServerSocket androidServer = new ServerSocket(8080);) {

            System.out.println("\n=== Android System ===");
            System.out.println("Server started at: 8080 ...");

            while (true) {
                Socket androidSocket = androidServer.accept();
                new AndroidThread(androidSocket).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
