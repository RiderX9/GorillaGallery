# 🦍 Gorilla Gallery

A premium offline Android gallery app with a beautiful liquid glass design system.

## 📱 Screenshots

| Home | Albums | Settings | Search | Editor |
|:---:|:---:|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/01e1d763-45e6-4814-9654-29cf280d68a8" width="200" alt="Home Tab" /> | <img src="https://github.com/user-attachments/assets/dbd8d355-fa0a-4ba0-8a92-021f0753eaa2" width="200" alt="Albums Tab" /> | <img src="https://github.com/user-attachments/assets/8038e583-c52f-47db-a793-4412d01cd9e7" width="200" alt="Settings Tab" /> | <img src="https://github.com/user-attachments/assets/884836ab-63bd-4752-baa6-1239b59c0a97" width="200" alt="Search Tab" /> | <img src="https://github.com/user-attachments/assets/57318ef1-c0b9-40fd-9bed-a114c652c3d1" width="200" alt="Editor Tab" /> |

## ✨ Features

* **Smart Organization:** Photo and video library grouped by Day, Month, and Year.
* **Intelligent Search:** Fully offline, on-device machine learning for face detection, image labeling, object detection, and text recognition (OCR).
* **Immersive Viewing:** Full-screen viewer with pinch-to-zoom support.
* **Built-in Photo Editor:** Powerful editor with crop, rotate, brightness, contrast, filters, and doodle capabilities.
* **Video Editor & Playback:** Hardware-accelerated 4K HDR10 playback (ExoPlayer + MediaCodec) with seek bar and mute control, plus a built-in video editor for trimming.
* **Zero-Allocation Thumbnail Engine:** A custom NIO-based disk cache that effortlessly handles massive high-quality photo grids without GC stutter or lag.
* **Premium Aesthetics:** Stunning liquid glass UI design system featuring adaptive accent colors and backdrop blur.
* **Smart Albums:** Auto-detection of albums directly from the MediaStore.
* **Secure & Private:** Dedicated Secure Folder protected by biometric unlock.
* **Safe Deletion:** Trash with 30-day auto-empty alongside a Favorites feature.

## 🔒 Privacy Guarantee (100% On-Device)

We believe your photos are your private property.
* **Local Machine Learning:** All AI processing—including Facial Clustering (`mobile_face_net.tflite`), Object Detection, and Text Recognition—runs **100% locally on your device**. No photos, faces, or personal data are ever uploaded to a server or the cloud.
* **Why the Internet Permission?** The `INTERNET` permission in the manifest is used strictly by Google ML Kit to download its base models on first launch. 
* **Biometrics:** The `USE_BIOMETRIC` permission is used exclusively to lock and unlock the Secure Folder locally.

## 🛠 Tech Stack

* **UI:** Kotlin + Jetpack Compose, Material Kolor (adaptive colors), Kyant Backdrop
* **Media:** Coil (image loading), Media3/ExoPlayer (video playback), Media3 Transformer (video editing)
* **Machine Learning:** Google ML Kit (Face Detection, Text Recognition, Image Labeling, Object Detection), TensorFlow Lite (MobileFaceNet)
* **Storage:** MediaStore API, Room Database, DataStore (preferences), NIO MappedByteBuffer (Thumbnail cache)
* **Security:** BiometricPrompt API
* **Graphics:** Android 12+ RenderEffect blur

## 📱 Requirements

* Android 12 or higher (Minimum SDK 31)

## 📥 Installation

1. Go to the **Releases** tab.
2. Download the latest `.apk` file.
3. Enable "Install from unknown sources" in your device settings.
4. Install and enjoy!

## 👨‍💻 Developer

Made by **[RiderX9](https://github.com/RiderX9)**
