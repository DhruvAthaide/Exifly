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
        
        switch (item.getStatus()) {
            case ImageModel.STATUS_CLEANED:
                h.binding.status.setText("Cleaned");
                break;
            case ImageModel.STATUS_FAILED:
                h.binding.status.setText("Failed");
                break;
            default:
                h.binding.status.setText("Pending");
        }
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
