package remix.myplayer.helper

import io.reactivex.CompletableSource
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.collections.forEachWithIndex
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.request.network.RxUtil
import remix.myplayer.util.Global
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.PlayListUtil
import remix.myplayer.util.PlayListUtil.*
import remix.myplayer.util.ToastUtil
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter


object M3UHelper {
    private const val TAG = "M3UHelper"

    private const val HEADER = "#EXTM3U"
    private const val ENTRY = "#EXTINF:"
    private const val DURATION_SEPARATOR = ","

    /**
     * 导入歌单
     */
    @JvmStatic
    fun importM3UFile(file: File, playlistName: String, newCreate: Boolean): Disposable? {
        return Observable.just(file).filter { it.isFile && it.canRead() }
                .doOnSubscribe {
                    if (newCreate) {
                        val playListId = PlayListUtil.addPlayList(playlistName)
                        if (playListId == -1) {
                            throw Throwable("name must not be empty")
                        }
                        if (playListId == -2) {
                            throw Throwable("$playlistName already exist")
                        }
                    }
                }
                .map {
                    val playlistSongs = ArrayList<Int>()
                    it.readLines().forEachWithIndex { i: Int, path: String ->
                        if (i != 0 && !path.startsWith(ENTRY)) {
                            val song = File(path)
                            if (song.exists() && song.isFile) {
                                playlistSongs.add(MediaStoreUtil.getSongIdByUrl(path))
                            }
                        }
                    }
                    return@map PlayListUtil.addMultiSongs(playlistSongs, playlistName)
                }
                .compose(RxUtil.applyScheduler())
                .subscribe({
                    ToastUtil.show(App.getContext(), App.getContext().getString(R.string.import_playlist_to_count, playlistName, it))
                }, {
                    ToastUtil.show(App.getContext(), R.string.import_fail, it.toString())
                })
    }

    @JvmStatic
    fun importLocalPlayList(map: Map<String, List<Int>>, select: Array<CharSequence>): Disposable? {
        var count = 0
        return Observable.fromIterable(map.entries)
                .filter {
                    select.contains(it.key)
                }
                .map {
                    var exist = false
                    Global.PlayList.forEach { playlist ->
                        if (playlist.name == it.key) {
                            exist = true
                            return@forEach
                        }
                    }
                    if (!exist) {
                        val playListId = PlayListUtil.addPlayList(it.key)
                        if (playListId == -1) {
                            throw Throwable("name must not be empty")
                        }
                        if (playListId == -2) {
                            throw Throwable("${it.key} already exist")
                        }
                    }
                    PlayListUtil.addMultiSongs(it.value, it.key)
                }
                .compose(RxUtil.applyScheduler())
                .doFinally {
                    ToastUtil.show(App.getContext(), R.string.import_count, count.toString())
                }
                .subscribe({
                    count += it
                }, {
                    ToastUtil.show(App.getContext(), R.string.import_fail, it.toString())
                })
    }

    @JvmStatic
    fun exportPlayListToFile(playlistName: String, file: File): Disposable? {
        return Single.fromCallable {
            getMP3ListByIds(getIDList(playlistName), getPlayListID(playlistName))
        }.flatMapCompletable { songs ->
            CompletableSource { source ->
                val bw = BufferedWriter(FileWriter(file))
                bw.write(HEADER)
                for (song in songs) {
                    bw.newLine()
                    bw.write(ENTRY + song.duration + DURATION_SEPARATOR + song.artist + " - " + song.title)
                    bw.newLine()
                    bw.write(song.url)
                }
                bw.close()
                source.onComplete()
            }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
            ToastUtil.show(App.getContext(), R.string.export_success)
        }, {
            ToastUtil.show(App.getContext(), R.string.export_fail, it.toString())
        })
    }
}

