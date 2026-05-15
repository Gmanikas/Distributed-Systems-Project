package com.example.luckygames.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.luckygames.ActivityHandler;
import com.example.luckygames.R;

public class WinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_win);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardWin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent i = getIntent();
        String winAmountString = i.getStringExtra("Result");

        if (winAmountString != null) {
            double winAmount = Double.parseDouble(winAmountString);
            // Prosthetoume to kerdos
            ActivityHandler.getInstance().addOverallBalance(winAmount);
            // To emfanizoume sto .xml
            ((TextView) findViewById(R.id.tvWinAmount)).setText(winAmount + " FUN Added to your Balance");
        }


        double overallBalance = ActivityHandler.getInstance().getOverallBalance();
        ((TextView) findViewById(R.id.tvNewBalanceWin)).setText("New Balance: " + overallBalance + " FUN");
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
                Intent i = new Intent(WinActivity.this, MainMenuActivity.class);
                startActivity(i);
            }
        });
    }

    public void handleWinOk(View v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(WinActivity.this, "Returning to Menu", Toast.LENGTH_SHORT).show();
            }
        });
        this.proceed();
    }
}