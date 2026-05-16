package com.example.luckygames.activities;

import com.example.luckygames.shared.models.Game;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Spinner;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luckygames.ActivityHandler;
import com.example.luckygames.CommunicationThread;
import com.example.luckygames.R;
import com.example.luckygames.shared.models.MyLinkedList;

import java.util.List;
import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {

    private MyLinkedList<String> toDoList;
    private String playerId;
    private double overallBalance;
    String message;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Sundesh me to Communication Thread
        ActivityHandler.getInstance().getCommunicationThread().setCurrentUI(this);
        toDoList = ActivityHandler.getInstance().getToDoList();

        playerId = ActivityHandler.getInstance().getPlayerId();
        ((TextView) findViewById(R.id.tvPlayerId)).setText("PlayerId: " + playerId);

        overallBalance = ActivityHandler.getInstance().getOverallBalance();
        ((TextView) findViewById(R.id.tvBalance)).setText("Balance: " + overallBalance + " FUN");

        progressBar = findViewById(R.id.searchProgressBar);
    }

    protected void onResume() {
        super.onResume();
        ActivityHandler.getInstance().getCommunicationThread().setCurrentUI(this);
    }

    public void proceed(Class<? extends AppCompatActivity> activityClass, String games) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SearchActivity.this, activityClass);
                // Apofasizoume an tha perasoume data h oxi
                if (games != null) {
                    i.putExtra("Games", games);
                }
                startActivity(i);
            }
        });
    }

    public void handleSearch(View v) {
        Spinner minimumStarsSpinner = findViewById(R.id.spMinStars);
        Spinner categorySpinner = findViewById(R.id.spBetCat);
        Spinner riskLevelSpinner = findViewById(R.id.spRisk);

        String minimumStars = minimumStarsSpinner.getSelectedItem().toString();
        String category = categorySpinner.getSelectedItem().toString();
        String riskLevel = riskLevelSpinner.getSelectedItem().toString();

        List<String> messageBuilder = new ArrayList<>();

        if (minimumStars.equals("-")) {messageBuilder.add("number of stars");}
        if (category.equals("-")) {messageBuilder.add("a category");}
        if (riskLevel.equals("-")) {messageBuilder.add("a risk level");}

        if (messageBuilder.isEmpty()) {
            // Progress bar
            setLoadingStatus(true);
            try {
                toDoList.put("SEARCH|" + minimumStars + "," + category + "," + riskLevel);
            } catch (InterruptedException e) {
                Log.d("ERROR when adding to toDoList", e.getMessage());
            }
        } else {
            message = TextUtils.join(", ", messageBuilder);
            // An den einai mono mia mh sumplhrwmenh prosthetoume "and"
            if (messageBuilder.size() > 1) {
                int lastComma = message.lastIndexOf(",");
                message = message.substring(0, lastComma) + " and" + message.substring(lastComma + 1);
            }
            message = "Must select " + message;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(SearchActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void handleSearchHomeButton(View v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SearchActivity.this, "Returning to Menu", Toast.LENGTH_SHORT).show();
            }
        });
        proceed(MainMenuActivity.class, null);
    }

    public void setLoadingStatus(boolean isLoading) {
        runOnUiThread(() -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

}