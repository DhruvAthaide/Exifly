package com.dhruvathaide.exifly;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.airbnb.lottie.LottieAnimationView;
import com.dhruvathaide.exifly.core.MetadataManager;
import com.dhruvathaide.exifly.databinding.ActivityMainBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ImageAdapter adapter;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK &&
                                result.getData() != null) {

                            Intent data = result.getData();
                            if (data.getClipData() != null) {
                                ClipData clipData = data.getClipData();
                                for (int i = 0; i < clipData.getItemCount(); i++) {
                                    adapter.addImage(clipData.getItemAt(i).getUri());
                                }
                            } else if (data.getData() != null) {
                                adapter.addImage(data.getData());
                            }
                            animateImageAdded();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        adapter = new ImageAdapter();
        binding.recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );
        binding.recyclerView.setAdapter(adapter);

        binding.selectArea.setOnClickListener(v -> openImagePicker());
        binding.cleanButton.setOnClickListener(v -> cleanAllImages());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePicker.launch(intent);
    }

    private void cleanAllImages() {
        binding.lottie.setVisibility(LottieAnimationView.VISIBLE);
        binding.lottie.playAnimation();

        List<ImageModel> items = adapter.getItems();

        for (int i = 0; i < items.size(); i++) {
            int index = i;
            ImageModel item = items.get(i);
            
            if (item.getStatus() == ImageModel.STATUS_CLEANED) continue;

            executor.execute(() -> {
                try {
                    MetadataManager.stripExif(
                            this,
                            item.getUri(),
                            "clean_" + System.currentTimeMillis() + "_" + index + ".jpg"
                    );

                    runOnUiThread(() ->
                            adapter.updateStatus(index, ImageModel.STATUS_CLEANED)
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            adapter.updateStatus(index, ImageModel.STATUS_FAILED)
                    );
                }
            });
        }

        binding.lottie.postDelayed(() -> {
            binding.lottie.cancelAnimation();
            binding.lottie.setVisibility(LottieAnimationView.GONE);
        }, 2000);
    }

    private void animateImageAdded() {
        binding.recyclerView.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(150)
                .withEndAction(() ->
                        binding.recyclerView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .start()
                ).start();
    }
}
