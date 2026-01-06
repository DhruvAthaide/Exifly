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
import com.dhruvathaide.exifly.core.MetadataInfo;
import com.dhruvathaide.exifly.databinding.ActivityMainBinding;

import java.util.List;

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
                                    analyzeMetadata(adapter.getItems().size() - 1);
                                }
                            } else if (data.getData() != null) {
                                adapter.addImage(data.getData());
                                analyzeMetadata(adapter.getItems().size() - 1);
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
                new androidx.recyclerview.widget.GridLayoutManager(this, 3)
        );
        binding.recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(this::showMetadataDialog);
        adapter.setOnItemShareClickListener(item -> {
            if (item.getCleanedUri() != null) {
                shareImage(item.getCleanedUri());
            }
        });
        
        binding.selectArea.setOnClickListener(v -> openImagePicker());
        binding.cleanButton.setOnClickListener(v -> cleanAllImages());
        binding.shareAllFab.setOnClickListener(v -> shareAllCleanedImages());

        // Mock Stats for "Dashboard" feel
        binding.statImagesCleaned.setText("128");
        binding.statStorageSaved.setText("45 MB");
        
        handleIncomingIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    adapter.addImage(imageUri);
                    analyzeMetadata(adapter.getItems().size() - 1);
                    animateImageAdded();
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                java.util.ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (imageUris != null) {
                    for (Uri uri : imageUris) {
                        adapter.addImage(uri);
                        analyzeMetadata(adapter.getItems().size() - 1);
                    }
                    animateImageAdded();
                }
            }
        }
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
        boolean randomize = binding.switchRandomize.isChecked();
        boolean removeDate = binding.switchRemoveDate.isChecked();

        for (int i = 0; i < items.size(); i++) {
            int index = i;
            ImageModel item = items.get(i);
            
            if (item.getStatus() == ImageModel.STATUS_CLEANED) continue;

            executor.execute(() -> {
                try {
                    String filename;
                    if (randomize) {
                         filename = java.util.UUID.randomUUID().toString().substring(0, 8) + ".jpg";
                    } else if (removeDate) {
                         filename = "clean_img_" + index + ".jpg";
                    } else {
                         filename = "clean_" + System.currentTimeMillis() + "_" + index + ".jpg";
                    }

                    Uri resultUri = MetadataManager.stripExif(
                            this,
                            item.getUri(),
                            filename
                    );

                    runOnUiThread(() -> {
                        item.setCleanedUri(resultUri);
                        adapter.updateStatus(index, ImageModel.STATUS_CLEANED);
                        updateShareFabVisibility();
                    });

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

    private void updateShareFabVisibility() {
        boolean hasCleaned = false;
        for (ImageModel item : adapter.getItems()) {
            if (item.getStatus() == ImageModel.STATUS_CLEANED) {
                hasCleaned = true;
                break;
            }
        }
        if (hasCleaned) {
             binding.shareAllFab.setVisibility(android.view.View.VISIBLE);
        } else {
             binding.shareAllFab.setVisibility(android.view.View.GONE);
        }
    }

    private void showMetadataDialog(ImageModel item) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        dialog.setContentView(R.layout.sheet_metadata);
        
        android.widget.ImageView thumb = dialog.findViewById(R.id.sheetThumb);
        android.widget.TextView model = dialog.findViewById(R.id.sheetModel);
        android.widget.TextView date = dialog.findViewById(R.id.sheetDate);
        android.widget.TextView gps = dialog.findViewById(R.id.sheetGps);
        android.widget.TextView raw = dialog.findViewById(R.id.sheetRaw); // New
        android.widget.Button close = dialog.findViewById(R.id.btnClose);
        android.widget.Button share = dialog.findViewById(R.id.btnShare);
        com.google.android.material.button.MaterialButton openMap = dialog.findViewById(R.id.btnOpenMap);

        if (thumb != null) thumb.setImageURI(item.getUri());
        
        MetadataInfo meta = item.getMetadata();
        if (meta != null) {
            model.setText(meta.deviceModel != null ? meta.deviceModel : "Unknown Device");
            date.setText(meta.dateTime != null ? meta.dateTime : "No Date");
            gps.setText(meta.gpsCoordinates != null ? meta.gpsCoordinates : "No GPS Data");
            if (raw != null) raw.setText(meta.rawTags != null && !meta.rawTags.isEmpty() ? meta.rawTags : "No Raw Metadata Found");
            
            if (meta.gpsCoordinates != null) {
                openMap.setVisibility(android.view.View.VISIBLE);
                openMap.setOnClickListener(v -> {
                    // geo:lat,lon
                   Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + meta.gpsCoordinates));
                   try { startActivity(intent); } catch (Exception ignored) {}
                });
            } else {
                openMap.setVisibility(android.view.View.GONE);
            }
        }

        if (share != null) {
            if (item.getStatus() == ImageModel.STATUS_CLEANED && item.getCleanedUri() != null) {
                share.setText("Share Cleaned Image");
                share.setOnClickListener(v -> shareImage(item.getCleanedUri()));
            } else {
                share.setText("Share Original");
                share.setOnClickListener(v -> shareImage(item.getUri()));
            }
        }

        if (close != null) close.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void shareImage(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpeg");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Image"));
    }

    private void shareAllCleanedImages() {
        java.util.ArrayList<Uri> uris = new java.util.ArrayList<>();
        for (ImageModel item : adapter.getItems()) {
            if (item.getStatus() == ImageModel.STATUS_CLEANED && item.getCleanedUri() != null) {
                uris.add(item.getCleanedUri());
            }
        }
        
        if (uris.isEmpty()) {
            android.widget.Toast.makeText(this, "No cleaned images available to share.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("image/jpeg");
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share All Cleaned Images"));
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
    private void analyzeMetadata(int index) {
        if (index < 0 || index >= adapter.getItems().size()) return;
        
        ImageModel item = adapter.getItems().get(index);
        executor.execute(() -> {
            MetadataInfo info = MetadataManager.extractMetadata(this, item.getUri());
            
            runOnUiThread(() -> {
                item.setMetadata(info);
                adapter.notifyItemChanged(index);
            });
        });
    }
}
