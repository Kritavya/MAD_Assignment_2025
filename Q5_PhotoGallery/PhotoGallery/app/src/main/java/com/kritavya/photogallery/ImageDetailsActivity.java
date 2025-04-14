package com.kritavya.photogallery;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.bumptech.glide.Glide;

import java.io.File;

public class ImageDetailsActivity extends AppCompatActivity {

    private ImageView imageViewDetail;
    private TextView textViewImageName, textViewFilePath, textViewSize, textViewDate;
    private Button buttonDelete;
    private ImageItem imageItem;
    private boolean isExternalUri;
    private Uri imageUri;

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

        // Get data from intent
        isExternalUri = getIntent().getBooleanExtra("IS_EXTERNAL_URI", false);
        if (isExternalUri) {
            String uriString = getIntent().getStringExtra("IMAGE_URI");
            if (uriString != null) {
                imageUri = Uri.parse(uriString);
                loadExternalImageDetails(imageUri);
            } else {
                Toast.makeText(this, "Error: Image URI not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            // Handle file-based image
            String imagePath = getIntent().getStringExtra("IMAGE_PATH");
            if (imagePath != null) {
                loadImageDetails(imagePath);
            } else {
                Toast.makeText(this, "Error: Image not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        // Set up delete button click listener
        buttonDelete.setOnClickListener(v -> confirmDeleteImage());
    }

    private void loadExternalImageDetails(Uri uri) {
        try {
            DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
            if (documentFile != null && documentFile.exists()) {
                String name = documentFile.getName();
                long size = documentFile.length();
                long lastModified = documentFile.lastModified();

                // Create ImageItem with URI
                imageItem = new ImageItem(uri, name, size, lastModified, this);

                // Load image with Glide
                Glide.with(this)
                        .load(uri)
                        .into(imageViewDetail);

                // Set text views with image details
                textViewImageName.setText("Name: " + imageItem.getName());
                textViewFilePath.setText("Path: " + imageItem.getPath());
                textViewSize.setText("Size: " + imageItem.getFormattedSize());
                textViewDate.setText("Date: " + imageItem.getFormattedDate());
            } else {
                Toast.makeText(this, "Error: External image not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading external image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
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
            boolean success = imageItem.delete();
            if (success) {
                Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
            }
        }
    }
} 