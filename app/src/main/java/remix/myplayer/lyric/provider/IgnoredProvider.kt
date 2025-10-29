package remix.myplayer.lyric.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.bean.misc.LyricOrder
import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.lyric.LyricsLine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IgnoredProvider @Inject constructor(
  @ApplicationContext
  context: Context
) : ILyricsProvider {

  override val id = LyricOrder.Ignore.toString()
  override val displayName = context.getString(LyricOrder.Ignore.stringRes)

  override suspend fun getLyrics(song: Song): List<LyricsLine> {
    return emptyList()
  }
}