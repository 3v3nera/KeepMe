package com.telegramdrive.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.telegramdrive.app.data.local.dao.FileMetadataDao
import com.telegramdrive.app.data.local.entity.FileMetadataEntity
import com.telegramdrive.app.data.local.dao.FolderDao
import com.telegramdrive.app.data.local.entity.FolderEntity

@Database(entities = [FolderEntity::class, FileMetadataEntity::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun fileMetadataDao(): FileMetadataDao
}
