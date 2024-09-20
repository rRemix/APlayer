package remix.myplayer.lyrics.provider

import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.lyrics.LyricsLine

object IgnoredProvider : ILyricsProvider {
  override val id = "ignored"
  override val displayName by lazy {
    App.context.getString(R.string.ignore_lrc)
  }

  override fun getLyrics(song: Song): List<LyricsLine> {
    return listOf()
  }
}