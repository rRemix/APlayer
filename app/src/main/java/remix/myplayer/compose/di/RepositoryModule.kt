package remix.myplayer.compose.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import remix.myplayer.compose.repo.AlbumRepoImpl
import remix.myplayer.compose.repo.AlbumRepository
import remix.myplayer.compose.repo.SongRepoImpl
import remix.myplayer.compose.repo.SongRepository
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class RepositoryModule {
  @Singleton
  @Binds
  abstract fun bindSongRepo(repo: SongRepoImpl): SongRepository

  @Singleton
  @Binds
  abstract fun bindAlbumRepo(repo: AlbumRepoImpl): AlbumRepository
}
