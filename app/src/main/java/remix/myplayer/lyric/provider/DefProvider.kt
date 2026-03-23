package remix.myplayer.lyric.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.misc.LyricOrder
import remix.myplayer.data.prefs.LyricPrefs
import remix.myplayer.lyric.LyricSearcher
import remix.myplayer.lyric.provider.network.KuGouProvider
import remix.myplayer.lyric.provider.network.NetEaseProvider
import remix.myplayer.lyric.provider.network.QQProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefProvider @Inject constructor(
  @param:ApplicationContext
  private val context: Context,
  // 默认优先级排序 内嵌-本地-酷狗-网易-QQ-忽略
  private val lyricPrefs: LyricPrefs,
  private val embeddedProvider: EmbeddedProvider,
  private val localProvider: LocalFileProvider,
  private val kuGouProvider: KuGouProvider,
  private val netEaseProvider: NetEaseProvider,
  private val qqProvider: QQProvider,
  private val ignoreProvider: IgnoredProvider
) : ILyricsProvider {

  private val providerMap = mapOf(
    LyricOrder.Embedded to embeddedProvider,
    LyricOrder.Local to localProvider,
    LyricOrder.Kugou to kuGouProvider,
    LyricOrder.Netease to netEaseProvider,
    LyricOrder.Qq to qqProvider,
    LyricOrder.Ignore to ignoreProvider
  )

  /**
   * 根据用户配置的顺序获取 provider 列表
   */
  private fun getOrderedProviders(): List<ILyricsProvider> {
    val providers = ArrayList<ILyricsProvider>()
    try {
      lyricPrefs.generalLyricOrderList.forEach { lyricOrder ->
        providerMap[lyricOrder]?.let { provider ->
          if (!providers.contains(provider)) {
            providers.add(provider)
          }
        }
      }
    } catch (t: Throwable) {
      Timber.w(t, "Failed to get search order from sp, using default order")
      // 如果配置读取失败，使用默认顺序
      return listOf(
        embeddedProvider,
        localProvider,
        kuGouProvider,
        netEaseProvider,
        qqProvider,
        ignoreProvider
      )
    }
    return providers
  }

  override val id = LyricOrder.Def.toString()
  override val displayName = context.getString(LyricOrder.Def.stringRes)

  override suspend fun getLyrics(song: Song): LyricsResult {
    getOrderedProviders().forEach {
      try {
        val ret = it.getLyrics(song)
        Timber.tag(LyricSearcher.TAG).v("Get lyric from: ${it.id}")
        return ret
      } catch (e: Exception) {
        Timber.w(e)
      }
    }

    throw Exception("no lyric found by $id")
  }

}
