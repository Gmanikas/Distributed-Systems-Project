package com.example.luckygames.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luckygames.ActivityHandler;
import com.example.luckygames.R;
import com.example.luckygames.shared.models.MyLinkedList;

public class FindGameActivity extends AppCompatActivity {

    private MyLinkedList<String> toDoList;
    private String playerId;
    private double overallBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_find_game);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.find_game), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Sundesh me to Communication Thread
        ActivityHandler.getInstance().getCommunicationThread().setCurrentUI(this);
        toDoList = ActivityHandler.getInstance().getToDoList();

        playerId = ActivityHandler.getInstance().getPlayerId();
        ((TextView) findViewById(R.id.tvPlayerId)).setText("PlayerId: " +playerId);

        overallBalance = ActivityHandler.getInstance().getOverallBalance();
        ((TextView) findViewById(R.id.tvBalance)).setText("Balance: " + overallBalance + " FUN");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityHandler.getInstance().getCommunicationThread().setCurrentUI(this);
    }

    public void proceed(Class<? extends AppCompatActivity> activityClass, String game) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(FindGameActivity.this, activityClass);
                // Apofasizoume an tha perasoume data h oxi
                if (game != null) {
                    i.putExtra("Game", game);
                }
                startActivity(i);
            }
        });
    }

    public void handleFindGame(View v) {
        EditText gameNameView = findViewById(R.id.etGameName);
        String gameName = gameNameView.getText().toString();

        try{
            toDoList.put("SEARCH|" + gameName);
        } catch (InterruptedException e) {
            Log.d("ERROR when adding to toDoList", e.getMessage());
        }
        Log.d("Find Game", gameName);
    }

    public void handleSearchHomeButton(View v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FindGameActivity.this, "Returning to Menu", Toast.LENGTH_SHORT).show();
            }
        });
        proceed(MainMenuActivity.class, null);
    }

}