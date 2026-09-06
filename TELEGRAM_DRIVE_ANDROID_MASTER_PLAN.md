# 📘 Telegram Drive Android — Master Build Document

**The "Dumb AI Can Build It" Edition**
Every single step, file, library, and decision documented so precisely that any AI agent can pick this up and execute it without guessing.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Prebuilt Library Registry](#-prebuilt-library-registry)
3. [Phase 0 — Project Scaffold](#phase-0--project-scaffold--easytdlib-fork)
4. [Phase 1 — Authentication Flow](#phase-1--authentication-flow)
5. [Phase 2 — Folder System](#phase-2--folder-system-channels-as-folders)
6. [Phase 3 — File Operations](#phase-3--file-operations-upload--download--delete)
7. [Phase 4 — Waterfall Layout & File Cards](#phase-4--waterfall-layout--file-cards)
8. [Phase 5 — UI/UX Design & Polish](#phase-5--uiux-design--polish-dedicated-ui-phase)
9. [Phase 6 — Advanced Features](#phase-6--advanced-features)
10. [Phase 7 — Camera Auto-Backup & Final Polish](#phase-7--camera-auto-backup-sharing--final-polish)
11. [Design System Reference](#-design-system-reference)
12. [Verification Checklist Per Phase](#-verification-checklist-per-phase)

---

## Project Overview

| Field | Value |
|---|---|
| **App Name** | Telegram Drive (or user's choice) |
| **Platform** | Android (min SDK 24 / Android 7.0) |
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Telegram SDK** | TDLib via EasyTDLib (prebuilt `.so` binaries, zero NDK setup) |
| **Database** | Room (local metadata, favorites, recents, trash) |
| **Image Loading** | Coil 3 (Compose-native) |
| **Video Player** | Media3 ExoPlayer |
| **PDF & External Docs** | Native Android `Intent.ACTION_VIEW` via `FileProvider` (opens in phone's default viewer) |
| **Background Jobs** | WorkManager |
| **Dependency Injection** | Hilt (Dagger) |
| **Navigation** | Compose Navigation (type-safe) |
| **Theme** | Dark mode only |

---

## 🔧 Prebuilt Library Registry

> [!IMPORTANT]
> **Rule: Use prebuilt whenever possible.** For every feature below, the "Source" column tells you exactly which prebuilt library or API to use. Do NOT write custom implementations when a prebuilt exists.

| Feature | Prebuilt Solution | Gradle Dependency / Source | What It Gives You |
|---|---|---|---|
| **Telegram API** | EasyTDLib | Clone [github.com/kcvabeysinghe/EasyTDLib](https://github.com/kcvabeysinghe/EasyTDLib) | Pre-compiled `libtdjni.so` for all architectures, auth flow, chat list, Kotlin coroutines wrapper |
| **Kotlin TDLib Wrapper** | td-ktx | [github.com/tdlibx/td-ktx](https://github.com/tdlibx/td-ktx) | Suspending functions + Flows for TDLib (optional, if EasyTDLib's wrapper isn't enough) |
| **UI Components** | Material 3 | `androidx.compose.material3:material3` | Cards, FAB, TopAppBar, BottomSheet, Dialogs, Chips, SearchBar, PullToRefresh |
| **Staggered/Masonry Grid** | Compose Foundation | `androidx.compose.foundation:foundation` → `LazyVerticalStaggeredGrid` | Built-in Pinterest-style waterfall layout. **Do NOT write a custom one.** |
| **Animations** | Compose Animation (built-in) | `androidx.compose.animation:animation` | `spring()`, `AnimatedVisibility`, `AnimatedContent`, `animateItemPlacement`, `SharedTransitionLayout` |
| **Image Loading** | Coil 3 | `io.coil-kt.coil3:coil-compose` | Async image loading, caching, disk cache, placeholder, crossfade |
| **Video Playback** | Media3 ExoPlayer | `androidx.media3:media3-exoplayer`, `media3-ui` | Streaming, seeking, PiP, subtitle tracks |
| **PDF & Documents** | Android System Viewer Intent | Built-in `Intent.ACTION_VIEW` + `FileProvider` | Opens PDFs/docs instantly in user's installed viewer app (Google Drive, Adobe, etc.) without extra dependencies |
| **Local Database** | Room | `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (KSP) | SQLite with Kotlin coroutines, type-safe queries |
| **DI** | Hilt | `com.google.dagger:hilt-android`, `hilt-compiler` | Singleton scoping, ViewModel injection |
| **Navigation** | Compose Navigation | `androidx.navigation:navigation-compose` | Type-safe routes, animated transitions |
| **Background Jobs** | WorkManager | `androidx.work:work-runtime-ktx` | Periodic camera backup, trash auto-purge |
| **Permissions** | Accompanist Permissions | `com.google.accompanist:accompanist-permissions` | Compose-friendly runtime permission requests |
| **Country Code Picker** | Compose Country Code Picker | `com.github.togisoft:jetpack_compose_country_code_picker` | Phone number input with country flags |
| **Charts** | Vico | `com.patrykandpatrick.vico:compose-m3` | Material 3 donut/pie/bar charts for storage dashboard |
| **Shimmer Loading** | Compose Shimmer | `com.valentinilk.shimmer:compose-shimmer` | Shimmer placeholder effect while loading |
| **Pull to Refresh** | Material 3 (built-in) | `PullToRefreshBox` in M3 1.3+ | Native pull-to-refresh |
| **Swipe Actions** | Material 3 (built-in) | `SwipeToDismissBox` in M3 | Swipe-to-delete on file cards |
| **Splash Screen** | Android Core Splash | `androidx.core:core-splashscreen` | Animated splash screen |
| **DataStore** | Preferences DataStore | `androidx.datastore:datastore-preferences` | Key-value settings storage (backup toggle, schedule time, etc.) |
| **Fonts** | Google Fonts Compose | `androidx.compose.ui:ui-text-google-fonts` | Load Google Fonts at runtime (Space Grotesk, Plus Jakarta Sans, JetBrains Mono) |

---

## Phase 0 — Project Scaffold / EasyTDLib Fork

### Goal
Set up the Android project by forking EasyTDLib, which gives us TDLib integration for free.

### Exact Steps

#### Step 0.1: Clone EasyTDLib
```bash
cd D:\tdrive
git clone https://github.com/kcvabeysinghe/EasyTDLib.git .
```
> This gives us a working Android project with:
> - Pre-compiled `libtdjni.so` for arm64-v8a, armeabi-v7a, x86_64
> - `TelegramManager.kt` with auth handling
> - Jetpack Compose + Material 3 setup
> - Coil for image loading

#### Step 0.2: Rename the Package
| What | From | To |
|---|---|---|
| Package name | `com.example.easytdlib` (or similar) | `com.telegramdrive.app` |
| App name | EasyTDLib | Telegram Drive |
| Application ID | `com.example.easytdlib` | `com.telegramdrive.app` |

**Files to modify:**
- `app/build.gradle.kts` → `applicationId`
- `settings.gradle.kts` → `rootProject.name`
- All Kotlin files → package declarations
- `AndroidManifest.xml` → package reference
- `res/values/strings.xml` → `app_name`

#### Step 0.3: Add Missing Dependencies
Add these to `app/build.gradle.kts` (EasyTDLib already includes some):

```kotlin
dependencies {
    // === ALREADY IN EASYTDLIB (verify versions) ===
    // Compose BOM, Material 3, Coil, Navigation, Hilt

    // === ADD THESE ===
    // Room Database
    val roomVersion = "2.7.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager (camera backup, trash purge)
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // DataStore (settings)
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // Media3 ExoPlayer (video streaming)
    val media3Version = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")

    // Charts (storage dashboard)
    implementation("com.patrykandpatrick.vico:compose-m3:2.1.2")

    // Shimmer loading
    implementation("com.valentinilk.shimmer:compose-shimmer:1.3.1")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.2.0-alpha02")

    // Google Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.8")

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
}
```

#### Step 0.4: Create Folder Structure
Create this package structure inside `app/src/main/java/com/telegramdrive/app/`:

```
com/telegramdrive/app/
├── TelegramDriveApp.kt           # @HiltAndroidApp Application class
├── MainActivity.kt                # Single Activity, Compose host
│
├── data/
│   ├── telegram/                  # TDLib wrappers (from EasyTDLib, modified)
│   │   ├── TdClient.kt           # TDLib lifecycle manager
│   │   ├── AuthManager.kt        # Auth state machine
│   │   └── FileManager.kt        # Upload/download/delete via TDLib
│   ├── local/
│   │   ├── AppDatabase.kt        # Room database class
│   │   ├── dao/                   # Data Access Objects
│   │   └── entity/                # Room entities
│   ├── repository/                # Repository pattern (single source of truth)
│   └── datastore/
│       └── SettingsDataStore.kt   # User preferences
│
├── domain/
│   ├── model/                     # Domain models (not DB entities)
│   └── usecase/                   # Business logic
│
├── ui/
│   ├── theme/                     # Colors, Typography, Shapes, Theme
│   ├── navigation/                # NavGraph, Routes
│   ├── screen/                    # Each screen gets its own package
│   │   ├── auth/
│   │   ├── home/
│   │   ├── files/
│   │   ├── folders/
│   │   ├── settings/
│   │   ├── search/
│   │   └── trash/
│   └── component/                 # Reusable composables
│
├── worker/                        # WorkManager workers
│   ├── CameraBackupWorker.kt
│   └── TrashPurgeWorker.kt
│
└── di/                            # Hilt modules
    ├── AppModule.kt
    ├── DatabaseModule.kt
    └── TelegramModule.kt
```

#### Step 0.5: Verify Build
```bash
# In Android Studio terminal:
./gradlew assembleDebug
```
**Acceptance Criteria**: App compiles. If you run it, the original EasyTDLib auth flow works.

---

## Phase 1 — Authentication Flow

### Goal
Multi-step login: API credentials → Phone → OTP → 2FA Password → Dashboard.

### What EasyTDLib Already Provides
EasyTDLib includes a basic auth flow in `TelegramManager.kt` that handles:
- Setting TDLib parameters (api_id, api_hash)
- Phone number authentication
- OTP code verification
- 2FA password handling

### What We Build On Top
We need to create **polished Compose screens** for each auth step with smooth transitions.

### Step 1.1: Create Auth State Sealed Class

**File**: `domain/model/AuthState.kt`
```kotlin
sealed class AuthState {
    data object Idle : AuthState()
    data object WaitApiCredentials : AuthState()
    data object WaitPhoneNumber : AuthState()
    data object WaitOtpCode : AuthState()
    data class WaitPassword(val hint: String) : AuthState()
    data object Ready : AuthState()
    data class Error(val message: String) : AuthState()
}
```

### Step 1.2: Create AuthViewModel

**File**: `ui/screen/auth/AuthViewModel.kt`

- Inject `AuthManager` (from EasyTDLib, adapted)
- Expose `StateFlow<AuthState>` that the UI observes
- Functions: `submitApiCredentials()`, `submitPhoneNumber()`, `submitOtpCode()`, `submitPassword()`
- On each submit, call the corresponding TDLib function via `AuthManager`
- Listen to TDLib's `AuthorizationState` updates and map them to our `AuthState`

### Step 1.3: Create Auth Screens (4 screens)

Each screen is a `@Composable` function. Use `AnimatedContent` for transitions between them.

#### Screen 1: `ApiCredentialsScreen.kt`
- **UI**: Dark card with Telegram Drive logo at top
- **Fields**: `api_id` (number keyboard), `api_hash` (text)
- **Link**: Clickable text "Get credentials at my.telegram.org"
- **Button**: "Continue" → calls `viewModel.submitApiCredentials()`
- **Validation**: Both fields required, api_id must be numeric

#### Screen 2: `PhoneInputScreen.kt`
- **UI**: Country code picker (use prebuilt library) + phone number field
- **Animation**: Slide in from right
- **Button**: "Send Code" → calls `viewModel.submitPhoneNumber()`

#### Screen 3: `OtpVerificationScreen.kt`
- **UI**: 5-digit code input (individual boxes, auto-advance focus)
- **Countdown timer**: "Resend code in 60s"
- **Paste support**: Detect clipboard OTP
- **Auto-submit**: When all 5 digits entered, automatically submit

#### Screen 4: `PasswordScreen.kt` (conditional)
- **UI**: Password field with visibility toggle
- **Hint text**: Show the password hint from Telegram
- **Only shown** if user has 2FA cloud password enabled

### Step 1.4: Create Auth NavGraph

**File**: `ui/screen/auth/AuthNavigation.kt`

```kotlin
// Observe AuthState in a single AuthHost composable
// Use AnimatedContent with slideInHorizontally / slideOutHorizontally
// to transition between screens based on current AuthState
```

### Step 1.5: Persist Session

- TDLib handles session persistence automatically via its database directory
- Store `api_id` and `api_hash` in encrypted `SettingsDataStore`
- On app restart, check if TDLib session is valid → skip auth, go to dashboard

### Acceptance Criteria
- [ ] Can enter api_id + api_hash and proceed
- [ ] Can enter phone number and receive OTP
- [ ] Can enter OTP and authenticate
- [ ] 2FA password screen appears when needed
- [ ] Session persists across app restarts (auto-login)
- [ ] Smooth slide animations between auth screens
- [ ] Back button navigates to previous auth step

---

## Phase 2 — Folder System (Channels as Folders)

### Goal
Show "Saved Messages" as root storage. Allow creating Telegram channels that appear as folders.

### Step 2.1: Create Folder Domain Model

**File**: `domain/model/TelegramFolder.kt`
```kotlin
data class TelegramFolder(
    val id: Long,           // Telegram chat ID
    val name: String,       // Channel title or "Saved Messages"
    val isSavedMessages: Boolean,
    val fileCount: Int,     // Cached count
    val thumbnailUrl: String?, // Latest file thumbnail
    val createdAt: Long
)
```

### Step 2.2: Create Room Entity & DAO

**File**: `data/local/entity/FolderEntity.kt`
```kotlin
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val chatId: Long,
    val name: String,
    val isSavedMessages: Boolean,
    val fileCount: Int,
    val lastSyncedAt: Long
)
```

**File**: `data/local/dao/FolderDao.kt`
```kotlin
@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY isSavedMessages DESC, name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Upsert
    suspend fun upsertFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}
```

### Step 2.3: Create FolderRepository

**File**: `data/repository/FolderRepository.kt`

- `getAllFolders()`: Fetches from Room cache, then syncs with TDLib in background
- `createFolder(name: String)`: Creates a new private Telegram channel via TDLib, inserts into Room
- `renameFolder(chatId, newName)`: Renames the channel via TDLib
- `deleteFolder(chatId)`: Deletes/leaves the channel via TDLib, removes from Room
- `syncFolders()`: Fetches all user's channels from TDLib, updates Room cache

### Step 2.4: TDLib Channel Operations

**File**: `data/telegram/FileManager.kt` (add methods)

| Operation | TDLib Function | Notes |
|---|---|---|
| Get Saved Messages | `getChat(chatId = selfUserId)` | Self-chat is Saved Messages |
| List channels | `getChats(chatList, limit)` | Filter for channels created by user |
| Create channel | `createNewSupergroupChat(title, isChannel=true)` | Creates a private channel |
| Rename channel | `setChatTitle(chatId, title)` | |
| Delete/leave | `leaveChat(chatId)` | User leaves the channel |

### Step 2.5: Folders Screen

**File**: `ui/screen/folders/FoldersScreen.kt`

- **Top**: "Saved Messages" card (always first, with special cloud icon)
- **Below**: Grid of folder cards (2 columns)
- **Each folder card shows**: Folder icon/thumbnail, name, file count
- **Long-press folder**: Shows bottom sheet with Rename / Delete options
- **FAB**: "Create Folder" → Dialog with name input

### Step 2.6: Folder Content Screen

**File**: `ui/screen/folders/FolderContentScreen.kt`

- Navigated to when user taps a folder
- Shows all files in that folder (waterfall layout — built in Phase 4)
- TopAppBar with folder name + back button

### Acceptance Criteria
- [ ] Saved Messages appears as first folder
- [ ] Can create a new folder (creates a Telegram channel)
- [ ] Can rename a folder
- [ ] Can delete a folder
- [ ] Folder list shows cached data immediately, syncs in background
- [ ] Tapping a folder navigates to its contents

---

## Phase 3 — File Operations (Upload / Download / Delete)

### Goal
Upload any file to a folder, download files to phone storage, delete files.

### Step 3.1: Create File Domain Model

**File**: `domain/model/TelegramFile.kt`
```kotlin
data class TelegramFile(
    val messageId: Long,       // Telegram message ID
    val chatId: Long,          // Which folder (channel) it's in
    val fileName: String,
    val fileSize: Long,        // Bytes
    val mimeType: String,
    val fileType: FileType,    // Image, Video, Document, Audio, Other
    val thumbnailPath: String?, // Local path to cached thumbnail
    val remoteFileId: String,  // TDLib remote file ID
    val localFilePath: String?, // Local path if downloaded
    val uploadedAt: Long,      // Timestamp
    val width: Int?,           // For images/videos
    val height: Int?,          // For images/videos
    val duration: Int?,        // For videos/audio (seconds)
    val isFavorite: Boolean,
    val isInTrash: Boolean
)

enum class FileType { IMAGE, VIDEO, DOCUMENT, AUDIO, OTHER }
```

### Step 3.2: Create File Room Entity & DAO

**File**: `data/local/entity/FileMetadataEntity.kt` — mirrors `TelegramFile` fields

**File**: `data/local/dao/FileMetadataDao.kt`
```kotlin
@Dao
interface FileMetadataDao {
    @Query("SELECT * FROM files WHERE chatId = :chatId AND isInTrash = 0 ORDER BY uploadedAt DESC")
    fun getFilesInFolder(chatId: Long): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM files WHERE isInTrash = 0 ORDER BY uploadedAt DESC LIMIT :limit")
    fun getRecentFiles(limit: Int = 50): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM files WHERE isFavorite = 1 AND isInTrash = 0")
    fun getFavoriteFiles(): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM files WHERE isInTrash = 1")
    fun getTrashFiles(): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM files WHERE fileName LIKE '%' || :query || '%' AND isInTrash = 0")
    fun searchFiles(query: String): Flow<List<FileMetadataEntity>>

    @Upsert
    suspend fun upsertFile(file: FileMetadataEntity)

    @Query("UPDATE files SET isFavorite = :isFav WHERE messageId = :messageId")
    suspend fun setFavorite(messageId: Long, isFav: Boolean)

    @Query("UPDATE files SET isInTrash = 1, trashedAt = :timestamp WHERE messageId = :messageId")
    suspend fun moveToTrash(messageId: Long, timestamp: Long)

    @Query("UPDATE files SET isInTrash = 0, trashedAt = null WHERE messageId = :messageId")
    suspend fun restoreFromTrash(messageId: Long)

    @Query("DELETE FROM files WHERE isInTrash = 1 AND trashedAt < :cutoff")
    suspend fun purgeOldTrash(cutoff: Long)
}
```

### Step 3.3: Create FileRepository

**File**: `data/repository/FileRepository.kt`

| Method | What It Does | TDLib Function |
|---|---|---|
| `getFilesInFolder(chatId)` | Returns Flow from Room, triggers background sync | `getChatHistory(chatId, ...)` |
| `uploadFile(chatId, uri)` | Picks file from system → uploads via TDLib → saves metadata to Room | `sendMessage(chatId, inputMessageDocument/Photo/Video)` |
| `downloadFile(messageId)` | Downloads via TDLib → saves to Downloads folder → updates Room | `downloadFile(fileId, ...)` |
| `deleteFile(messageId)` | Moves to trash (soft delete in Room). Actual Telegram deletion after 30 days | `deleteMessages(chatId, [messageId])` |
| `syncFolder(chatId)` | Fetches message history from TDLib, extracts file metadata, upserts into Room | `getChatHistory` + iterate |

### Step 3.4: Transfer Progress Tracking

**File**: `domain/model/TransferProgress.kt`
```kotlin
data class TransferProgress(
    val fileId: Int,
    val fileName: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val isUpload: Boolean,    // true = upload, false = download
    val state: TransferState  // Queued, InProgress, Paused, Completed, Failed
) {
    val percentage: Float get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
    val speedBytesPerSec: Long = 0 // Calculated from delta
    val etaSeconds: Long = 0       // Calculated from speed
}

enum class TransferState { QUEUED, IN_PROGRESS, PAUSED, COMPLETED, FAILED }
```

### Step 3.5: Upload Flow Implementation

```
User taps FAB "Upload" → System file picker opens → User selects file(s)
→ For each file:
    1. Create TransferProgress (state = QUEUED)
    2. Read file metadata (name, size, mime type, dimensions for images/videos)
    3. Determine TDLib input type:
       - Image (jpg/png/webp) → InputMessagePhoto
       - Video (mp4/mkv) → InputMessageVideo
       - Audio (mp3/ogg) → InputMessageAudio
       - Everything else → InputMessageDocument
    4. Send via TDLib: sendMessage(chatId, inputMessage)
    5. Listen to TDLib's updateFile events for progress
    6. On complete: Save FileMetadataEntity to Room, update TransferProgress (state = COMPLETED)
    7. Show notification with progress
```

### Step 3.6: Download Flow Implementation

```
User taps download button on a file card
→ 1. Create TransferProgress (state = QUEUED)
  2. Call TDLib: downloadFile(remoteFileId, priority=5)
  3. Listen to updateFile events for progress
  4. On complete: Copy from TDLib cache to Downloads/{AppName}/
  5. Notify MediaScanner so file appears in gallery/file manager
  6. Update Room entity with local file path
  7. Show completion notification
```

### Step 3.7: Create Transfer UI Components

**File**: `ui/component/TransferProgressBar.kt`
- Shows: filename, progress bar, percentage, speed (MB/s), ETA, pause/cancel buttons
- Uses `animateFloatAsState` for smooth progress animation

**File**: `ui/component/TransferNotification.kt`
- Android notification with progress bar (use `NotificationCompat.Builder` with `setProgress`)

### Acceptance Criteria
- [ ] Can upload a photo, video, document, and APK from phone
- [ ] Upload progress shown in-app and as notification
- [ ] Can download any file to phone Downloads folder
- [ ] Downloaded files appear in system file manager / gallery
- [ ] Can delete files (soft delete → moves to trash in Room)
- [ ] Transfer progress shows percentage, speed, and ETA
- [ ] Can pause/cancel/resume transfers

---

## Phase 4 — Waterfall Layout & File Cards

### Goal
Display files in a Pinterest-style masonry (staggered) grid with animated cards.

### Step 4.1: Implement WaterfallGrid Component

**File**: `ui/component/WaterfallGrid.kt`

> [!IMPORTANT]
> **USE THE PREBUILT `LazyVerticalStaggeredGrid` from `androidx.compose.foundation`.** Do NOT write a custom masonry layout.

```kotlin
@Composable
fun WaterfallGrid(
    files: List<TelegramFile>,
    onFileClick: (TelegramFile) -> Unit,
    onFileLongPress: (TelegramFile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2), // 2 columns
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier
    ) {
        items(
            count = files.size,
            key = { files[it].messageId }
        ) { index ->
            FileCard(
                file = files[index],
                onClick = { onFileClick(files[index]) },
                onLongPress = { onFileLongPress(files[index]) },
                modifier = Modifier.animateItem() // Built-in item animation
            )
        }
    }
}
```

### Step 4.2: Create FileCard Component

**File**: `ui/component/FileCard.kt`

The file card adapts its appearance based on `FileType`:

| File Type | Card Appearance |
|---|---|
| **IMAGE** | Shows the image thumbnail filling the card. Natural aspect ratio preserved. |
| **VIDEO** | Shows video thumbnail with a play icon overlay and duration badge (e.g. "2:34") in bottom-right |
| **DOCUMENT** | Shows file icon (PDF icon, DOC icon, etc.) + filename + file size. Fixed height. |
| **AUDIO** | Shows audio waveform icon + filename + duration. Fixed height. |
| **OTHER** | Shows generic file icon + filename + file size. Fixed height. |

```kotlin
@Composable
fun FileCard(
    file: TelegramFile,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        when (file.fileType) {
            FileType.IMAGE -> ImageFileCard(file)
            FileType.VIDEO -> VideoFileCard(file)
            else -> DocumentFileCard(file)
        }
    }
}
```

#### ImageFileCard Sub-component
```kotlin
@Composable
fun ImageFileCard(file: TelegramFile) {
    AsyncImage(
        model = file.thumbnailPath ?: file.localFilePath,
        contentDescription = file.fileName,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (file.width != null && file.height != null)
                    Modifier.aspectRatio(file.width.toFloat() / file.height.toFloat())
                else
                    Modifier.height(200.dp)
            )
    )
}
```

#### VideoFileCard Sub-component
```kotlin
@Composable
fun VideoFileCard(file: TelegramFile) {
    Box {
        AsyncImage(
            model = file.thumbnailPath ?: file.localFilePath,
            contentDescription = file.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(180.dp)
        )

        Icon(
            imageVector = Icons.Filled.PlayCircle,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(40.dp).align(Alignment.Center)
        )

        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
        ) {
            Text(
                text = formatDuration(file.duration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
```

#### DocumentFileCard Sub-component
```kotlin
@Composable
fun DocumentFileCard(file: TelegramFile) {
    Row(modifier = Modifier.padding(12.dp)) {
        FileTypeIcon(mimeType = file.mimeType, modifier = Modifier.size(40.dp))

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = file.fileName, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = formatFileSize(file.fileSize), style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

### Step 4.3: Shimmer Loading Placeholder

**File**: `ui/component/ShimmerFileCard.kt`

Use the prebuilt `compose-shimmer` library:
```kotlin
@Composable
fun ShimmerFileCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().shimmer(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((120..220).random().dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
```

### Step 4.4: Item Appearance Animation

```kotlin
AnimatedVisibility(
    visible = true,
    enter = fadeIn(
        animationSpec = tween(durationMillis = 300, delayMillis = index * 50)
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    )
)
```

### Acceptance Criteria
- [ ] Files display in 2-column Pinterest/masonry layout
- [ ] Images show with natural aspect ratio (true waterfall)
- [ ] Videos show thumbnail + play icon + duration
- [ ] Documents show icon + name + size
- [ ] Shimmer placeholders while loading
- [ ] Staggered fade-in animation when items appear
- [ ] Smooth scrolling performance (60fps)
- [ ] Long-press shows context menu (delete, download, favorite)

---

## Phase 5 — UI/UX Design & Polish (Dedicated UI Phase)

> [!IMPORTANT]
> **This is the dedicated UI-only phase.** No new features are added here. This phase is purely about making every pixel perfect and every animation smooth. The user will provide **UI reference images** and we will match that design.

### Goal
Apply a premium, award-winning dark UI design using our design skills (color theory, golden ratio, typography). All existing screens from Phase 0–4 get redesigned.

### Step 5.1: Design System — Color Tokens

**File**: `ui/theme/Color.kt`

Using **Monochromatic Deep Navy with Electric Blue Primary & Emerald Accent** palette.
Engineered using the color-theory-mastery skill (70:25:5 ratio, tinted neutrals, WCAG compliant).

```kotlin
object TDriveColors {
    // === CANVAS (70% — Backgrounds) ===
    val Background =        Color(0xFF080C18)  // Deep space navy (L=5%, S=12%, H=225°)
    val BackgroundAlt =     Color(0xFF0A0F1E)  // Slightly lighter variant

    // === SURFACE (25% — Cards, Sheets, Dialogs) ===
    val Surface =           Color(0xFF111827)  // Card surface (L=10%, S=11%)
    val SurfaceElevated =   Color(0xFF1A2238)  // Modals, bottom sheets (L=15%)
    val SurfaceBright =     Color(0xFF243050)  // Hover states, chips (L=20%)

    // === PRIMARY (5% — Actions, FAB, Links) ===
    val Primary =           Color(0xFF6C8FFF)  // Electric periwinkle blue
    val PrimaryVariant =    Color(0xFF4A6FE5)  // Pressed state
    val PrimaryContainer =  Color(0xFF1A2D5A)  // Muted container background

    // === SECONDARY (Accent — Success states) ===
    val Secondary =         Color(0xFF00E5A0)  // Emerald mint (upload complete, online)
    val SecondaryContainer= Color(0xFF0A2E24)  // Muted green container

    // === ERROR / DANGER ===
    val Error =             Color(0xFFFF6B8A)  // Soft coral-pink
    val ErrorContainer =    Color(0xFF3D1520)  // Muted red container

    // === WARNING ===
    val Warning =           Color(0xFFFFB74D)  // Warm amber
    val WarningContainer =  Color(0xFF3D2E14)

    // === TEXT (NEVER pure white — OLED glare rule) ===
    val OnBackground =      Color(0xFFE8ECF4)  // Primary text (rgba 255,255,255,0.92)
    val OnSurface =         Color(0xFFB0B8CC)  // Secondary text (rgba 255,255,255,0.72)
    val OnSurfaceDim =      Color(0xFF6B7394)  // Tertiary/disabled text (rgba 255,255,255,0.45)

    // === OUTLINE / DIVIDERS ===
    val Outline =           Color(0xFF1E2740)  // Subtle borders (NOT white/gray lines)
    val OutlineVariant =    Color(0xFF2A3456)  // Interactive borders
}
```

### Step 5.2: Design System — Typography

**File**: `ui/theme/Type.kt`

Font pairing: **Space Grotesk** (headings — geometric, modern) + **Plus Jakarta Sans** (body — warm, readable) + **JetBrains Mono** (mono — file sizes, technical).

```kotlin
val SpaceGrotesk = GoogleFont("Space Grotesk")
val PlusJakartaSans = GoogleFont("Plus Jakarta Sans")
val JetBrainsMono = GoogleFont("JetBrains Mono")

val TDriveTypography = Typography(
    headlineLarge = TextStyle(fontFamily = spaceGroteskFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = spaceGroteskFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = spaceGroteskFamily, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = plusJakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = plusJakartaFamily, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = plusJakartaFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = plusJakartaFamily, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = plusJakartaFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = plusJakartaFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = plusJakartaFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = jetBrainsMonoFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = jetBrainsMonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp)
)
```

### Step 5.3: Design System — Shapes & Elevation

**File**: `ui/theme/Shape.kt`

```kotlin
val TDriveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
```

### Step 5.4: Theme Composition

**File**: `ui/theme/Theme.kt`

```kotlin
@Composable
fun TelegramDriveTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        background = TDriveColors.Background,
        surface = TDriveColors.Surface,
        surfaceVariant = TDriveColors.SurfaceElevated,
        primary = TDriveColors.Primary,
        onPrimary = Color.White,
        secondary = TDriveColors.Secondary,
        error = TDriveColors.Error,
        onBackground = TDriveColors.OnBackground,
        onSurface = TDriveColors.OnSurface,
        outline = TDriveColors.Outline
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = TDriveTypography,
        shapes = TDriveShapes,
        content = content
    )
}
```

### Step 5.5: Animation Presets

**File**: `ui/theme/Motion.kt`

```kotlin
object TDriveMotion {
    val screenEnter = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
    ) + fadeIn(animationSpec = tween(200))

    val screenExit = slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
    ) + fadeOut(animationSpec = tween(150))

    fun itemEnter(index: Int) = fadeIn(
        animationSpec = tween(300, delayMillis = index * 40)
    ) + slideInVertically(
        initialOffsetY = { it / 4 },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
    )

    val fabSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 500f)
    val cardPress = spring<Float>(dampingRatio = 0.5f, stiffness = 800f)
    val contentSwap = fadeIn(tween(200)) togetherWith fadeOut(tween(150))
}
```

### Step 5.6: Apply Theme to All Existing Screens
Go through **every screen** from Phase 1–4 and:
1. Wrap in `TelegramDriveTheme`
2. Replace hardcoded colors with `MaterialTheme.colorScheme.*`
3. Replace hardcoded text styles with `MaterialTheme.typography.*`
4. Replace hardcoded shapes with `MaterialTheme.shapes.*`
5. Add animation presets from `TDriveMotion`
6. Add shimmer loading states
7. Add empty state illustrations

### Step 5.7: Apply User's UI Reference
> Match the user-provided reference images (spacing, card styling, icons, navigation).

### Step 5.8: Micro-Interactions & Polish
- File card scale down to 0.97 on press
- FAB 45° rotation with staggered child bounce
- Pull to refresh custom cloud animation
- Delete swipe with red background
- Favorite star bounce
- Search bar expand animation

### Step 5.9: Splash Screen
Use `androidx.core:core-splashscreen` for smooth start-up.

### Acceptance Criteria
- [ ] All colors use `TDriveColors` tokens
- [ ] All text uses `TDriveTypography`
- [ ] Matches user's UI reference images
- [ ] Every interactive element has a tap/press animation
- [ ] Screen transitions use consistent spring physics
- [ ] Shimmer loading on all data-loading states
- [ ] No white borders or outlines on cards (surface elevation only)
- [ ] Text is WCAG AA compliant (≥4.5:1 contrast)
- [ ] 60fps smooth on a real Android device

---

## Phase 6 — Advanced Features

### Goal
Add Search, Sort, Favorites, Recents, Trash, File Preview, and Storage Dashboard.

### Step 6.1: Search
**File**: `ui/screen/search/SearchScreen.kt`
- M3 `SearchBar` composable with 300ms debounce
- Queries `FileMetadataDao.searchFiles(query)`
- Results displayed in WaterfallGrid

### Step 6.2: Sort
**File**: `ui/screen/files/FilesScreen.kt`
- M3 `FilterChip` row (Name, Date, Size, Type)
- Sorts `Flow<List<FileMetadataEntity>>` in ViewModel

### Step 6.3: Favorites
- Star icon on each FileCard
- Stored as `isFavorite` in Room DB
- Displayed in "Favorites" section on HomeScreen

### Step 6.4: Recents
- Auto-tracks `lastAccessedAt` timestamp
- Displayed in "Recents" horizontal scroll section on HomeScreen

### Step 6.5: Trash
**File**: `ui/screen/trash/TrashScreen.kt`
- Soft delete (`isInTrash = true`, `trashedAt = now`)
- Trash screen with "Restore" and "Delete Permanently"
- `TrashPurgeWorker` runs daily to purge files trashed >30 days ago

### Step 6.6: File Preview & Opening

#### Image Preview
**File**: `ui/screen/preview/ImagePreviewScreen.kt`
- Full-screen viewer with pinch-to-zoom, pan, and horizontal swipe

#### Video Preview
**File**: `ui/screen/preview/VideoPreviewScreen.kt`
- Media3 ExoPlayer with streaming playback from Telegram

#### PDF & Other Documents (Native System Intent)
**Implementation**: Open in user's default app via `FileProvider` Intent. No extra third-party viewer library required!

```kotlin
fun openDocumentInExternalApp(context: Context, file: TelegramFile) {
    val localFile = File(file.localFilePath ?: return)
    val contentUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        localFile
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, file.mimeType.ifEmpty { "application/pdf" })
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(Intent.createChooser(intent, "Open with"))
}
```

#### Audio Preview
- Bottom mini player bar with playback controls

### Step 6.7: Storage Dashboard
**File**: `ui/screen/home/StorageDashboard.kt`
- Vico donut chart showing storage breakdown by category (Images, Videos, Documents, Audio, Other)
- Total files and total storage used

### Acceptance Criteria
- [ ] Search finds files by name with debounced input
- [ ] Sort chips change file order instantly
- [ ] Can favorite/unfavorite files with animated star
- [ ] Recent files section shows last 20 accessed files
- [ ] Deleted files go to Trash, recoverable for 30 days
- [ ] Trash auto-purges after 30 days
- [ ] Image viewer: pinch-zoom, pan, swipe between images
- [ ] Video streams without full download first
- [ ] PDF & documents open in user's phone viewer app via FileProvider
- [ ] Storage dashboard shows donut chart breakdown

---

## Phase 7 — Camera Auto-Backup, Sharing & Final Polish

### Goal
Scheduled camera photo backup, Android share sheet, drag-and-drop, and final production polish.

### Step 7.1: Camera Auto-Backup
**File**: `worker/CameraBackupWorker.kt`
- WorkManager `PeriodicWorkRequest` running daily at user-selected time (default 2:00 AM)
- Scans `MediaStore` for new photos/videos since last backup
- Uploads to dedicated "Camera Backup" Telegram channel
- Settings controls: On/off toggle, time picker, Wi-Fi only, charging only

### Step 7.2: Multi-Select & Bulk Actions
- Long-press to enter selection mode
- Bottom action bar: Delete All, Download All, Move All, Share All

### Step 7.3: Share
- Share FROM app to external apps via `Intent.ACTION_SEND`
- Share TO app from external apps via incoming `ACTION_SEND` intent filter

### Step 7.4: Drag & Drop (Between Folders)
- Long-press and drag file onto folder card in FoldersScreen to move it

### Step 7.5: Final Production Polish
- ProGuard rules for TDLib and Room
- Comprehensive error handling & flood-wait mitigation
- Custom branded app launcher icon
- Android 13+ runtime permissions handled cleanly

### Acceptance Criteria
- [ ] Camera backup runs daily at scheduled time
- [ ] Backup respects Wi-Fi only and charging settings
- [ ] Can enable/disable backup from Settings
- [ ] Multi-select works with visual feedback
- [ ] Bulk delete, download, and share work correctly
- [ ] Can share files to and from other apps
- [ ] Drag & drop moves files between folders
- [ ] App handles network errors and Telegram flood-waits gracefully
- [ ] Production-ready APK builds successfully

---

## 🎨 Design System Reference

### Color Palette Visual
```
┌──────────────────────────────────────────────────┐
│ Background        #080C18  ████████████████████  │  70%
│ Surface           #111827  ██████████████████     │  25%
│ Surface Elevated  #1A2238  ████████████████       │
│ Surface Bright    #243050  ██████████████         │
│──────────────────────────────────────────────────│
│ Primary           #6C8FFF  ████                   │  5%
│ Primary Variant   #4A6FE5  ████                   │
│ Primary Container #1A2D5A  ████                   │
│──────────────────────────────────────────────────│
│ Secondary         #00E5A0  ███                    │  Success
│ Error             #FF6B8A  ███                    │  Danger
│ Warning           #FFB74D  ███                    │  Warning
│──────────────────────────────────────────────────│
│ Text Primary      #E8ECF4  NEVER pure #FFFFFF    │
│ Text Secondary    #B0B8CC                        │
│ Text Dim          #6B7394                        │
│ Outline           #1E2740  NO white/gray borders │
└──────────────────────────────────────────────────┘
```

### Typography Scale
```
Heading:    Space Grotesk Bold    42sp / 28sp / 22sp / 18sp
Body:       Plus Jakarta Sans    16sp / 14sp / 12sp
Mono:       JetBrains Mono       12sp / 10sp (file sizes, tech info)
```

### Spacing Scale (Fibonacci-inspired)
```
4dp → 8dp → 12dp → 16dp → 24dp → 32dp → 48dp → 64dp → 96dp
```

---

## ✅ Verification Checklist Per Phase

### Phase 0 ✓
- [ ] Project compiles with `./gradlew assembleDebug`
- [ ] Package renamed to `com.telegramdrive.app`
- [ ] All dependencies added and resolved

### Phase 1 ✓
- [ ] Can login with real Telegram account
- [ ] Session persists across restarts
- [ ] All 4 auth screens have smooth transitions

### Phase 2 ✓
- [ ] Saved Messages folder visible
- [ ] Can create/rename/delete folders
- [ ] Folder data cached in Room

### Phase 3 ✓
- [ ] Can upload any file type
- [ ] Can download files to phone storage
- [ ] Progress shows %, speed, ETA
- [ ] Soft delete works (trash)

### Phase 4 ✓
- [ ] Waterfall layout with natural aspect ratios
- [ ] Image/Video/Document cards look correct
- [ ] Shimmer loading while fetching
- [ ] 60fps scroll performance

### Phase 5 ✓
- [ ] Matches user's UI reference
- [ ] All colors via design tokens
- [ ] All text via typography system
- [ ] Every tap has animation feedback
- [ ] WCAG AA contrast compliance

### Phase 6 ✓
- [ ] Search works with debounce
- [ ] Sort by name/date/size/type
- [ ] Favorites persist and display
- [ ] Recents auto-track
- [ ] Trash with 30-day auto-purge
- [ ] Image & video viewer works
- [ ] PDFs open in user's installed reader app via FileProvider
- [ ] Storage dashboard with chart

### Phase 7 ✓
- [ ] Camera backup runs on schedule
- [ ] Share to/from works
- [ ] Multi-select + bulk actions
- [ ] Error handling is user-friendly
- [ ] No crashes on edge cases
- [ ] Production-ready APK builds

---

> [!TIP]
> **Recommended execution order**: Phase 0 → 1 → 2 → 3 → 4 → 5 (UI Reference) → 6 → 7
> Each phase is self-contained and testable. You can install the APK and verify after each phase.
