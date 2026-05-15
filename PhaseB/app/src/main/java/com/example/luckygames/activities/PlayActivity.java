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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luckygames.ActivityHandler;
import com.example.luckygames.R;
import com.example.luckygames.MyAdapter;
import com.example.luckygames.shared.models.Game;
import com.example.luckygames.shared.models.MyLinkedList;
import com.google.gson.Gson;


import java.util.Arrays;
import java.util.List;

public class PlayActivity extends AppCompatActivity {

    private MyLinkedList<String> toDoList;
    private String playerId;
    private double overallBalance;
    private final Gson gson = new Gson();
    private Game[] game;
    private List<Game> gameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_play);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.play), (v, insets) -> {
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


        Intent i = getIntent();
        String gamesString = i.getStringExtra("Game");
        game = gson.fromJson(gamesString, Game[].class); // To Game[] ta metatrepei se ArrayList<Game>, alla tha einai panta mono ena to paixnidi
        gameList = Arrays.asList(game);

        // Xrhsimopoioume pali recycler, gaiti etsi exoume thesei ton Adaptora na douleuei
        RecyclerView gameView = findViewById(R.id.rvGamePlay);
        gameView.setLayoutManager(new LinearLayoutManager(this)); // To kanoume na einai katheti lista

        //Dhmiourgoume ton adaptor pou tha metatrepsei th List<Game>
        MyAdapter myAdapter = new MyAdapter(gameList);
        gameView.setAdapter(myAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityHandler.getInstance().getCommunicationThread().setCurrentUI(this);
    }

    public void proceed(Class<? extends AppCompatActivity> activityClass, String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(PlayActivity.this, activityClass);
                // Apofasizoume an tha perasoume data h oxi
                if (result != null) {
                    i.putExtra("Result", result);
                }
                startActivity(i);
            }
        });
    }

    public void handleBet(View v) {
        EditText betAmountView = findViewById(R.id.etBetAmount);
        String betAmount = betAmountView.getText().toString();

        try{
            toDoList.put("PLAY|" + game[0].getName() + "," + betAmount);
        } catch (InterruptedException e) {
            Log.d("ERROR when adding to toDoList", e.getMessage());
        }
        Log.d("Play", betAmount);
    }

    public void handlePlayHomeButton(View v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PlayActivity.this, "Returning to Menu", Toast.LENGTH_SHORT).show();
            }
        });
        this.proceed(MainMenuActivity.class, null);
    }
}