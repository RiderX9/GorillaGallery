# GorillaGallery 🦍

GorillaGallery is a beautiful, high-performance Android gallery app built with modern UI aesthetics in mind. It features a stunning liquid glassmorphism design, a lightning-fast NIO-based thumbnail engine, and robust hardware-accelerated 4K HDR10 video playback.

## ✨ Features
- **Liquid Glassmorphism UI:** Gorgeous, dynamic blur effects that seamlessly respond to the content playing behind them.
- **Zero-Allocation Thumbnail Engine:** A custom-built thumbnail loader using `MappedByteBuffer` and `RGBA_F16` that effortlessly handles massive high-quality photo grids without GC stutter or lag.
- **Hardware-Accelerated 4K HDR10:** Smooth, buttery 4K video scrubbing utilizing the device's native `MediaCodec` instead of slow software decoders.
- **Smart Albums & Search:** Organize and find your photos easily using on-device machine learning.

## 🔒 Privacy & Security (100% On-Device)
We believe your photos are your private property.
* **On-Device Machine Learning:** All AI processing—including Facial Recognition, Object Detection, and Text Recognition—runs **100% locally on your device**. No photos, faces, or personal data are ever uploaded to a server or the cloud.
* **Secure Folder:** The app features a Secure Folder locked behind your device's native Biometrics (fingerprint/face unlock). The `USE_BIOMETRIC` permission is used strictly for this feature.
* **Why the Internet Permission?** The `INTERNET` permission is solely required to download the initial Google ML Kit models on first launch.

## 🛠️ Tech Stack & Credits
This project was built on the shoulders of giants. We want to thank the open-source community:
- **ExoPlayer:** For powering our robust 4K HDR10 video playback.
- **MobileFaceNet:** We use the lightweight `mobile_face_net.tflite` model for fast, local facial clustering.
- **Google ML Kit:** For on-device object detection and text recognition.
- **Liquid Backdrop / Glassmorphism:** For the gorgeous frosted glass rendering techniques used throughout our bottom sheets and toolbars.

## 📄 License
This project is licensed under the MIT License - feel free to use, modify, and distribute it!
