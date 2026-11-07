package com.telegramdrive.app.data.telegram

import com.telegramdrive.app.engine.TelegramManager
import org.drinkless.tdlib.TdApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    private val telegramManager: TelegramManager
) {
    suspend fun getMe(): TdApi.User {
        return telegramManager.sendSuspend(TdApi.GetMe())
    }

    suspend fun getChat(chatId: Long): TdApi.Chat {
        return telegramManager.sendSuspend(TdApi.GetChat(chatId))
    }

    suspend fun getChannels(limit: Int = 100): TdApi.Chats {
        return telegramManager.sendSuspend(TdApi.GetChats(TdApi.ChatListMain(), limit))
    }
    
    /**
     * Every chat id that could be a KeepMe folder: the whole main list, the whole archive
     * (TDLib only returns chats already loaded, so page them in first) and a title search.
     */
    suspend fun getAllCandidateChatIds(): Set<Long> {
        val ids = linkedSetOf<Long>()
        for (list in listOf<TdApi.ChatList>(TdApi.ChatListMain(), TdApi.ChatListArchive())) {
            try {
                for (page in 0 until 30) {
                    try {
                        telegramManager.sendSuspend(TdApi.LoadChats(list, 100))
                    } catch (e: Exception) {
                        break // 404 = whole list already loaded
                    }
                }
                ids.addAll(telegramManager.sendSuspend(TdApi.GetChats(list, 5000)).chatIds.toList())
            } catch (_: Exception) { }
        }
        for (tag in listOf("[TG]", "[TD]")) {
            try {
                ids.addAll(telegramManager.sendSuspend(TdApi.SearchChats(tag, 100)).chatIds.toList())
            } catch (_: Exception) { }
        }
        return ids
    }

    suspend fun getSupergroupStatus(supergroupId: Long): TdApi.ChatMemberStatus {
        return telegramManager.sendSuspend(TdApi.GetSupergroup(supergroupId)).status
    }

    suspend fun createChannel(title: String): TdApi.Chat {
        return telegramManager.sendSuspend(
            TdApi.CreateNewSupergroupChat(
                title, 
                false, // isForum
                true, // isChannel
                "", // description
                null, // location
                0, // messageAutoDeleteTime
                false // forImport
            )
        )
    }

    suspend fun setChatTitle(chatId: Long, title: String): TdApi.Ok {
        return telegramManager.sendSuspend(TdApi.SetChatTitle(chatId, title))
    }

    suspend fun leaveChat(chatId: Long): TdApi.Ok {
        return telegramManager.sendSuspend(TdApi.LeaveChat(chatId))
    }
}
