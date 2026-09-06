# 📱 Project Context: KeepMe (formerly Telegram Drive) Android App

> **Notice for any AI Agent in a new chat session:**
> This repository is dedicated to building **KeepMe** (Android) — a high-performance native Android application using **Kotlin + Jetpack Compose + TDLib** that transforms a user's Telegram account into a personal, unlimited cloud storage drive.
>
> 📖 **Full Implementation Specification**: Read [TELEGRAM_DRIVE_ANDROID_MASTER_PLAN.md](file:///D:/tdrive/TELEGRAM_DRIVE_ANDROID_MASTER_PLAN.md) for full phase-by-phase build instructions and prebuilt library registries.

---

## 🎯 Core Requirements & User Invariants

1. **Native Android Platform**: Built with **Kotlin 2.0+** and **Jetpack Compose + Material 3**.
2. **Telegram Engine**: Uses **TDLib** via [EasyTDLib](https://github.com/kcvabeysinghe/EasyTDLib) (pre-compiled `.so` native binaries, zero C++/NDK hassle).
3. **Authentication**: User provides their own `api_id` and `api_hash` (from my.telegram.org) + phone number OTP + 2FA password support.
4. **Storage Architecture**:
   - **Root Storage**: Telegram "Saved Messages" chat.
   - **Folders**: Telegram private Channels created and managed by the app.
5. **Waterfall Layout**: True Pinterest-style staggered masonry grid for all media/files using native `LazyVerticalStaggeredGrid` (preserves aspect ratios).
6. **Prebuilt Solutions Mandate**: ALWAYS use established prebuilt libraries/APIs. Do NOT write custom masonry grids, custom video engines, or custom PDF parsers.
7. **Document / PDF Handling**: Open PDFs and docs directly in the user's installed phone apps via `Intent.ACTION_VIEW` + `FileProvider`. No in-app PDF viewer library needed.
8. **Video Playback**: Stream directly from Telegram without full pre-downloading using **Media3 ExoPlayer**.
9. **Automated Camera Backup**: Scheduled daily photo/video backup using Android **WorkManager** (configurable time, Wi-Fi only option).
10. **Design System & Theme**:
    - **Dark Mode Only** (Deep navy canvas `#080C18`, elevated surfaces `#111827`, electric blue primary `#6C8FFF`, emerald secondary `#00E5A0`).
    - **Typography**: Space Grotesk (headings) + Plus Jakarta Sans (body) + JetBrains Mono (file sizes/tech info).
    - **Smooth Animations**: Native Compose physics-based springs (`spring(dampingRatio, stiffness)`).
11. **Dedicated UI Phase (Phase 5)**: All functionality is built in Phases 0–4; Phase 5 is exclusively reserved for styling and matching the user's specific UI reference images.

---

## 🗺️ 8-Phase Roadmap & Status Tracker

| Phase | Description | Status |
|---|---|---|
| **Phase 0** | **Project Scaffold**: Clone EasyTDLib, rename package to `com.telegramdrive.app`, setup Gradle dependencies (Room, Media3, WorkManager, Coil, Vico, Shimmer) | 🟢 Done |
| **Phase 1** | **Authentication Flow**: API credentials screen → Phone input → OTP verification → 2FA Password → Session persistence | 🟢 Done |
| **Phase 2** | **Folder System**: Saved Messages root, create/rename/delete channels as folders, Room database folder caching | 🟢 Done |
| **Phase 3** | **File Operations**: Upload (photo, video, audio, docs, APK), download to phone storage, soft delete (Trash), progress tracking | 🟢 Done |
| **Phase 4** | **Waterfall Layout**: `LazyVerticalStaggeredGrid`, adaptive `FileCard` components, shimmer loading placeholders, staggered entry animations | 🟢 Done |
| **Phase 5** | **UI/UX Design & Polish**: Dedicated UI phase to match user-provided reference images, design tokens, micro-interactions, splash screen | 🟢 Done |
| **Phase 6** | **Advanced Features**: Search bar (debounced), sort chips, favorites, recent files, 30-day auto-purging trash, video streaming player, storage dashboard | 🟢 Done |
| **Phase 7** | **Camera Auto-Backup & Final Polish**: WorkManager periodic backup, multi-select bulk actions, external share sheet integration, drag & drop | 🟢 Done |

---

## 📦 Key Prebuilt Libraries

- **TDLib**: EasyTDLib ([github.com/kcvabeysinghe/EasyTDLib](https://github.com/kcvabeysinghe/EasyTDLib))
- **Masonry Grid**: `androidx.compose.foundation:foundation` (`LazyVerticalStaggeredGrid`)
- **Video**: `androidx.media3:media3-exoplayer`
- **Images**: `io.coil-kt.coil3:coil-compose`
- **Database**: `androidx.room:room-runtime` & `room-ktx`
- **Background Jobs**: `androidx.work:work-runtime-ktx`
- **Charts**: `com.patrykandpatrick.vico:compose-m3`
- **Shimmer**: `com.valentinilk.shimmer:compose-shimmer`
- **Country Picker**: `com.github.togisoft:jetpack_compose_country_code_picker`
