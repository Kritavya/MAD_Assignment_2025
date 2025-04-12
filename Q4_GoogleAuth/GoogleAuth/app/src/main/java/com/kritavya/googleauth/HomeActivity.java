package com.kritavya.googleauth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private TextView mUserDetails;
    private Button mSignOutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Initialize views
        mUserDetails = findViewById(R.id.user_details);
        mSignOutButton = findViewById(R.id.sign_out_button);
        
        // Set up sign out button click listener
        mSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUI(mAuth.getCurrentUser());
    }
    
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            String displayText = "Name: " + (user.getDisplayName() != null ? user.getDisplayName() : "N/A") + 
                                "\nEmail: " + user.getEmail();
            mUserDetails.setText(displayText);
        } else {
            // User is not signed in, return to login screen
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
    
    private void signOut() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Revoke access - this will force the account chooser to appear on next sign-in
        mGoogleSignInClient.revokeAccess()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Access revoked successfully");
                        } else {
                            Log.w(TAG, "Failed to revoke access", task.getException());
                        }
                        // Return to login screen regardless of revokeAccess result
                        startActivity(new Intent(HomeActivity.this, MainActivity.class));
                        finish();
                    }
                });
    }
} 