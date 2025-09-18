package remix.myplayer.compose.lyric.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.R
import remix.myplayer.bean.misc.LyricOrder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.LyricsLine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IgnoredProvider @Inject constructor(
  @ApplicationContext
  context: Context
) : ILyricsProvider {

  override val id = LyricOrder.Ignore.toString()
  override val displayName by lazy {
    context.getString(R.string.ignore_lrc)
  }

  override suspend fun getLyrics(song: Song): List<LyricsLine> {
    return emptyList()
  }
}