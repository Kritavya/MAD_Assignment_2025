package com.kritavya.unitconverter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private EditText editTextValue;
    private Spinner spinnerFrom, spinnerTo;
    private Button buttonConvert;
    private TextView textViewResult;
    private ImageView imageViewSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        SharedPreferences preferences = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        SettingsActivity.applyTheme(preferences);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editTextValue = findViewById(R.id.editTextValue);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        buttonConvert = findViewById(R.id.buttonConvert);
        textViewResult = findViewById(R.id.textViewResult);
        imageViewSettings = findViewById(R.id.imageViewSettings);

        // Set up spinners with unit types
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.unit_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        // Set convert button click listener
        buttonConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertUnits();
            }
        });
        
        // Set settings icon click listener
        imageViewSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        
        // Set the appropriate icon based on the current theme
        updateSettingsIcon();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Update the settings icon when activity resumes
        updateSettingsIcon();
    }
    
    /**
     * Updates the settings icon based on the current theme
     */
    private void updateSettingsIcon() {
        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Dark theme is active
            imageViewSettings.setImageResource(R.drawable.settings_dark);
        } else {
            // Light theme is active
            imageViewSettings.setImageResource(R.drawable.settings_light);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void convertUnits() {
        // Get input value
        String inputValueStr = editTextValue.getText().toString().trim();
        
        // Validate input
        if (inputValueStr.isEmpty()) {
            Toast.makeText(this, "Please enter a value", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            double inputValue = Double.parseDouble(inputValueStr);
            String fromUnit = spinnerFrom.getSelectedItem().toString();
            String toUnit = spinnerTo.getSelectedItem().toString();
            
            // Convert to meters first (base unit)
            double valueInMeters = convertToMeters(inputValue, fromUnit);
            
            // Convert from meters to target unit
            double result = convertFromMeters(valueInMeters, toUnit);
            
            // Format and display result
            String formattedResult = String.format("%.4f %s = %.4f %s", 
                    inputValue, fromUnit, result, toUnit);
            textViewResult.setText(formattedResult);
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }
    
    // Convert any unit to meters
    private double convertToMeters(double value, String fromUnit) {
        switch (fromUnit) {
            case "Feet":
                return value * 0.3048;
            case "Inches":
                return value * 0.0254;
            case "Centimeters":
                return value * 0.01;
            case "Meters":
                return value;
            case "Yards":
                return value * 0.9144;
            default:
                return 0;
        }
    }
    
    // Convert from meters to any unit
    private double convertFromMeters(double meters, String toUnit) {
        switch (toUnit) {
            case "Feet":
                return meters / 0.3048;
            case "Inches":
                return meters / 0.0254;
            case "Centimeters":
                return meters / 0.01;
            case "Meters":
                return meters;
            case "Yards":
                return meters / 0.9144;
            default:
                return 0;
        }
    }
}