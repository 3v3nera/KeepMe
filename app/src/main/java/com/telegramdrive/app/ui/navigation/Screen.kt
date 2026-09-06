package com.telegramdrive.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Folders : Screen("folders")
    object Trash : Screen("trash")
    object Settings : Screen("settings")
    object RecentUploads : Screen("recent_uploads")
    
    object FolderContent : Screen("folder_content/{folderName}/{chatId}") {
        fun createRoute(folderName: String, chatId: Long): String {
            val encodedName = android.net.Uri.encode(folderName)
            return "folder_content/$encodedName/$chatId"
        }
    }
    
    object MediaViewer : Screen("media_viewer/{folderId}/{fileId}") {
        fun createRoute(folderId: Long, fileId: Long) = "media_viewer/$folderId/$fileId"
    }
}
