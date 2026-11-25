package remix.myplayer.ui.screen.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import remix.myplayer.data.remote.RemoteFile

/** A request is a snapshot: subsequent selection changes cannot silently expand a deletion. */
internal data class RemoteDeleteRequest(val files: List<RemoteFile>) {
  val directoryCount: Int get() = files.count { it.isDirectory }
}

/** No remote deletion can run until [confirm] explicitly consumes the pending request. */
internal class RemoteDeleteConfirmation {
  var pending by mutableStateOf<RemoteDeleteRequest?>(null)
    private set

  fun request(files: List<RemoteFile>) {
    if (pending != null) return
    val snapshot = files.distinctBy { it.path }
    if (snapshot.isNotEmpty()) pending = RemoteDeleteRequest(snapshot)
  }

  fun cancel() {
    pending = null
  }

  fun confirm(): List<RemoteFile> {
    val files = pending?.files.orEmpty()
    pending = null
    return files
  }
}
