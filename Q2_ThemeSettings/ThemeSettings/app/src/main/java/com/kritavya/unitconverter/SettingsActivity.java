package com.kritavya.unitconverter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchDarkMode;
    private SharedPreferences sharedPreferences;
    public static final String PREFS_NAME = "theme_prefs";
    public static final String KEY_DARK_MODE = "dark_mode";
    private boolean initialThemeState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        initialThemeState = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        applyTheme(sharedPreferences);
        
        super.onCreate(savedInstanceState);
        
        // Ensure status bar is properly handled
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        
        setContentView(R.layout.activity_settings);
        
        // Set windowInsets listener
        View settingsLayout = findViewById(R.id.settings_main);
        ViewCompat.setOnApplyWindowInsetsListener(settingsLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            settingsLayout.setPadding(
                settingsLayout.getPaddingLeft(),
                statusBarHeight + 16, // Add padding to original padding
                settingsLayout.getPaddingRight(),
                settingsLayout.getPaddingBottom()
            );
            return insets;
        });

        // Setup UI elements
        switchDarkMode = findViewById(R.id.switchDarkMode);
        
        // Set the switch state based on saved preference
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(isDarkMode);
        
        // Add listener for switch changes
        switchDarkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Only proceed if the theme actually changed
                if (isChecked != initialThemeState) {
                    // Save the preference
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(KEY_DARK_MODE, isChecked);
                    editor.apply();
                    
                    // Apply the theme change
                    if (isChecked) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                    
                    // Recreate the activity for theme to take effect properly
                    recreate();
                    
                    // Also inform MainActivity to recreate itself when we return to it
                    setResult(RESULT_OK);
                }
            }
        });
    }
    
    // Static method to apply theme from preferences (used on app startup)
    public static void applyTheme(SharedPreferences prefs) {
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
} 