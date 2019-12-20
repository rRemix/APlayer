package remix.myplayer.service

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import remix.myplayer.App
import java.io.File
import java.io.FileOutputStream

object LogWriter {
  private val file = File(App.getContext().externalCacheDir, "playQueue.log")

  fun write(log: String) {
    Completable
        .fromAction {
          if (!file.exists()) {
            file.createNewFile()
          }

          val fos = FileOutputStream(file, true)
          fos.write((log + "\n").toByteArray())
          fos.flush()
        }
        .subscribeOn(Schedulers.io())
        .subscribe()
  }
}