package remix.myplayer.compose.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import remix.myplayer.compose.lyric.provider.DefProvider
import remix.myplayer.compose.lyric.provider.EmbeddedProvider
import remix.myplayer.compose.lyric.provider.ILyricsProvider
import remix.myplayer.compose.lyric.provider.IgnoredProvider
import remix.myplayer.compose.lyric.provider.KuGouProvider
import remix.myplayer.compose.lyric.provider.LocalFileProvider
import remix.myplayer.compose.lyric.provider.NetEaseProvider
import remix.myplayer.compose.lyric.provider.QQProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LyricProviderModule {

  @Provides
  @Singleton
  @IntoSet
  fun provideEmbedded(provider: EmbeddedProvider): ILyricsProvider = provider

  @Provides
  @Singleton
  @IntoSet
  fun provideIgnored(provider: IgnoredProvider): ILyricsProvider = provider

  @Provides
  @Singleton
  @IntoSet
  fun provideKuGou(kuGouProvider: KuGouProvider): ILyricsProvider = kuGouProvider

  @Provides
  @Singleton
  @IntoSet
  fun provideQQ(provider: QQProvider): ILyricsProvider = provider

  @Provides
  @Singleton
  @IntoSet
  fun provideNetEase(provider: NetEaseProvider): ILyricsProvider = provider

  @Provides
  @Singleton
  @IntoSet
  fun provideLocal(provider: LocalFileProvider): ILyricsProvider = provider

  @Provides
  @Singleton
  @IntoSet
  fun provideDef(provider: DefProvider): ILyricsProvider = provider
}