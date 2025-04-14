package com.kritavya.photogallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private RecyclerView recyclerViewImages;
    private TextView textViewEmpty;
    private FloatingActionButton fabCamera;
    private ImageAdapter imageAdapter;
    private File currentPhotoFile;
    private List<ImageItem> imageItems;

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            result -> {
                if (result) {
                    loadImages();
                } else {
                    // Delete the empty file if user canceled
                    if (currentPhotoFile != null && currentPhotoFile.exists()) {
                        currentPhotoFile.delete();
                    }
                    Toast.makeText(this, "Photo capture canceled", Toast.LENGTH_SHORT).show();
                }
            }
    );
    
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        // Copy the selected image to our app's directory
                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                        String imageFileName = "PICKED_" + timeStamp + ".jpg";
                        File destFile = new File(storageDir, imageFileName);
                        
                        // Copy content from URI to our file
                        copyImageToFile(uri, destFile);
                        
                        loadImages();
                    } catch (IOException e) {
                        Toast.makeText(this, "Error saving selected image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) {
                    allGranted = allGranted && granted;
                }
                if (allGranted) {
                    showImageSourceDialog();
                } else {
                    Toast.makeText(this, "Permissions are required to use the camera and access photos", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        fabCamera = findViewById(R.id.fabCamera);

        // Setup RecyclerView
        recyclerViewImages.setLayoutManager(new GridLayoutManager(this, 3));
        imageItems = new ArrayList<>();
        imageAdapter = new ImageAdapter(this, imageItems);
        recyclerViewImages.setAdapter(imageAdapter);

        // Setup FAB click listener
        fabCamera.setOnClickListener(v -> checkPermissionsAndShowDialog());

        // Load images on startup
        loadImages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImages();
    }
    
    private void checkPermissionsAndShowDialog() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Storage permissions based on API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (permissionsNeeded.isEmpty()) {
            showImageSourceDialog();
        } else {
            requestPermissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
        }
    }
    
    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Select from Gallery"};
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }
    
    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }
    
    private void copyImageToFile(Uri sourceUri, File destFile) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(destFile)) {
            
            if (in == null) {
                throw new IOException("Could not open input stream");
            }
            
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    private void openCamera() {
        try {
            currentPhotoFile = createImageFile();
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.kritavya.photogallery.fileprovider",
                    currentPhotoFile);
            takePictureLauncher.launch(photoURI);
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    private void loadImages() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && storageDir.exists()) {
            File[] files = storageDir.listFiles((dir, name) -> 
                    name.toLowerCase().endsWith(".jpg") || 
                    name.toLowerCase().endsWith(".jpeg") || 
                    name.toLowerCase().endsWith(".png"));

            if (files != null && files.length > 0) {
                imageItems.clear();
                
                // Sort files by last modified date (newest first)
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                
                for (File file : files) {
                    imageItems.add(new ImageItem(file));
                }
                
                textViewEmpty.setVisibility(View.GONE);
                recyclerViewImages.setVisibility(View.VISIBLE);
            } else {
                imageItems.clear();
                textViewEmpty.setVisibility(View.VISIBLE);
                recyclerViewImages.setVisibility(View.GONE);
            }
            
            imageAdapter.updateData(imageItems);
        }
    }
}