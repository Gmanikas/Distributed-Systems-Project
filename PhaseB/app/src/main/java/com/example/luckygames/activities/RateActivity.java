package com.example.luckygames.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luckygames.ActivityHandler;
import com.example.luckygames.R;
import com.example.luckygames.shared.models.MyLinkedList;

public class RateActivity extends AppCompatActivity {

    private MyLinkedList<String> toDoList;
    private String playerId;
    private double overallBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rate);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rate), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Sundesh me to Communication Thread
        ActivityHandler.getInstance().getCommunicationThread().setCurrentUI(this);
        toDoList = ActivityHandler.getInstance().getToDoList();

        playerId = ActivityHandler.getInstance().getPlayerId();
        ((TextView) findViewById(R.id.tvRatePlayerId)).setText("PlayerId: " + playerId);

        overallBalance = ActivityHandler.getInstance().getOverallBalance();
        ((TextView) findViewById(R.id.tvRateBalance)).setText("Balance: " + overallBalance + " FUN");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityHandler.getInstance().getCommunicationThread().setCurrentUI(this);
    }

    public void proceed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(RateActivity.this, MainMenuActivity.class);
                startActivity(i);
            }
        });
    }

    public void handleRate(View v) {
        EditText gameNameView = findViewById(R.id.etRateGameName);
        String gameName = gameNameView.getText().toString();

        EditText starsView = findViewById(R.id.etRatingStars);
        String stars = starsView.getText().toString();

        // Stelnoume thn entolh
        try {
            toDoList.put("RATE|" + gameName + "," + stars);
        } catch (InterruptedException e) {
            Log.d("ERROR when adding to toDoList", e.getMessage());
        }
        Log.d("Rate", gameName + "," + stars);
    }

    public void handleRateHomeButton(View v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RateActivity.this, "Returning to Menu", Toast.LENGTH_SHORT).show();
            }
        });
        proceed();
    }

}