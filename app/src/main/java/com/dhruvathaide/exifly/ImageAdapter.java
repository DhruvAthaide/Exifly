package com.dhruvathaide.exifly;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dhruvathaide.exifly.databinding.ItemImageBinding;

import java.util.*;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private final List<ImageModel> items = new ArrayList<>();

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemImageBinding binding;
        ViewHolder(ItemImageBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(ImageModel item);
    }

    public interface OnItemShareClickListener {
        void onItemShareClick(ImageModel item);
    }
    
    private OnItemClickListener listener;
    private OnItemShareClickListener shareListener;
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemShareClickListener(OnItemShareClickListener listener) {
        this.shareListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        return new ViewHolder(
                ItemImageBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ImageModel item = items.get(position);
        h.binding.imageThumb.setImageURI(item.getUri());
        
        // Share Button Visibility
        if (item.getStatus() == ImageModel.STATUS_CLEANED) {
            h.binding.btnItemShare.setVisibility(android.view.View.VISIBLE);
            h.binding.btnItemShare.setOnClickListener(v -> {
                if (shareListener != null) shareListener.onItemShareClick(item);
            });
        } else {
            h.binding.btnItemShare.setVisibility(android.view.View.GONE);
        }
        
        switch (item.getStatus()) {
            case ImageModel.STATUS_CLEANED:
                h.binding.status.setText("Cleaned");
                h.binding.status.setTextColor(0xFF4CAF50); // Green
                break;
            case ImageModel.STATUS_FAILED:
                h.binding.status.setText("Failed");
                h.binding.status.setTextColor(0xFFF44336); // Red
                break;
            default:
                h.binding.status.setText("Pending");
                h.binding.status.setTextColor(0xFFFFFFFF); // White
        }

        // Bind Metadata
        com.dhruvathaide.exifly.core.MetadataInfo meta = item.getMetadata();
        if (meta == null) {
            h.binding.metadataBadges.setText("Analyzing...");
        } else if (!meta.hasRisk()) {
            h.binding.metadataBadges.setText("✅ Safe (No Metadata)");
        } else {
            StringBuilder sb = new StringBuilder();
            boolean hasPrev = false;

            if (meta.gpsCoordinates != null) {
                sb.append("📍 ").append(meta.gpsCoordinates);
                hasPrev = true;
            }
            if (meta.deviceModel != null) {
                if (hasPrev) sb.append("\n"); // New line
                sb.append("📷 ").append(meta.deviceModel.trim());
                hasPrev = true;
            }
            if (meta.dateTime != null) {
                if (hasPrev) sb.append("\n"); // New line
                // Fix generic EXIF date format "2023:10:05 12:00:00" -> "2023-10-05 12:00"
                String cleanDate = meta.dateTime.replaceFirst("^(\\d{4}):(\\d{2}):(\\d{2})", "$1-$2-$3");
                sb.append("📅 ").append(cleanDate);
            }
            
            h.binding.metadataBadges.setText(sb.toString());
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addImage(Uri uri) {
        items.add(new ImageModel(uri));
        notifyItemInserted(items.size() - 1);
    }

    public List<ImageModel> getItems() {
        return items;
    }

    public void updateStatus(int position, int status) {
        if (position >= 0 && position < items.size()) {
            items.get(position).setStatus(status);
            notifyItemChanged(position);
        }
    }
}
