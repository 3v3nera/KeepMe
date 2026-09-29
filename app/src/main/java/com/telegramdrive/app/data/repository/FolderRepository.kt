package com.telegramdrive.app.data.repository

import com.telegramdrive.app.data.local.dao.FileMetadataDao
import com.telegramdrive.app.data.local.dao.FolderDao
import com.telegramdrive.app.data.local.entity.FolderEntity
import com.telegramdrive.app.data.telegram.FileManager
import com.telegramdrive.app.domain.model.TelegramFolder
import com.telegramdrive.app.ui.utils.FolderEmojiUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.drinkless.tdlib.TdApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val fileMetadataDao: FileMetadataDao,
    private val fileManager: FileManager
) {
    fun getAllFolders(): Flow<List<TelegramFolder>> {
        return folderDao.getAllFolders().map { entities ->
            entities.map { entity ->
                TelegramFolder(
                    id = entity.chatId,
                    name = if (entity.isSavedMessages) entity.name else cleanFolderName(entity.name),
                    isSavedMessages = entity.isSavedMessages,
                    fileCount = entity.fileCount,
                    thumbnailUrl = null,
                    createdAt = entity.lastSyncedAt,
                    emoji = entity.emoji ?: if (entity.isSavedMessages) "☁️" else FolderEmojiUtils.getDeterministicEmoji(entity.chatId)
                )
            }
        }
    }

    suspend fun syncFolders() {
        try {
            val me = fileManager.getMe()
            
            // Upsert "Saved Messages" (chat ID is usually the user's ID)
            val existingSaved = folderDao.getFolderById(me.id)
            folderDao.upsertFolder(
                FolderEntity(
                    chatId = me.id,
                    name = "Saved Messages",
                    isSavedMessages = true,
                    fileCount = 0,
                    lastSyncedAt = System.currentTimeMillis(),
                    emoji = existingSaved?.emoji ?: "☁️"
                )
            )

            // Get Channels
            val chatsList = fileManager.getChannels(100)
            val keptChatIds = mutableSetOf<Long>()
            
            for (chatId in chatsList.chatIds) {
                try {
                    val chat = fileManager.getChat(chatId)
                    // We only want channels where we are the creator/admin for folders
                    if (chat.type is org.drinkless.tdlib.TdApi.ChatTypeSupergroup) {
                        val supergroupType = chat.type as org.drinkless.tdlib.TdApi.ChatTypeSupergroup
                        if (supergroupType.isChannel && (chat.title.contains("[TG]") || chat.title.contains("[TD]"))) {
                            keptChatIds.add(chat.id)
                            val existingFolder = folderDao.getFolderById(chat.id)
                            val folderEmoji = existingFolder?.emoji ?: FolderEmojiUtils.getDeterministicEmoji(chat.id)
                            folderDao.upsertFolder(
                                FolderEntity(
                                    chatId = chat.id,
                                    name = chat.title,
                                    isSavedMessages = false,
                                    fileCount = 0,
                                    lastSyncedAt = System.currentTimeMillis(),
                                    emoji = folderEmoji
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore individual chat sync errors
                    keptChatIds.add(chatId)
                }
            }

            // Folders deleted or left elsewhere (e.g. in Telegram itself) must disappear here too.
            // Only prune when the channel list was not truncated by the fetch limit.
            if (chatsList.chatIds.size < 100) {
                for (localId in folderDao.getCustomFolderIds()) {
                    if (localId !in keptChatIds) {
                        folderDao.deleteFolderById(localId)
                        fileMetadataDao.deleteFilesInChat(localId)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createFolder(name: String, emoji: String? = null): Long {
        val clean = cleanFolderName(name)
        val finalName = "$clean [TG]"
        val chat = fileManager.createChannel(finalName)
        val finalEmoji = if (!emoji.isNullOrBlank()) emoji else FolderEmojiUtils.getRandomEmoji()
        folderDao.upsertFolder(
            FolderEntity(
                chatId = chat.id,
                name = chat.title,
                isSavedMessages = false,
                fileCount = 0,
                lastSyncedAt = System.currentTimeMillis(),
                emoji = finalEmoji
            )
        )
        return chat.id
    }

    suspend fun renameFolder(chatId: Long, newName: String, emoji: String? = null) {
        val clean = cleanFolderName(newName)
        val finalName = "$clean [TG]"
        fileManager.setChatTitle(chatId, finalName)
        val chat = fileManager.getChat(chatId)
        val existing = folderDao.getFolderById(chatId)
        val updatedEmoji = emoji ?: existing?.emoji ?: FolderEmojiUtils.getDeterministicEmoji(chatId)
        folderDao.upsertFolder(
            FolderEntity(
                chatId = chat.id,
                name = chat.title,
                isSavedMessages = false,
                fileCount = existing?.fileCount ?: 0,
                lastSyncedAt = System.currentTimeMillis(),
                emoji = updatedEmoji
            )
        )
    }

    suspend fun deleteFolder(chatId: Long) {
        fileManager.leaveChat(chatId)
        folderDao.deleteFolder(
            FolderEntity(chatId, "", false, 0, 0)
        )
        fileMetadataDao.deleteFilesInChat(chatId)
    }

    suspend fun getSavedMessagesChatId(): Long {
        return try {
            fileManager.getMe().id
        } catch (e: Exception) {
            0L
        }
    }

    companion object {
        fun cleanFolderName(raw: String): String {
            return raw.replace("[TG]", "").replace("[TD]", "").trim()
        }
    }
}
