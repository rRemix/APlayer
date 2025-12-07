package remix.myplayer.lyric.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.misc.LyricOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IgnoredProvider @Inject constructor(
  @ApplicationContext
  context: Context
) : ILyricsProvider {

  override val id = LyricOrder.Ignore.toString()
  override val displayName = context.getString(LyricOrder.Ignore.stringRes)

  override suspend fun getLyrics(song: Song): LyricsResult {
    return LyricsResult(emptyList(), id)
  }
}