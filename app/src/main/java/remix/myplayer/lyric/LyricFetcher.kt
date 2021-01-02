package remix.myplayer.lyric

import io.reactivex.disposables.Disposable
import remix.myplayer.App
import remix.myplayer.bean.mp3.Song
import remix.myplayer.lyric.bean.LrcRow
import remix.myplayer.lyric.bean.LrcRow.LYRIC_EMPTY_ROW
import remix.myplayer.lyric.bean.LyricRowWrapper
import remix.myplayer.lyric.bean.LyricRowWrapper.LYRIC_WRAPPER_NO
import remix.myplayer.lyric.bean.LyricRowWrapper.LYRIC_WRAPPER_SEARCHING
import remix.myplayer.service.MusicService
import remix.myplayer.util.SPUtil
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by remix on 2019/2/6
 */
class LyricFetcher(service: MusicService) {

  private val lrcRows = CopyOnWriteArrayList<LrcRow>()
  private val reference: WeakReference<MusicService> = WeakReference(service)
  private var disposable: Disposable? = null
  private var status = Status.SEARCHING
  var song: Song = Song.EMPTY_SONG
  var offset = 0
  private val lyricSearcher = LyricSearcher()


  fun findCurrentLyric(): LyricRowWrapper {
    val wrapper = LyricRowWrapper()
    wrapper.status = status
    val service = reference.get()

    when {
      service == null || status == Status.NO -> {
        return LYRIC_WRAPPER_NO
      }
      status == Status.SEARCHING -> {
        return LYRIC_WRAPPER_SEARCHING
      }
      status == Status.NORMAL -> {
        val song = service.currentSong
        if (song == Song.EMPTY_SONG) {
          Timber.v("歌曲异常")
          return wrapper
        }
        val progress = service.progress + offset

        for (i in lrcRows.indices.reversed()) {
          val lrcRow = lrcRows[i]
          val interval = progress - lrcRow.time
          if (i == 0 && interval < 0) {
            //未开始歌唱前显示歌曲信息
            wrapper.lineOne = LrcRow("", 0, song.title)
            wrapper.lineTwo = LrcRow("", 0, song.artist + " - " + song.album)
            return wrapper
          } else if (progress >= lrcRow.time) {
            if (lrcRow.hasTranslate()) {
              wrapper.lineOne = LrcRow(lrcRow)
              wrapper.lineOne.content = lrcRow.content
              wrapper.lineTwo = LrcRow(lrcRow)
              wrapper.lineTwo.content = lrcRow.translate
            } else {
              wrapper.lineOne = lrcRow
              wrapper.lineTwo = LrcRow(if (i + 1 < lrcRows.size) lrcRows[i + 1] else LYRIC_EMPTY_ROW)
            }
            return wrapper
          }
        }
        return wrapper
      }
      else -> {
        return LYRIC_WRAPPER_NO
      }
    }
  }

  fun updateLyricRows(song: Song) {
    this.song = song

    if (song == Song.EMPTY_SONG) {
      status = Status.NO
      lrcRows.clear()
      return
    }

    val id = song.id

    disposable?.dispose()
    disposable = lyricSearcher.setSong(song)
        .getLyricObservable()
        .doOnSubscribe {
          status = Status.SEARCHING
        }
        .subscribe({
//          Timber.v("updateLyricRows, lrc: $it")
          if (id == song.id) {
            status = Status.NORMAL
            offset = SPUtil.getValue(App.getContext(), SPUtil.LYRIC_OFFSET_KEY.NAME, id.toString(), 0)
            lrcRows.clear()
            lrcRows.addAll(it)
          } else {
            lrcRows.clear()
            status = Status.NO
          }
        }, { throwable ->
          Timber.v(throwable)
          if (id == song.id) {
            status = Status.NO
            lrcRows.clear()
          }
        })
  }

  fun dispose() {
    disposable?.dispose()
  }

  enum class Status {
    NO, SEARCHING, NORMAL
  }

  companion object {
    const val LYRIC_FIND_INTERVAL = 400L
  }

}