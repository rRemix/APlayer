package remix.myplayer.helper

import android.content.Context
import android.provider.MediaStore
import io.reactivex.CompletableSource
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.collections.forEachWithIndex
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.request.network.RxUtil.applySingleScheduler
import remix.myplayer.theme.Theme
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.ToastUtil
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

object M3UHelper {
  private val databaseRepository = DatabaseRepository.getInstance()
  private const val TAG = "M3UHelper"

  private const val HEADER = "#EXTM3U"
  private const val ENTRY = "#EXTINF:"
  private const val DURATION_SEPARATOR = ","

  /**
   * 导入歌单
   */
  @JvmStatic
  fun importM3UFile(context: Context, file: File, playlistName: String, newCreate: Boolean): Disposable? {
    val dialog = Theme.getBaseDialog(context)
        .title(R.string.saveing)
        .content(R.string.please_wait)
        .cancelable(false)
        .progress(true, 0)
        .progressIndeterminateStyle(false).build()
    dialog.show()

    return Single
        .just(file)
        .filter {
          it.isFile && it.canRead()
        }
        .doOnSubscribe {
          if (newCreate) {
            val newId = databaseRepository.insertPlayList(playlistName).blockingGet()
            if (newId <= 0) {
              throw Exception("insert $playlistName failed")
            }
          }
        }
        .map {
          val audioIds = ArrayList<Int>()
          file.readLines().forEachWithIndex { i: Int, path: String ->
            if (i != 0 && !path.startsWith(ENTRY)) {
              val id: Int
              // 先直接判断本地文件是否存在
              val song = File(path)
              id = if (song.exists() && song.isFile) {
                MediaStoreUtil.getSongIdByUrl(path)
              } else {
                // 再根据歌曲名去查找
                MediaStoreUtil.getSongId(MediaStore.Audio.Media.DATA + " like ?",
                    arrayOf("%" + path.replace("\\", "/")))
              }
              if (id > 0) {
                audioIds.add(id)
              }
            }
          }
          return@map audioIds
              .filter { audioId ->
                audioId > 0
              }
        }
        .flatMapSingle {
          databaseRepository.insertToPlayList(it, playlistName)
        }
        .compose(applySingleScheduler())
        .subscribe(
            {
              dialog.dismiss()
              ToastUtil.show(context, App.getContext().getString(R.string.import_playlist_to_count, playlistName, it))
            },
            {
              dialog.dismiss()
              ToastUtil.show(context, R.string.import_fail, it.toString())
            })
  }

  @JvmStatic
  fun importLocalPlayList(context: Context, playlistLocal: Map<String, List<Int>>, select: Array<CharSequence>): Disposable? {
    val singles = playlistLocal.entries
        .filter {
          select.contains(it.key)
        }
        .map {
          Single
              .fromCallable {
                databaseRepository.insertPlayList(it.key).subscribe()
                databaseRepository.insertToPlayList(it.value, it.key).blockingGet()
              }
              .onErrorResumeNext(Single.just(0))
        }

    return Single
        .zip(singles) { objects ->
          var count = 0
          objects.forEach {
            count += it as Int
          }
          count
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
          ToastUtil.show(context, R.string.import_count, it.toString())
        }, {
          ToastUtil.show(context, R.string.import_fail, it.toString())
        })


  }

  @JvmStatic
  fun exportPlayListToFile(context: Context, playlistName: String, file: File): Disposable? {
    return databaseRepository
        .getPlayList(playlistName)
        .flatMap {
          databaseRepository.getPlayListSongs(context, it)
        }
        .flatMapCompletable { songs ->
          CompletableSource {
            val bw = BufferedWriter(FileWriter(file))
            bw.write(HEADER)
            for (song in songs) {
              bw.newLine()
              bw.write(ENTRY + song.getDuration() + DURATION_SEPARATOR + song.artist + " - " + song.title)
              bw.newLine()
              bw.write(song.url)
            }
            bw.close()
            it.onComplete()
          }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
          ToastUtil.show(context, R.string.export_success)
        }, {
          ToastUtil.show(context, R.string.export_fail, it.toString())
        })
  }
}

