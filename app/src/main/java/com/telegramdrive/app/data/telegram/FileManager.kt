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
