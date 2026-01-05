package remix.myplayer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import remix.myplayer.data.db.room.AppDatabase
import remix.myplayer.data.db.room.dao.HistoryDao
import remix.myplayer.data.db.room.dao.MetaDataCacheDao
import remix.myplayer.data.db.room.dao.PlayListDao
import remix.myplayer.data.db.room.dao.PlayQueueDao
import remix.myplayer.data.db.room.dao.WebDavDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

  @Provides
  @Singleton
  fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
    return AppDatabase.getInstance(context)
  }

  @Provides
  fun providePlayListDao(database: AppDatabase): PlayListDao {
    return database.playListDao()
  }

  @Provides
  fun providePlayQueueDao(database: AppDatabase): PlayQueueDao {
    return database.playQueueDao()
  }

  @Provides
  fun provideHistoryDao(database: AppDatabase): HistoryDao {
    return database.historyDao()
  }

  @Provides
  fun provideWebdavDao(database: AppDatabase): WebDavDao {
    return database.webDavDao()
  }

  @Provides
  fun provideMetaDataCacheDao(database: AppDatabase): MetaDataCacheDao {
    return database.metaDataCacheDao()
  }

  @Provides
  fun provideSmbDao(database: AppDatabase): remix.myplayer.data.db.room.dao.SmbDao {
    return database.smbDao()
  }
}