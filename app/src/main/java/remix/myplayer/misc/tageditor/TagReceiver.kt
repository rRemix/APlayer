package remix.myplayer.misc.tageditor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import remix.myplayer.bean.mp3.Song
import remix.myplayer.misc.interfaces.OnTagEditListener

class TagReceiver(private val listener: OnTagEditListener?) : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val newSong = intent.getParcelableExtra<Song>("newSong")
    listener?.onTagEdit(newSong)
  }

  companion object {
    const val ACTION_EDIT_TAG = "remix.music.ACTION_EDIT_TAG"
  }
}
