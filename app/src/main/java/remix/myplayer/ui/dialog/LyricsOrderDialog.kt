package remix.myplayer.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import remix.myplayer.R
import remix.myplayer.lyrics.LyricsSearcher
import remix.myplayer.theme.Theme
import remix.myplayer.ui.adapter.LyricsOrderAdapter
import remix.myplayer.ui.dialog.base.BaseDialog
import java.util.Collections

class LyricsOrderDialog : BaseDialog() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val context = requireContext()
    val recyclerView = RecyclerView(context)
    val adapter = LyricsOrderAdapter(LyricsSearcher.order)
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = adapter

    ItemTouchHelper(object :
      ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
      ): Boolean {
        val p1 = viewHolder.adapterPosition.takeIf { it in adapter.order.indices } ?: return false
        val p2 = target.adapterPosition.takeIf { it in adapter.order.indices } ?: return false
        Collections.swap(adapter.order, p1, p2)
        adapter.notifyItemMoved(p1, p2)
        return true
      }

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }).attachToRecyclerView(recyclerView)

    return Theme.getBaseDialog(context)
        .title(R.string.lrc_priority)
        .customView(recyclerView, false)
        .positiveText(R.string.confirm)
        .negativeText(R.string.cancel)
        .onPositive { _, _ -> LyricsSearcher.order = adapter.order }
        .onNegative { _, _ -> dismiss() }
        .build()
  }
}