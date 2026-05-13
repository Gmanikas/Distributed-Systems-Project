package com.example.luckygames.activities;

import android.annotation.SuppressLint;

import android.os.Bundle;

import android.view.View;

import android.content.Intent;

import android.util.Log;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luckygames.ActivityHandler;
import com.example.luckygames.CommunicationThread;
import com.example.luckygames.R;
import com.example.luckygames.shared.models.MyLinkedList;

public class MainActivity extends AppCompatActivity {

    private CommunicationThread communicationThread;

    private MyLinkedList<String> toDoList;

    private String playerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Sundesh me to Communication Thread
        ActivityHandler.getInstance().getCommunicationThread().setCurrentUI(this);
        toDoList = ActivityHandler.getInstance().getToDoList();
    }

    @Override
    protected void onResume() { // Se periptwsh episkepshs tou Activity 2h fora. Kathws exei hdh dhmourghthei, den tha trexei to onCreate()
        super.onResume();

        // Bgazoume mhnuma, giati exei ginei Log Out
        Intent i = getIntent();
        String message = i.getStringExtra("LogOut");
        if (message != null) {
            // Den xreiazetai runOnUiThread()
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    public void proceed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(MainActivity.this, MainMenuActivity.class);
                ActivityHandler.getInstance().setPlayerId(playerId);
                startActivity(i);
            }
        });
    }

    public void handleLogin(View v) {
        EditText playerIdView = findViewById(R.id.etPlayerId);
        playerId = playerIdView.getText().toString();

        try{
            toDoList.put("LOGIN|" + playerId);
        } catch (InterruptedException e) {
            Log.d("ERROR when adding to toDoList", e.getMessage());
        }
        Log.d("PlayerId", playerId);
    }
    
}