package com.kritavya.photogallery;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;

public class ImageDetailsActivity extends AppCompatActivity {

    private ImageView imageViewDetail;
    private TextView textViewImageName, textViewFilePath, textViewSize, textViewDate;
    private Button buttonDelete;
    private ImageItem imageItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        // Initialize UI components
        imageViewDetail = findViewById(R.id.imageViewDetail);
        textViewImageName = findViewById(R.id.textViewImageName);
        textViewFilePath = findViewById(R.id.textViewFilePath);
        textViewSize = findViewById(R.id.textViewSize);
        textViewDate = findViewById(R.id.textViewDate);
        buttonDelete = findViewById(R.id.buttonDelete);

        // Get image path from intent
        String imagePath = getIntent().getStringExtra("IMAGE_PATH");
        if (imagePath != null) {
            loadImageDetails(imagePath);
        } else {
            Toast.makeText(this, "Error: Image not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set up delete button click listener
        buttonDelete.setOnClickListener(v -> confirmDeleteImage());
    }

    private void loadImageDetails(String imagePath) {
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            imageItem = new ImageItem(imageFile);

            // Load image with Glide
            Glide.with(this)
                    .load(imageFile)
                    .into(imageViewDetail);

            // Set text views with image details
            textViewImageName.setText("Name: " + imageItem.getName());
            textViewFilePath.setText("Path: " + imageItem.getPath());
            textViewSize.setText("Size: " + imageItem.getFormattedSize());
            textViewDate.setText("Date: " + imageItem.getFormattedDate());
        } else {
            Toast.makeText(this, "Error: Image file does not exist", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void confirmDeleteImage() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Delete", (dialog, which) -> deleteImage())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImage() {
        if (imageItem != null) {
            File file = imageItem.getFile();
            if (file.delete()) {
                Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 