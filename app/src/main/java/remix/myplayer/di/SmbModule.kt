package remix.myplayer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import remix.myplayer.data.model.smb.SmbClientDelegate
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmbModule {

  @Provides
  @Singleton
  fun provideSmbClientDelegate(): SmbClientDelegate? {
    return try {
      val clazz = Class.forName("remix.myplayer.smb.SmbClientDelegateImpl")
      clazz.getDeclaredConstructor().newInstance() as SmbClientDelegate
    } catch (e: Exception) {
      Timber.e(e, "Failed to load SmbClientDelegateImpl")
      null
    }
  }
}