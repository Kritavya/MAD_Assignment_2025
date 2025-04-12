package com.kritavya.lottieanimation;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;

public class MainActivity extends AppCompatActivity {

    private LottieAnimationView animationView;
    private Button pauseButton;
    private boolean isPlaying = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Initialize the animation view
        animationView = findViewById(R.id.animationView);
        
        // Initialize the pause button
        pauseButton = findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(v -> {
            if (isPlaying) {
                animationView.pauseAnimation();
                pauseButton.setText("Play");
            } else {
                animationView.resumeAnimation();
                pauseButton.setText("Pause");
            }
            isPlaying = !isPlaying;
        });
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left + v.getPaddingLeft(), 
                         systemBars.top + v.getPaddingTop(), 
                         systemBars.right + v.getPaddingRight(), 
                         systemBars.bottom + v.getPaddingBottom());
            return insets;
        });
    }
}