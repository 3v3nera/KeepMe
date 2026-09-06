package com.telegramdrive.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.telegramdrive.app.engine.TelegramManager
import com.telegramdrive.app.ui.components.AnimatedMeshBackground
import com.telegramdrive.app.ui.navigation.AppNavGraph
import com.telegramdrive.app.ui.screen.auth.AuthHost
import com.telegramdrive.app.ui.screen.share.ShareToAppScreen
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.theme.TelegramDriveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import org.drinkless.tdlib.TdApi
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var telegramManager: TelegramManager

    private val _sharedUris = MutableStateFlow<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            telegramManager.authState.value == null
        }
        splashScreen.setOnExitAnimationListener { viewProvider ->
            viewProvider.remove()
        }
        enableEdgeToEdge()

        telegramManager.initClient()
        _sharedUris.value = getUrisFromIntent(intent)

        setContent {
            val sharedUris by _sharedUris.collectAsState()
            val authState by telegramManager.authState.collectAsState()

            TelegramDriveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TDriveColors.Background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. App Content
                        if (authState?.constructor == TdApi.AuthorizationStateReady.CONSTRUCTOR) {
                            if (sharedUris.isNotEmpty()) {
                                ShareToAppScreen(
                                    sharedUris = sharedUris,
                                    onClose = {
                                        _sharedUris.value = emptyList()
                                        intent?.action = Intent.ACTION_MAIN
                                    }
                                )
                            } else {
                                AppNavGraph()
                            }
                        } else if (authState != null) {
                            AuthHost(
                                onAuthSuccess = {
                                    // Handled automatically by the state change above
                                }
                            )
                        } else {
                            // Cold-start / resume fallback
                            AnimatedMeshBackground()
                        }

                        // 2. Single unified launch splash overlay on top
                        if (!com.telegramdrive.app.ui.screen.home.AppSessionState.hasPlayedSplash) {
                            com.telegramdrive.app.ui.components.LaunchSplashOverlay(
                                isReady = authState?.constructor == TdApi.AuthorizationStateReady.CONSTRUCTOR || (authState != null && authState?.constructor != TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR),
                                onSplashComplete = {
                                    com.telegramdrive.app.ui.screen.home.AppSessionState.hasPlayedSplash = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _sharedUris.value = getUrisFromIntent(intent)
    }

    private fun getUrisFromIntent(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val uris = mutableListOf<Uri>()
        if (intent.action == Intent.ACTION_SEND) {
            val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            if (uri != null) uris.add(uri)
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val multipleUris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            if (multipleUris != null) uris.addAll(multipleUris)
        }
        return uris
    }
}