@file:Suppress("SpellCheckingInspection")

package com.telegramdrive.app.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import com.telegramdrive.app.data.datastore.SettingsDataStore

class TelegramManager(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : Client.ResultHandler {

    companion object {
        init {
            System.loadLibrary("tdjni")
        }
    }

    private var client: Client? = null

    private val _authState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState.asStateFlow()

    private val _errorFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorFlow: SharedFlow<String> = _errorFlow.asSharedFlow()

    private val _chatIds = MutableStateFlow<List<Long>>(emptyList())
    val chatIds: StateFlow<List<Long>> = _chatIds.asStateFlow()

    private val _chats = MutableStateFlow<Map<Long, TdApi.Chat>>(emptyMap())
    val chats: StateFlow<Map<Long, TdApi.Chat>> = _chats.asStateFlow()

    private val _downloadedFiles = MutableStateFlow<Map<Int, String>>(emptyMap())
    val downloadedFiles: StateFlow<Map<Int, String>> = _downloadedFiles.asStateFlow()

    private val _fileUpdates = MutableSharedFlow<TdApi.File>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val fileUpdates: SharedFlow<TdApi.File> = _fileUpdates.asSharedFlow()

    private val pendingMessageSends = ConcurrentHashMap<Long, CompletableDeferred<TdApi.Message>>()

    fun registerPendingMessage(tempMessageId: Long): CompletableDeferred<TdApi.Message> {
        val deferred = CompletableDeferred<TdApi.Message>()
        pendingMessageSends[tempMessageId] = deferred
        return deferred
    }

    fun unregisterPendingMessage(tempMessageId: Long) {
        pendingMessageSends.remove(tempMessageId)
    }

    private val _currentUser = MutableStateFlow<TdApi.User?>(null)
    val currentUser: StateFlow<TdApi.User?> = _currentUser.asStateFlow()

    private val _userProfilePhotoPath = MutableStateFlow<String?>(null)
    val userProfilePhotoPath: StateFlow<String?> = _userProfilePhotoPath.asStateFlow()

    private val clientLock = Any()
    @Volatile private var parametersSent = false
    @Volatile private var pendingStorageClean = false

    fun initClient() {
        synchronized(clientLock) {
            if (client != null) {
                Log.d("TDLib", "TDLib client already running; skipping duplicate init.")
                return
            }
            parametersSent = false
            Client.execute(TdApi.SetLogVerbosityLevel(0))
            client = Client.create(this, null, null)
            Log.d("TDLib", "Created new TDLib client instance.")
        }
    }

    override fun onResult(result: TdApi.Object) {
        when (result.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateAuthorizationState
                onAuthorizationStateUpdated(update.authorizationState)
            }

            TdApi.UpdateNewChat.CONSTRUCTOR -> {
                val chat = (result as TdApi.UpdateNewChat).chat
                _chats.update { it + (chat.id to chat) }
            }

            TdApi.UpdateFile.CONSTRUCTOR -> {
                val file = (result as TdApi.UpdateFile).file
                _fileUpdates.tryEmit(file)
                if (file.local.isDownloadingCompleted) {
                    _downloadedFiles.update { it + (file.id to file.local.path) }
                    val currentPhoto = _currentUser.value?.profilePhoto
                    if (currentPhoto != null && (file.id == currentPhoto.small?.id || file.id == currentPhoto.big?.id)) {
                        _userProfilePhotoPath.value = file.local.path
                    }
                }
            }

            TdApi.UpdateMessageSendSucceeded.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateMessageSendSucceeded
                Log.d("TDLib", "Message upload succeeded: oldMessageId=${update.oldMessageId}, newId=${update.message.id}")
                pendingMessageSends.remove(update.oldMessageId)?.complete(update.message)
            }

            TdApi.UpdateMessageSendFailed.CONSTRUCTOR -> {
                val update = result as TdApi.UpdateMessageSendFailed
                Log.e("TDLib", "Message upload failed: oldMessageId=${update.oldMessageId}, error=${update.error.code} - ${update.error.message}")
                val exception = Exception(update.error.message ?: "Upload failed (${update.error.code})")
                pendingMessageSends.remove(update.oldMessageId)?.completeExceptionally(exception)
            }
            
            TdApi.Error.CONSTRUCTOR -> {
                val error = result as TdApi.Error
                _errorFlow.tryEmit(error.message)
                Log.e("TDLib", "TDLib Error: ${error.code} - ${error.message}")
            }

            else -> {
                // Exhaustive branch to suppress "missing case" warnings
            }
        }
    }

    private fun onAuthorizationStateUpdated(state: TdApi.AuthorizationState) {
        _authState.value = state

        when (state.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                if (settingsDataStore.hasApiCredentials()) {
                    if (!parametersSent) {
                        Log.d("TDLib", "Credentials exist; sending TdlibParameters automatically.")
                        sendTdlibParameters()
                    } else {
                        Log.d("TDLib", "Parameters already sent to this client; skipping auto-send.")
                    }
                } else {
                    Log.d("TDLib", "Waiting for user to enter API credentials on Welcome screen.")
                }
            }

            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR,
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                parametersSent = true
                if (state.constructor == TdApi.AuthorizationStateReady.CONSTRUCTOR) {
                    Log.d("TDLib", "Engine ready! Fetching chats and user info...")
                    loadChats()
                    loadCurrentUser()
                }
            }

            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                Log.d("TDLib", "Engine completely closed. Rebooting...")
                synchronized(clientLock) {
                    client = null
                    parametersSent = false
                    _authState.value = null
                    if (pendingStorageClean) {
                        cleanStorageDirectory()
                        pendingStorageClean = false
                    }
                }
                initClient()
            }

            else -> {
                // Exhaustive branch to suppress "missing case" warnings
            }
        }
    }

    fun sendTdlibParameters(
        apiId: Int = settingsDataStore.getApiId(),
        apiHash: String = settingsDataStore.getApiHash() ?: ""
    ) {
        if (apiId <= 0 || apiHash.isBlank()) {
            Log.w("TDLib", "Cannot send SetTdlibParameters: invalid credentials (apiId=$apiId)")
            return
        }

        synchronized(clientLock) {
            if (parametersSent) {
                Log.w("TDLib", "SetTdlibParameters already sent to this client; ignoring duplicate call.")
                return
            }

            val currentAuthState = _authState.value
            if (currentAuthState != null && currentAuthState.constructor != TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR) {
                Log.w("TDLib", "Current auth state is not WaitTdlibParameters (${currentAuthState.constructor}). Skipping SetTdlibParameters.")
                if (currentAuthState.constructor == TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR) {
                    parametersSent = true
                }
                return
            }

            val tdlibDir = File(context.filesDir, "tdlib").apply { mkdirs() }
            val request = TdApi.SetTdlibParameters(
                false,
                tdlibDir.absolutePath,
                "",
                ByteArray(0),
                true,
                true,
                true,
                true,
                apiId,
                apiHash,
                "en",
                android.os.Build.MODEL.ifBlank { "Android" },
                android.os.Build.VERSION.RELEASE.ifBlank { "Unknown" },
                "1.0"
            )

            if (client == null) {
                Log.d("TDLib", "Client is null; initializing TDLib client...")
                initClient()
            } else {
                Log.d("TDLib", "Sending SetTdlibParameters (apiId=$apiId) to active TDLib client...")
                parametersSent = true
                client?.send(request, this)
            }
        }
    }

    fun resetAndRestart(clearStorage: Boolean = false) {
        synchronized(clientLock) {
            Log.d("TDLib", "Resetting and restarting TDLib client (clearStorage=$clearStorage)...")
            parametersSent = false
            pendingStorageClean = clearStorage
            val currentClient = client
            if (currentClient != null) {
                try {
                    currentClient.send(TdApi.Destroy()) { result ->
                        Log.d("TDLib", "Destroy result: ${result.constructor}")
                    }
                } catch (e: Exception) {
                    Log.e("TDLib", "Error sending Destroy to client", e)
                    try {
                        currentClient.send(TdApi.Close(), null)
                    } catch (e2: Exception) {
                        Log.e("TDLib", "Error sending Close to client", e2)
                    }
                }
            } else {
                if (clearStorage) {
                    cleanStorageDirectory()
                    pendingStorageClean = false
                }
                _authState.value = null
                initClient()
            }
        }
    }

    private fun cleanStorageDirectory() {
        try {
            val tdlibDir = File(context.filesDir, "tdlib")
            if (tdlibDir.exists()) {
                val success = tdlibDir.deleteRecursively()
                Log.d("TDLib", "Cleaned TDLib storage directory (success=$success): ${tdlibDir.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e("TDLib", "Failed to clean storage directory", e)
        }
    }

    fun restartClient() {
        resetAndRestart(clearStorage = false)
    }

    fun sendPhoneNumber(phoneNumber: String) {
        client?.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), this)
    }

    fun sendVerificationCode(code: String) {
        client?.send(TdApi.CheckAuthenticationCode(code), this)
    }

    fun sendPassword(password: String) {
        client?.send(TdApi.CheckAuthenticationPassword(password), this)
    }

    fun logout() {
        settingsDataStore.clearApiCredentials()
        client?.send(TdApi.LogOut()) {}
    }

    fun downloadFile(fileId: Int) {
        client?.send(TdApi.DownloadFile(fileId, 1, 0, 0, false)) {}
    }

    /** Download a full file at the highest priority (32). Used for preview screens. */
    fun downloadFileHighPriority(fileId: Int) {
        client?.send(TdApi.DownloadFile(fileId, 32, 0, 0, false)) {}
    }

    private fun loadChats() {
        client?.send(TdApi.LoadChats(TdApi.ChatListMain(), 100)) { result ->
            when (result.constructor) {
                TdApi.Ok.CONSTRUCTOR    -> getChatIds()
                TdApi.Error.CONSTRUCTOR -> Log.e("TDLib", "Failed to load chats")
                else -> {} // Suppress missing case warning
            }
        }
    }

    fun reloadCurrentUser() {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        client?.send(TdApi.GetMe()) { result ->
            if (result.constructor == TdApi.User.CONSTRUCTOR) {
                val user = result as TdApi.User
                _currentUser.value = user
                val photo = user.profilePhoto
                val photoFile = photo?.small ?: photo?.big
                if (photoFile != null) {
                    if (photoFile.local.isDownloadingCompleted && photoFile.local.path.isNotBlank() && File(photoFile.local.path).exists()) {
                        _userProfilePhotoPath.value = photoFile.local.path
                    } else {
                        downloadFile(photoFile.id)
                    }
                }
            }
        }
    }

    private fun getChatIds() {
        client?.send(TdApi.GetChats(TdApi.ChatListMain(), 100)) { result ->
            if (result.constructor == TdApi.Chats.CONSTRUCTOR) {
                _chatIds.value = (result as TdApi.Chats).chatIds.toList()
            }
        }
    }

    suspend fun ensureReady() {
        if (client == null) {
            if (settingsDataStore.hasApiCredentials()) {
                initClient()
            } else {
                throw IllegalStateException("TDLib client is not initialized and no API credentials saved.")
            }
        }

        if (_authState.value?.constructor != TdApi.AuthorizationStateReady.CONSTRUCTOR) {
            withTimeoutOrNull(20000) {
                _authState.filter { it?.constructor == TdApi.AuthorizationStateReady.CONSTRUCTOR }.first()
            }
        }
    }

    suspend fun <T : TdApi.Object> sendSuspend(query: TdApi.Function<T>): T {
        ensureReady()

        var attempts = 0
        var delayMs = 1000L
        while (true) {
            val currentClient = client ?: throw IllegalStateException("TDLib client is null")
            val result = suspendCancellableCoroutine<TdApi.Object> { cont ->
                currentClient.send(query) { res ->
                    if (cont.isActive) {
                        cont.resume(res)
                    }
                }
            }
            if (result is TdApi.Error) {
                if (result.code == 429 && attempts < 5) { // Flood wait
                    // Extract wait time from message if possible, usually "Too Many Requests: retry after X"
                    // Fallback to exponential backoff
                    val waitTime = Regex("\\d+").find(result.message)?.value?.toLongOrNull()?.times(1000L) ?: delayMs
                    Log.w("TDLib", "Flood wait 429, retrying after ${waitTime}ms")
                    kotlinx.coroutines.delay(waitTime)
                    delayMs *= 2
                    attempts++
                    continue
                }
                throw Exception(result.message)
            } else {
                @Suppress("UNCHECKED_CAST")
                return result as T
            }
        }
    }
}