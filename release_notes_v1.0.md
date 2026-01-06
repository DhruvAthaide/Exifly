# Exifly v1.0: Initial Release

**Fly light, fly safe. The modern privacy tool for your photos.**

Exifly is a powerful, offline-first Android application designed to strip sensitive EXIF metadata from your images before you share them. With a beautiful "Glassmorphism" UI and smart privacy features, it ensures your location, device details, and timestamps stay private.

## ✨ Key Features

### 🔍 Metadata Inspector & Deep Search
*   **Instant Analysis**: Tap any image to see hidden metadata (GPS, Model, Date).
*   **Pro Map Link**: One-tap "Open in Maps" for found GPS coordinates.
*   **Raw Data View**: Inspect the full list of EXIF tags for complete transparency.
*   **Robust Extraction**: Custom algorithms dig deep to find location data even when other apps fail.

### 🛡️ Smart Privacy
*   **Automatic Scrubbing**: Removes all EXIF tags (GPS, Camera, Software, etc.).
*   **Randomize Filenames**: Option to save files as random UUIDs (e.g., `a1b2...jpg`) to obfuscate capture sequence.
*   **Date Scrubbing**: Intelligently removes timestamp patterns from filenames.

### 🚀 Seamless Workflow
*   **Share to Clean**: Share images directly from your Gallery/WhatsApp to Exifly.
*   **Batch Processing**: Select multiple images and clean them in seconds.
*   **Smart Sharing**:
    *   Share individual cleaned images directly from the grid.
    *   "Share All" button to send your entire safe batch to another app.

### 🎨 Modern UI
*   **Glassmorphism Design**: Sleek, semi-transparent cards and blur effects.
*   **Dashboard Stats**: Real-time tracking of images cleaned and risk removed.
*   **Fluid Layout**: Responsive grid that adapts to your workflow.

## 🔒 Privacy Architecture
Exifly is built on a **"No Network Access"** promise.
*   **Offline Only**: The app does not request `INTERNET` permission.
*   **Local Processing**: All image processing happens on your device.
*   **Sandboxed**: Cleaned images are saved to a public `Pictures/CleanExif` directory, keeping your originals untouched.
