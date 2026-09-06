package com.telegramdrive.app.ui.screen.preview

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.telegramdrive.app.domain.model.TelegramFile
import java.io.File

@Composable
fun VideoPreviewPage(
    file: TelegramFile,
    isCurrentPage: Boolean,
    viewModel: MediaPreviewViewModel = hiltViewModel(key = file.messageId.toString())
) {
    val context = LocalContext.current

    // Kick off download (or use already-cached path) once on entry
    LaunchedEffect(file.messageId) {
        viewModel.prepareFile(file)
    }

    val localPath by viewModel.localPath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.downloadProgress.collectAsState()

    // Build ExoPlayer once with error listener
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = isCurrentPage
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("VideoPreviewPage", "ExoPlayer playback error: ${error.errorCodeName}", error)
                }
            })
        }
    }

    // Set media item safely when complete localPath is available
    LaunchedEffect(localPath) {
        val path = localPath ?: return@LaunchedEffect
        val f = File(path)
        if (f.exists() && f.length() > 0) {
            val uri = Uri.fromFile(f)
            if (exoPlayer.currentMediaItem?.localConfiguration?.uri != uri) {
                exoPlayer.setMediaItem(MediaItem.fromUri(uri))
                exoPlayer.prepare()
            }
            exoPlayer.playWhenReady = isCurrentPage
        }
    }

    // Pause when swiping away to prevent MediaCodec resource contention
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Thumbnail placeholder while downloading ──
        if (localPath == null && file.thumbnailPath != null) {
            AsyncImage(
                model = file.thumbnailPath,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── ExoPlayer surface (uses TextureView for Compose compatibility) ──
        if (localPath != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Download progress indicator ──
        if (isLoading) {
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    strokeWidth = 5.dp
                )
            }
        }
    }
}
