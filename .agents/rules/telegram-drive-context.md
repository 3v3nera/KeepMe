# 📱 Workspace Context & Rules: Telegram Drive (Android)

## Goal
Build **Telegram Drive (Android)** — a high-performance native Android application using **Kotlin + Jetpack Compose + TDLib** that turns a Telegram account into an unlimited, personal cloud storage drive.

## Full Technical Document
Refer to [TELEGRAM_DRIVE_ANDROID_MASTER_PLAN.md](file:///D:/tdrive/TELEGRAM_DRIVE_ANDROID_MASTER_PLAN.md) for full phase-by-phase build instructions, code patterns, and prebuilt library registries.

## Core Rules for Any Agent Working in this Workspace
1. **Never write custom components when prebuilts exist**:
   - Waterfall / Masonry Layout $\to$ Use `LazyVerticalStaggeredGrid` from `androidx.compose.foundation`
   - Video Playback $\to$ Use `Media3 ExoPlayer`
   - Image Loading $\to$ Use `Coil 3`
   - PDF & External Docs $\to$ Open with native Android `Intent.ACTION_VIEW` via `FileProvider`
   - Shimmer $\to$ Use `compose-shimmer`
   - Storage Charts $\to$ Use `Vico` for Compose Material 3
   - TDLib C++ JNI $\to$ Use `EasyTDLib` pre-compiled `.so` binaries
2. **Dedicated UI Phase (Phase 5)**:
   - Complete functionality across Phases 0–4 before focusing on custom UI theme styling.
   - In Phase 5, match the specific UI reference images provided by the user.
3. **Always adhere to the Dark Mode design tokens**:
   - Background `#080C18`, Surface `#111827`, Primary `#6C8FFF`, Secondary `#00E5A0`.
   - Never use pure `#FFFFFF` text (OLED glare rule) $\to$ use `#E8ECF4` or `#B0B8CC`.
   - No 1px gray/white wireframe box outlines $\to$ use surface elevation and subtle tone shifts.
