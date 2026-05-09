package com.example.luckygames;

import android.annotation.SuppressLint;
import android.os.Bundle;

import android.view.View;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luckygames.shared.models.MyLinkedList;

public class MainActivity extends AppCompatActivity {

    MainActivityCommunicationThread communicationThread;

    private MyLinkedList<String> toDoList;
    private final String IP = "10.0.2.2";
    private final int PORT = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toDoList = new MyLinkedList<>(100); // Dhmiourgia ths listas, mesw tis opoias tha stelnoume ta request tou app

        // Dhmioyrgia tou Thread
        communicationThread = new MainActivityCommunicationThread(IP, PORT, toDoList);
        communicationThread.start();

    }

    @SuppressLint("SetTextI18n")
    public void fetch(View v) {
        v.setEnabled(false);
        Log.d("Success", "Button Disabled!"); // Emfanizei sto Logcat kapoio output

        Button vButton = (Button) v; // Metatrepoume to View se Button. Etsi xekleidwnontai kialles leitourgies
                                     // To Button einai paidi tou View
        vButton.setText("Clicked");

        try {
            toDoList.put("SEARCH");
        } catch (InterruptedException e) {
            Log.d("ERROR when adding to toDoList", e.getMessage());
        }
    }

    public void handlePlayerIdText(View v) {
//        EditText playerIdView = findViewById(R.id.playerIdText);
//        String playerId = playerIdView.getText().toString();
        try {
            //toDoList.put("LOGIN|"  + playerId);
            toDoList.put("LOGIN|123");
            toDoList.put("ADD_BALANCE|100");
            toDoList.put("SEARCH|4");
            toDoList.put("SEARCH");
            toDoList.put("SEARCH|");
            toDoList.put("PLAY|CyberPoker,30");
            toDoList.put("SEARCH|4,$$$,high");
        } catch (InterruptedException e) {
            Log.d("ERROR when adding to toDoList", e.getMessage());
        }
        //usernameView.setEnabled(false);
        //Log.d("Username", playerId);
        Log.d("PlayerId", "playerId");
    }

}