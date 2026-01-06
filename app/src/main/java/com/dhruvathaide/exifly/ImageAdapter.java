package com.dhruvathaide.exifly;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dhruvathaide.exifly.databinding.ItemImageBinding;

import java.util.*;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private final List<Uri> images = new ArrayList<>();
    private final Set<Integer> cleanedIndexes = new HashSet<>();

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
        h.binding.imageThumb.setImageURI(images.get(position));
        h.binding.status.setText(
                cleanedIndexes.contains(position)
                        ? "Cleaned"
                        : "Pending"
        );
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void addImage(Uri uri) {
        images.add(uri);
        notifyItemInserted(images.size() - 1);
    }

    public List<Uri> getImages() {
        return images;
    }

    public void markCleaned(int index) {
        cleanedIndexes.add(index);
        notifyItemChanged(index);
    }
}
