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

import org.w3c.dom.Text;

public class LossActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loss);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardLoss), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent i = getIntent();
        String lossAmountString = i.getStringExtra("Result");
        if (lossAmountString != null) {
            double lossAmount = Double.parseDouble(lossAmountString);
            // Afairoume th zhmia
            ActivityHandler.getInstance().subtractOverallBalance(lossAmount);
            // To emfanizoume sto .xml
            ((TextView) findViewById(R.id.tvLossAmount)).setText(lossAmount + " FUN removed from your Balance");
        }

        double overallBalance = ActivityHandler.getInstance().getOverallBalance();
        ((TextView) findViewById(R.id.tvNewBalanceLoss)).setText("New Balance: " + overallBalance);
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
                Intent i = new Intent(LossActivity.this, MainMenuActivity.class);
                startActivity(i);
            }
        });
    }

    public void handleLossOk(View v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LossActivity.this, "Returning to Menu", Toast.LENGTH_SHORT).show();
            }
        });
        this.proceed();
    }
}