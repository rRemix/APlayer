package remix.myplayer.misc.log

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import timber.log.Timber

class LogTrojanProvider : ContentProvider() {

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
    return 0
  }

  override fun getType(uri: Uri): String? {
    return null
  }

  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    return Uri.EMPTY
  }


  override fun onCreate(): Boolean {
    Timber.plant(LogTree())
    Timber.v("onCreate")
    return true
  }

  override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
    return null
  }

  override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
    return 0
  }
}
