package com.kritavya.photogallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
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

public class MainActivity extends AppCompatActivity implements ImageAdapter.OnImageClickListener, ImageAdapter.OnImageLongClickListener {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int IMAGE_DETAILS_REQUEST_CODE = 1001;
    private RecyclerView recyclerViewImages;
    private TextView textViewEmpty;
    private FloatingActionButton fabCamera;
    private FloatingActionButton fabDelete;
    private ImageAdapter imageAdapter;
    private File currentPhotoFile;
    private List<ImageItem> imageItems;
    private boolean displayingExternalFolder = false;
    private Uri currentFolderUri = null;
    private boolean isMultiSelectMode = false;
    private List<ImageItem> selectedItems = new ArrayList<>();

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
                        
                        displayingExternalFolder = false;
                        loadImages();
                    } catch (IOException e) {
                        Toast.makeText(this, "Error saving selected image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
    
    private final ActivityResultLauncher<Intent> pickFolderLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri folderUri = result.getData().getData();
                    if (folderUri != null) {
                        // Take persistence permission to access the folder across reboots
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(folderUri, takeFlags);
                        
                        currentFolderUri = folderUri;
                        displayingExternalFolder = true;
                        loadImagesFromFolder(folderUri);
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
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        recyclerViewImages = findViewById(R.id.recyclerView);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        fabCamera = findViewById(R.id.fabAdd);
        fabDelete = findViewById(R.id.fabDelete);

        // Setup RecyclerView
        recyclerViewImages.setLayoutManager(new GridLayoutManager(this, 3));
        imageItems = new ArrayList<>();
        imageAdapter = new ImageAdapter(this, this, this);
        recyclerViewImages.setAdapter(imageAdapter);

        // Setup FAB click listeners
        fabCamera.setOnClickListener(v -> checkPermissionsAndShowDialog());
        fabDelete.setOnClickListener(v -> deleteSelectedItems());

        // Load images on startup
        loadImages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (displayingExternalFolder && currentFolderUri != null) {
            loadImagesFromFolder(currentFolderUri);
        } else {
            loadImages();
        }
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
        String[] options = {"Take Photo", "Select from Gallery", "Select Folder"};
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else if (which == 1) {
                        openGallery();
                    } else {
                        openFolderPicker();
                    }
                })
                .show();
    }
    
    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickFolderLauncher.launch(intent);
    }
    
    private void loadImagesFromFolder(Uri folderUri) {
        List<ImageItem> folderImages = new ArrayList<>();
        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, folderUri);
        
        if (pickedDir != null && pickedDir.exists()) {
            DocumentFile[] files = pickedDir.listFiles();
            
            // First, clear any existing images
            imageItems.clear();
            
            // Then load existing images from storage
            loadImagesFromStorage(false);
            
            // Get the list of existing file paths for checking duplicates
            List<String> existingFilePaths = new ArrayList<>();
            for (ImageItem item : imageItems) {
                existingFilePaths.add(item.getPath());
            }
            
            // Process folder files
            for (DocumentFile file : files) {
                if (file.isFile() && isImageFile(file.getName())) {
                    try {
                        String fileName = file.getName();
                        // Check if this file already exists in our storage
                        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        String newFileName = "FOLDER_" + fileName;
                        File destFile = new File(storageDir, newFileName);
                        
                        // Only copy if the file doesn't exist in our list
                        if (!existingFilePaths.contains(destFile.getAbsolutePath()) && !destFile.exists()) {
                            copyImageToFile(file.getUri(), destFile);
                            ImageItem item = new ImageItem(destFile);
                            folderImages.add(item);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error processing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            
            if (!folderImages.isEmpty()) {
                // Add new folder images at the beginning
                imageItems.addAll(0, folderImages);
                textViewEmpty.setVisibility(View.GONE);
                recyclerViewImages.setVisibility(View.VISIBLE);
            } else {
                if (imageItems.isEmpty()) {
                    textViewEmpty.setText("No images found in selected folder");
                    textViewEmpty.setVisibility(View.VISIBLE);
                    recyclerViewImages.setVisibility(View.GONE);
                }
            }
            
            imageAdapter.setImages(new ArrayList<>(imageItems));
        }
    }
    
    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".jpg") || 
               lowerCaseName.endsWith(".jpeg") || 
               lowerCaseName.endsWith(".png");
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
            displayingExternalFolder = false;
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
        imageItems.clear();
        loadImagesFromStorage(true);
        imageAdapter.setImages(new ArrayList<>(imageItems));
    }
    
    private void loadImagesFromStorage(boolean updateVisibility) {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && storageDir.exists()) {
            File[] files = storageDir.listFiles((dir, name) -> 
                    name.toLowerCase().endsWith(".jpg") || 
                    name.toLowerCase().endsWith(".jpeg") || 
                    name.toLowerCase().endsWith(".png"));

            if (files != null && files.length > 0) {                
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                
                for (File file : files) {
                    boolean exists = false;
                    for (ImageItem item : imageItems) {
                        if (item.getPath().equals(file.getAbsolutePath())) {
                            exists = true;
                            break;
                        }
                    }
                    
                    if (!exists) {
                        imageItems.add(new ImageItem(file));
                    }
                }
                
                if (updateVisibility) {
                    textViewEmpty.setVisibility(View.GONE);
                    recyclerViewImages.setVisibility(View.VISIBLE);
                }
            } else if (!displayingExternalFolder && updateVisibility) {
                imageItems.clear();
                textViewEmpty.setText("No photos found. Tap the + button to add photos.");
                textViewEmpty.setVisibility(View.VISIBLE);
                recyclerViewImages.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImages();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem deleteItem = menu.findItem(R.id.menu_delete);
        MenuItem selectAllItem = menu.findItem(R.id.menu_select_all);
        MenuItem cancelItem = menu.findItem(R.id.menu_cancel);

        deleteItem.setVisible(isMultiSelectMode);
        selectAllItem.setVisible(isMultiSelectMode);
        cancelItem.setVisible(isMultiSelectMode);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.menu_delete) {
            deleteSelectedItems();
            return true;
        } else if (id == R.id.menu_select_all) {
            selectAllItems();
            return true;
        } else if (id == R.id.menu_cancel) {
            exitMultiSelectMode();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void deleteSelectedItems() {
        if (selectedItems.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Selected")
                .setMessage("Are you sure you want to delete " + selectedItems.size() + " photos?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    List<ImageItem> itemsToRemove = new ArrayList<>(selectedItems);
                    for (ImageItem item : itemsToRemove) {
                        if (item.delete()) {
                            imageItems.remove(item);
                        }
                    }
                    // Create a new list to force refresh
                    imageAdapter.setImages(new ArrayList<>(imageItems));
                    exitMultiSelectMode();
                    if (imageItems.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerViewImages.setVisibility(View.GONE);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void selectAllItems() {
        selectedItems.clear();
        selectedItems.addAll(imageAdapter.getImages());
        imageAdapter.setSelectedItems(selectedItems);
        imageAdapter.notifyDataSetChanged();
    }

    private void exitMultiSelectMode() {
        isMultiSelectMode = false;
        selectedItems.clear();
        imageAdapter.setMultiSelectMode(false);
        imageAdapter.setSelectedItems(selectedItems);
        imageAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
        updateFabVisibility();
    }
    
    private void updateFabVisibility() {
        FloatingActionButton fabDelete = findViewById(R.id.fabDelete);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        
        if (isMultiSelectMode) {
            fabDelete.setVisibility(View.VISIBLE);
            fabAdd.setVisibility(View.GONE);
        } else {
            fabDelete.setVisibility(View.GONE);
            fabAdd.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onImageClick(int position) {
        if (isMultiSelectMode) {
            ImageItem item = imageAdapter.getImages().get(position);
            if (selectedItems.contains(item)) {
                selectedItems.remove(item);
            } else {
                selectedItems.add(item);
            }
            imageAdapter.setSelectedItems(selectedItems);
            imageAdapter.notifyItemChanged(position);
        } else {
            // Open image details
            ImageItem selectedItem = imageAdapter.getImages().get(position);
            Intent intent = new Intent(this, ImageDetailsActivity.class);
            intent.putExtra("IMAGE_PATH", selectedItem.getPath());
            intent.putExtra("IS_EXTERNAL_URI", selectedItem.isExternalUri());
            if (selectedItem.isExternalUri()) {
                intent.putExtra("IMAGE_URI", selectedItem.getUri().toString());
            }
            startActivityForResult(intent, IMAGE_DETAILS_REQUEST_CODE);
        }
    }

    @Override
    public void onImageLongClick(int position) {
        if (!isMultiSelectMode) {
            isMultiSelectMode = true;
            ImageItem item = imageAdapter.getImages().get(position);
            selectedItems.add(item);
            imageAdapter.setMultiSelectMode(true);
            imageAdapter.setSelectedItems(selectedItems);
            imageAdapter.notifyDataSetChanged();
            invalidateOptionsMenu();
            updateFabVisibility();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_DETAILS_REQUEST_CODE && resultCode == RESULT_OK) {
            // Image was deleted, reload images
            loadImages();
        }
    }
}