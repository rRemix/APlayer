package remix.myplayer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import remix.myplayer.repo.AlbumRepoImpl
import remix.myplayer.repo.AlbumRepository
import remix.myplayer.repo.ArtistRepoImpl
import remix.myplayer.repo.ArtistRepository
import remix.myplayer.repo.FolderRepoImpl
import remix.myplayer.repo.FolderRepository
import remix.myplayer.repo.GenreRepoImpl
import remix.myplayer.repo.GenreRepository
import remix.myplayer.repo.HistoryRepoImpl
import remix.myplayer.repo.HistoryRepository
import remix.myplayer.repo.PlayListRepoImpl
import remix.myplayer.repo.PlayListRepository
import remix.myplayer.repo.PlayQueueRepoImpl
import remix.myplayer.repo.PlayQueueRepository
import remix.myplayer.repo.SmbRepoImpl
import remix.myplayer.repo.SmbRepository
import remix.myplayer.repo.SongRepoImpl
import remix.myplayer.repo.SongRepository
import remix.myplayer.repo.WebDavRepository
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

  @Singleton
  @Binds
  abstract fun bindHistoryRepo(repo: HistoryRepoImpl): HistoryRepository

  @Singleton
  @Binds
  abstract fun bindWebDavRepo(repo: remix.myplayer.repo.WebDavRepoImpl): WebDavRepository

  @Singleton
  @Binds
  abstract fun bindSmbRepo(repo: SmbRepoImpl): SmbRepository
}
