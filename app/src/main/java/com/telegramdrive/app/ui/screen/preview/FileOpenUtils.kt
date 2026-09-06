package com.telegramdrive.app.ui.screen.preview

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.telegramdrive.app.domain.model.TelegramFile
import java.io.File

object FileOpenUtils {
    fun openDocumentInExternalApp(context: Context, file: TelegramFile) {
        val localFile = File(file.localFilePath ?: return)
        if (!localFile.exists()) return

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            localFile
        )
        
        val mime = file.mimeType.ifEmpty { "application/pdf" }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mime)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareFileInExternalApp(context: Context, file: TelegramFile) {
        val localFile = File(file.localFilePath ?: return)
        if (!localFile.exists()) return

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            localFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = file.mimeType.ifEmpty { "*/*" }
            putExtra(Intent.EXTRA_STREAM, contentUri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Share with"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
