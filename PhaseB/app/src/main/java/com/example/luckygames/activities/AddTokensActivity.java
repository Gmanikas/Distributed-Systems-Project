package com.example.luckygames.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import android.view.View;

import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
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

public class AddTokensActivity extends AppCompatActivity {

    private CommunicationThread communicationThread;

    private MyLinkedList<String> toDoList;

    private String playerId;

    private double overallBalance;
    private String balance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_tokens);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_tokens), (v, insets) -> {
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
                Intent i = new Intent(AddTokensActivity.this, MainMenuActivity.class);
                if (balance != null) { // Einai null otan patietai to HomeButton, xwris na exoume kanei kapoio ADD_BALANCE
                    double temp = Double.parseDouble(balance);
                    ActivityHandler.getInstance().addOverallBalance(temp);
                }
                startActivity(i);
            }
        });
    }

    public void handleAddBalance(View v) {
        EditText balanceView = findViewById(R.id.etAddAmount);
        balance =  balanceView.getText().toString();

        // Stelnoume thn entolh
        try {
            toDoList.put("ADD_BALANCE|" + balance);
        } catch (InterruptedException e) {
            Log.d("ERROR when adding to toDoList", e.getMessage());
        }
    }

    public void handleAddTokensHomeButton(View v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AddTokensActivity.this, "Returning to Menu", Toast.LENGTH_SHORT).show();
            }
        });
        proceed();
    }

}