package com.example.luckygames.activities;

import android.os.Bundle;

import android.content.Intent;
import android.view.View;

import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCaller;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luckygames.ActivityHandler;
import com.example.luckygames.CommunicationThread;
import com.example.luckygames.R;
import com.example.luckygames.shared.models.MyLinkedList;

public class MainMenuActivity extends AppCompatActivity {

    private CommunicationThread communicationThread;

    private MyLinkedList<String> toDoList;

    private String playerId;
    private double overallBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_menu), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Sundesh me to Communication Thread
        ActivityHandler.getInstance().getCommunicationThread().setCurrentUI(this);
        toDoList = ActivityHandler.getInstance().getToDoList();

        //Intent i = getIntent();
        playerId = ActivityHandler.getInstance().getPlayerId();
        ((TextView) findViewById(R.id.tvPlayerId)).setText("PlayerId: " + playerId);

        overallBalance = ActivityHandler.getInstance().getOverallBalance();
        ((TextView) findViewById(R.id.tvBalance)).setText("Balance: " + overallBalance);

    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityHandler.getInstance().getCommunicationThread().setCurrentUI(this);
    }

    private void proceed(Class<? extends AppCompatActivity> activityClass, String message) { // Me to Class<...> mporoume na dexomaste opoiadhpote activity
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(MainMenuActivity.this, activityClass);
                if (message != null) {
                    i.putExtra("LogOut", message);
                }
                i.putExtra("PlayerId", playerId);
                i.putExtra("Balance", overallBalance);
                startActivity(i);
            }
        });
    }

    public void handleSearchGames(View v) {
        this.proceed(SearchActivity.class, null);
    }

    public void handlePlayGame(View v) {

    }

    public void handleAddTokens(View v) {
        this.proceed(AddTokensActivity.class, null);
    }

    public void handleRateGame(View v) {

    }

    public void handleLogOut(View v) {
        this.proceed(ChangePlayerActivity.class, "Logged out");
    }

}