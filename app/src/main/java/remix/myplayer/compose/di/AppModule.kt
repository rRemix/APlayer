package remix.myplayer.compose.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import remix.myplayer.compose.ui.theme.ThemeController
import remix.myplayer.compose.ui.theme.ThemeControllerImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class AppModule {
  @Singleton
  @Binds
  abstract fun bindSongRepo(controller: ThemeControllerImpl): ThemeController
}