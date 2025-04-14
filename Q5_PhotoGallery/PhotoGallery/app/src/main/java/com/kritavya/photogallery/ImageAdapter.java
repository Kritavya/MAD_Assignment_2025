package com.kritavya.photogallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<ImageItem> images;
    private List<ImageItem> selectedItems;
    private boolean isMultiSelectMode;
    private OnImageClickListener clickListener;
    private OnImageLongClickListener longClickListener;

    public interface OnImageClickListener {
        void onImageClick(int position);
    }

    public interface OnImageLongClickListener {
        void onImageLongClick(int position);
    }

    public ImageAdapter(MainActivity context, OnImageClickListener clickListener, OnImageLongClickListener longClickListener) {
        this.images = new ArrayList<>();
        this.selectedItems = new ArrayList<>();
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageItem item = images.get(position);
        
        // Load image using Glide
        Glide.with(holder.itemView.getContext())
                .load(item.getUri())
                .centerCrop()
                .into(holder.imageView);

        // Handle checkbox visibility and state
        holder.checkBox.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedItems.contains(item));

        // Set click listeners
        holder.itemView.setOnClickListener(v -> clickListener.onImageClick(position));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onImageLongClick(position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void setImages(List<ImageItem> images) {
        this.images = new ArrayList<>(images);
        notifyDataSetChanged();
    }

    public List<ImageItem> getImages() {
        return images;
    }

    public void setMultiSelectMode(boolean isMultiSelectMode) {
        this.isMultiSelectMode = isMultiSelectMode;
        notifyDataSetChanged();
    }

    public void setSelectedItems(List<ImageItem> selectedItems) {
        this.selectedItems = selectedItems;
        notifyDataSetChanged();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        CheckBox checkBox;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
} 