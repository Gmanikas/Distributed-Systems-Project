package Android;

import java.net.Socket;

public class AndroidThread extends Thread {
    
    private Socket socket;

    public AndroidThread(Socket s) {
        this.socket = s;
    }

    @Override
    public void run() {
        
    }

}
