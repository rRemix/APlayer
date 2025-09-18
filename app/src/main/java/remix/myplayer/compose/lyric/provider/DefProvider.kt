package remix.myplayer.compose.lyric.provider

import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.misc.LyricOrder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.LyricsLine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefProvider @Inject constructor(
  //默认优先级排序 内嵌-本地-酷狗-网易-QQ-忽略
  embeddedProvider: EmbeddedProvider,
  localProvider: LocalFileProvider,
  kuGouProvider: KuGouProvider,
  netEaseProvider: NetEaseProvider,
  qqProvider: QQProvider,
  ignoreProvider: IgnoredProvider
) : ILyricsProvider {

  private val providers =
    listOf(embeddedProvider, localProvider, kuGouProvider, netEaseProvider, qqProvider, ignoreProvider)

  override val id = LyricOrder.Def.toString()
  override val displayName = App.context.getString(R.string.default_lyric_priority)

  override suspend fun getLyrics(song: Song): List<LyricsLine> {
    providers.forEach {
      try {
        val ret = it.getLyrics(song)
        return ret
      } catch (e: Exception) {
        Timber.w(e)
      }
    }

    throw Exception("no lyric found by def")
  }

}