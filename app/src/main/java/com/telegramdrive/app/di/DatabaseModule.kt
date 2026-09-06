package com.telegramdrive.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.telegramdrive.app.data.local.AppDatabase
import com.telegramdrive.app.data.local.dao.FileMetadataDao
import com.telegramdrive.app.data.local.dao.FolderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

class Migration5To6 : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE folders ADD COLUMN emoji TEXT DEFAULT NULL")
    }
}

class Migration6To7 : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE files ADD COLUMN dateTaken INTEGER DEFAULT NULL")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "telegram_drive.db"
        )
        .addMigrations(Migration5To6(), Migration6To7())
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideFolderDao(db: AppDatabase): FolderDao {
        return db.folderDao()
    }

    @Provides
    @Singleton
    fun provideFileMetadataDao(db: AppDatabase): FileMetadataDao {
        return db.fileMetadataDao()
    }
}
