# 📺 OpenTVBox

> **Revive your legacy Android TV Box without a PC or firmware flashing.**  
> Unlocks Google Play Store TV compatibility, fixes "App not compatible / YouTube unavailable" errors, and provides a 1-click curated TV App Hub for Tanix TX3 Mini, MXQ, X96, and Amlogic/Rockchip/Allwinner devices.

---

## ⚡ The Problem

Millions of Android TV boxes worldwide (such as the **Tanix TX3 Mini**, **MXQ Pro 4K**, **X96 Mini**, and similar devices running **Amlogic S905W / S905X / Rockchip RK3229** on Android 7.1.2 Nougat) are suddenly becoming obsolete:

1. **YouTube Stops Working**: Google bumped YouTube's `minSdkVersion` to 26 (Android 8.0+). The pre-installed TV YouTube app displays `This version is no longer supported` or `Device incompatible`.
2. **Google Play Store Blocks TV Apps**: Google Play marks apps as "Your device isn't compatible with this version" because legacy TV boxes ship with phone/tablet Android ROMs missing the official `android.software.leanback` and `android.hardware.type.television` permission manifests.
3. **Flashing Custom ROMs is Risky**: Flashing custom firmware (like SlimBOX or aidan's ROM) requires opening the box, shorting pins, finding matching WiFi chip drivers (Realtek vs Broadcom vs Ampak), and risks permanently bricking the device or losing remote control functionality.

---

## 🚀 The Solution: OpenTVBox

**OpenTVBox is an all-in-one APK that runs directly on your TV box. Zero PC, zero USB burning tools, zero terminal commands.**

```
+---------------------------------------------------------------------------------+
|                                 OpenTVBox                                       |
|               TX3 Mini & Legacy Android TV Compatibility Hub                    |
+---------------------------------------------------------------------------------+
|  [ SoC: Amlogic S905W (GXL) | Android 7.1.2 (Nougat) | Root: Pre-Installed ]    |
+---------------------------------------------------------------------------------+
|                                                                                 |
|  +-------------------------+  +-------------------------+  +-----------------+  |
|  |  1-CLICK PLAY UNLOCK    |  |    YOUTUBE 4K FIX       |  |  SYSTEM TOOLS   |  |
|  |                         |  |                         |  |                 |  |
|  | Injects Leanback XML    |  | Installs SmartTube 4K   |  | Soft Reboot TV  |  |
|  | Spoofs TV profile       |  | Ad-free + SponsorBlock  |  | Hardware Specs  |  |
|  | Wipes Play Store Cache  |  | Zero Google Play Req.   |  | Memory Clean    |  |
|  |                         |  |                         |  |                 |  |
|  | [ RUN UNLOCK ]          |  | [ INSTALL YOUTUBE ]     |  | [ REBOOT DEVICE]|  |
|  +-------------------------+  +-------------------------+  +-----------------+  |
|                                                                                 |
|  ================ Curated TV Apps (Direct Download & Install) ================  |
|  [ SmartTube 4K ]   [ VLC Media Player ]   [ Stremio TV ]   [ Kodi 21 Omega ]   |
|                                                                                 |
|  ============================ Live Operation Log =============================  |
|  [19:15:02] [SUCCESS] Mount /system Read-Write: System mounted as RW            |
|  [19:15:03] [SUCCESS] Inject Leanback Permissions: XML installed                |
|  [19:15:04] [SUCCESS] Spoof Android TV Profile: Characteristics set to 'tv'    |
|  [19:15:05] [SUCCESS] Flush Play Store Cache: Cleared device records            |
+---------------------------------------------------------------------------------+
```

---

## ✨ Key Features

- **1-Click Google Play Store Compatibility Unlock**:
  - Automatically mounts `/system` read-write using pre-installed TV box root (`/system/xbin/su`).
  - Injects `/system/etc/permissions/android.software.leanback.xml`.
  - Injects `android.hardware.type.television` and `android.software.live_tv` features.
  - Updates `ro.build.characteristics=tv` in `/system/build.prop` (with automatic `.bak` backup).
  - Clears `com.android.vending` and `com.google.android.gms` cache so Google Play re-evaluates the device as a certified Android TV.
- **1-Click YouTube 4K (SmartTube)**:
  - Installs **SmartTube**, the gold-standard open-source YouTube client designed specifically for TV remotes.
  - Zero Google Play Services or microG requirements.
  - 4K 60fps playback, HDR, SponsorBlock, background playback, and voice search support.
- **Curated TV App Store**:
  - One-click direct downloads for remote-friendly TV streaming apps:
    - **SmartTube** (YouTube 4K ad-free)
    - **VLC Media Player** (Plays all video codecs from USB flash drives & network SMB)
    - **Stremio TV** (Movies, series & addons with Android TV UI)
    - **Nova Video Player** (Ultra-lightweight player with automatic poster scraping)
    - **Kodi 21 Omega** (Complete home theater system)
- **Built for TV Remotes (10-Foot UI)**:
  - High-contrast visual focus indicators for seamless navigation with standard infrared (IR) TV remotes.
  - Full D-Pad direction and `OK`/`ENTER` key support.
- **Hardware & Chipset Diagnostic**:
  - Detects Amlogic (S905W, S905X, S912, S905X2/X3/X4), Rockchip (RK3229, RK3328, RK3399), and Allwinner SoCs.
  - Displays real-time RAM allocation, internal flash storage health, Android API level, and root permission status.

---

## 📥 How to Install (No PC Needed)

### Method 1: USB Flash Drive (Easiest)
1. Download `OpenTVBox.apk` from the [Releases](https://github.com/abeeropu/OpenTVBox/releases) section.
2. Copy `OpenTVBox.apk` to any standard USB flash drive (FAT32 or NTFS).
3. Plug the USB flash drive into your TV box (e.g. Tanix TX3 Mini).
4. Open the pre-installed **FileBrowser** or **AppInstaller** app on your TV box.
5. Click `OpenTVBox.apk` and select **Install**.

### Method 2: TV Web Browser
1. Open the built-in browser (Chrome, TV Bro, or Lightning) on your TV box.
2. Navigate to `https://github.com/abeeropu/OpenTVBox/releases`.
3. Download the latest `OpenTVBox.apk` and open it from the download notification or Downloads folder.

---

## 🛠️ How to Use

1. Launch **OpenTVBox** from your TV box home screen.
2. Select **"1-Click Play Store Unlock"** with your remote's `OK` button:
   - The app will mount `/system` as RW, install the TV Leanback permission manifest, and clear Play Store cache.
3. Select **"Soft Reboot Device"** (or unplug and plug back the power cable).
4. Open Google Play Store: TV apps and YouTube updates will now be visible and installable!
5. Or select **"Install SmartTube"** directly inside OpenTVBox for an immediate, high-performance YouTube 4K experience.

---

## ⚙️ Building from Source

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API 34)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/abeeropu/OpenTVBox.git
cd OpenTVBox

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📜 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
