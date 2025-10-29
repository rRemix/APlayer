package remix.myplayer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import remix.myplayer.lyric.provider.DefProvider
import remix.myplayer.lyric.provider.EmbeddedProvider
import remix.myplayer.lyric.provider.ILyricsProvider
import remix.myplayer.lyric.provider.IgnoredProvider
import remix.myplayer.lyric.provider.LocalFileProvider
import remix.myplayer.lyric.provider.network.KuGouProvider
import remix.myplayer.lyric.provider.network.NetEaseProvider
import remix.myplayer.lyric.provider.network.QQProvider
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