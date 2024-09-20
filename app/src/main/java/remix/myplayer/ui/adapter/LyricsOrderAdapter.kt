package remix.myplayer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import remix.myplayer.R
import remix.myplayer.lyrics.provider.ILyricsProvider

// TODO: implements BaseAdapter?
class LyricsOrderAdapter(val order: List<ILyricsProvider>) :
  RecyclerView.Adapter<LyricsOrderAdapter.ViewHolder>() {
  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title: TextView = view.findViewById(R.id.item_title)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    LayoutInflater.from(parent.context).inflate(R.layout.item_lyrics_order, parent, false).let {
      return ViewHolder(it)
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.title.text = order[position].displayName
  }

  override fun getItemCount() = order.size
}
