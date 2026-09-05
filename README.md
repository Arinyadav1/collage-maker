# Collage Maker - On-Device Video Face Detection & Story Collage

An Android application built with Kotlin, Jetpack Compose, and MVI Architecture that processes portrait videos entirely on-device, detects faces, identifies the same person across continuous appearance segments, selects the strongest representative shot for each individual, and generates beautiful, shareable Instagram-Story-style collages.

---

## 🌟 Key Features

1. **100% On-Device Machine Learning Pipeline**:
   - **Face Detection**: Google ML Kit `FaceDetection` with `ACCURATE_MODE`, `ALL_CLASSIFICATIONS`, and `ALL_LANDMARKS` to detect face bounding boxes, head poses (Euler Y, Euler Z), eye-open probabilities, and smiling probabilities.
   - **On-Device Face Embeddings**: High-performance neural embedding model based on **MobileFaceNet (TensorFlow Lite)** generating 128-dimensional normalized face feature vectors.
   - **Continuous Appearance Segment Tracking**: Continuous frame tracking over time. An appearance starts when a face is clearly visible and ends when it disappears or whip-pan blur occurs (> 0.8s threshold).

2. **Appearance Clustering & Count**:
   - Groups continuous appearance segments across separate occurrences in the video into distinct **Person clusters** using Cosine Similarity Agglomerative Clustering ($> 0.62$ similarity threshold).
   - Accurately tracks appearance counts per person (e.g. Sample 1: 5 distinct people, 4 appearances each = 20 total appearances; with simultaneous appearances tracked separately).

3. **Representative Shot Selection**:
   Evaluates each candidate frame using a multi-factor Quality Score formula:
   $$\text{QualityScore} = 0.35 \cdot \text{Frontality} + 0.25 \cdot \text{EyesOpen} + 0.20 \cdot \text{Sharpness} + 0.20 \cdot \text{Smiling} - \text{ClippingPenalty}$$
   - **Frontality**: Prefers frontal head pose (Euler Y and Z close to 0°).
   - **Focus / Sharpness**: Evaluated via Laplacian gradient variance on the face region.
   - **Eye Openness**: Prefers open eyes, penalizing closed eyes/blinks.
   - **Expression**: Prefers pleasant, smiling expressions.
   - **Full Face / Generous Crop**: Avoids faces clipped by video frame edges.

4. **Instagram Story Style Collages**:
   - Generates 1080x1920 high-resolution collages using generous crops (head, hair, and shoulder context, avoiding low-res tight face crops).
   - Draws elegant dark gradient background, header summary, custom layout styles (*Story Grid*, *Magazine Modern*, *Minimal Cards*), and person pill badges ("Person 1 • 4 Appearances").

5. **Gallery Export & Android Share Sheet**:
   - Save collage directly to device Photo Gallery via Android `MediaStore`.
   - Share instantly through the standard Android Share Sheet using secure `FileProvider` (`content://` URI).

---

## 🏗️ Technical Architecture & MVI Pattern

The app strictly follows **MVI (Model-View-Intent)** architecture and clean package separation:

```
com.collageMaker/
├── data/
│   ├── ml/
│   │   ├── FaceDetectorEngine.kt        # ML Kit Face Detection
│   │   ├── FaceEmbeddingEngine.kt       # On-device TFLite MobileFaceNet Embedding Model
│   │   ├── AppearanceClusteringEngine.kt# Continuous segment tracking & Cosine Clustering
│   │   ├── RepresentativeShotSelector.kt# Multi-factor face quality scoring & generous crop
│   │   └── CollageGeneratorEngine.kt    # Story collage canvas renderer
│   ├── model/
│   │   └── DomainModels.kt              # Immutable state, results, and data models
│   └── repository/
│       ├── VideoFrameExtractor.kt      # Frame extraction using MediaMetadataRetriever
│       └── VideoRepository.kt           # Video processing coordinator & MediaStore exporter
├── features/
│   ├── BaseViewModel.kt                 # MVI ViewModel base class
│   ├── components/                      # Reusable UI components
│   │   ├── AppHeader.kt
│   │   ├── PrimaryButton.kt
│   │   ├── VideoSelectorSection.kt
│   │   ├── ProcessingProgressCard.kt
│   │   ├── PersonSummaryCard.kt
│   │   └── CollagePreviewSection.kt
│   ├── home/                            # Home screen MVI feature
│   │   ├── HomeState.kt                 # UI State
│   │   ├── HomeAction.kt                # UI Intent Actions
│   │   ├── HomeEvent.kt                 # One-shot Side Effects
│   │   ├── HomeViewModel.kt             # View Model
│   │   └── HomeScreen.kt                # Jetpack Compose UI
│   └── navigation/
│       └── AppRoot.kt                   # Type-safe Compose Navigation
└── ui/theme/                            # Design system tokens (Color, Type, Theme)
```

---

## 🛠️ On-Device Machine Learning Documentation

### Face Embedding Model (`MobileFaceNet`)
- **Input**: 112x112 RGB normalized tensor ($[-1.0, 1.0]$).
- **Output**: 128-dimensional L2-normalized float embedding vector.
- **Inference Engine**: TensorFlow Lite Android Runtime (`org.tensorflow:tensorflow-lite:2.16.1`).
- **Clustering Algorithm**: Cosine Similarity Agglomerative Hierarchical Clustering:
  $$\text{CosineSimilarity}(\vec{u}, \vec{v}) = \frac{\vec{u} \cdot \vec{v}}{\|\vec{u}\|_2 \|\vec{v}\|_2}$$

---

## 🚀 Building & Running

1. **Prerequisites**: Android Studio Ladybug/Ladybird or AGP 8.11+, JDK 17, Android SDK 36.
2. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
3. **Run on Device / Emulator**:
   ```bash
   android run
   ```
