package com.example.luckygames;

import Android.os.Bundle;
import Androidx.activity.EdgeToEdge;
import Androidx.appcompat.app.AppCompatActivity;
import Androidx.core.graphics.Insets;
import Androidx.core.view.ViewCompat;
import Androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Edw xekinaei to app
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}