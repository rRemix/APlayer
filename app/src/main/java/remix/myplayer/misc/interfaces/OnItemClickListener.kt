package remix.myplayer.misc.interfaces

import android.view.View

/**
 * Created by taeja on 16-1-28.
 */
interface OnItemClickListener {
  fun onItemClick(view: View, position: Int)
  fun onItemLongClick(view: View, position: Int)
}