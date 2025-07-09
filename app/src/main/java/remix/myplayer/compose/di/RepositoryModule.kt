package remix.myplayer.compose.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import remix.myplayer.compose.repo.AlbumRepoImpl
import remix.myplayer.compose.repo.AlbumRepository
import remix.myplayer.compose.repo.ArtistRepoImpl
import remix.myplayer.compose.repo.ArtistRepository
import remix.myplayer.compose.repo.FolderRepoImpl
import remix.myplayer.compose.repo.FolderRepository
import remix.myplayer.compose.repo.GenreRepoImpl
import remix.myplayer.compose.repo.GenreRepository
import remix.myplayer.compose.repo.PlayListRepoImpl
import remix.myplayer.compose.repo.PlayListRepository
import remix.myplayer.compose.repo.PlayQueueRepoImpl
import remix.myplayer.compose.repo.PlayQueueRepository
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

  @Singleton
  @Binds
  abstract fun bindArtistRepo(repo: ArtistRepoImpl): ArtistRepository

  @Singleton
  @Binds
  abstract fun bindGenreRepo(repo: GenreRepoImpl): GenreRepository

  @Singleton
  @Binds
  abstract fun bindPlayListRepo(repo: PlayListRepoImpl): PlayListRepository

  @Singleton
  @Binds
  abstract fun bindFolderRepo(repo: FolderRepoImpl): FolderRepository

  @Singleton
  @Binds
  abstract fun bindPlayQueueRepo(repo: PlayQueueRepoImpl): PlayQueueRepository
}
