package com.example.luckygames.activities;

import com.example.luckygames.CommunicationThread;
import com.example.luckygames.MyAdapter;
import com.example.luckygames.shared.models.Game;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.view.View;
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

import com.example.luckygames.shared.models.MyLinkedList;
import com.google.gson.Gson;

import java.util.List;
import java.util.Arrays;


public class ResultsActivity extends AppCompatActivity {

    private CommunicationThread communicationThread;

    private MyLinkedList<String> toDoList;
    private String playerId;
    private double overallBalance;

    private final Gson gson = new Gson();
    private Game[] games;
    private List<Game> gamesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_results);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.results), (v, insets) -> {
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
        ((TextView) findViewById(R.id.tvBalance)).setText("Balance: " + overallBalance);


        Intent i = getIntent();
        String gamesString = i.getStringExtra("Games");
        games = gson.fromJson(gamesString, Game[].class); // To Game[] ta metatrepei se ArrayList<Game>, wste na kaluptoume thn periptwsh apostols parapanw apo enos game
        gamesList = Arrays.asList(games);

        RecyclerView results = findViewById(R.id.rvResults);
        results.setLayoutManager(new LinearLayoutManager(this)); // To kanoume na einai katheti lista

        //Dhmiourgoume ton adaptor pou tha metatrepsei th List<Game>
        MyAdapter myAdapter = new MyAdapter(gamesList);
        results.setAdapter(myAdapter);
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
                Intent i = new Intent(ResultsActivity.this, MainMenuActivity.class);
                startActivity(i);
            }
        });
    }

    public void handleResultsHomeButton(View v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ResultsActivity.this, "Returning to Menu", Toast.LENGTH_SHORT).show();
            }
        });
        proceed();
    }
}