package remix.myplayer.ui.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import remix.myplayer.R
import remix.myplayer.databinding.ItemWebDavBinding
import remix.myplayer.db.room.model.WebDav
import remix.myplayer.misc.menu.WebDavPopupListener
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.WebDavActivity
import remix.myplayer.ui.activity.WebDavDetailActivity

class WebDavAdapter : RecyclerView.Adapter<WebDavAdapter.WebDavHolder>() {
  private val webDavList = ArrayList<WebDav>()

  fun setWebDavList(newList: List<WebDav>) {
    val oldList = ArrayList(webDavList)
    webDavList.clear()
    webDavList.addAll(newList)

    DiffUtil.calculateDiff(object : DiffUtil.Callback() {
      override fun getOldListSize(): Int {
        return oldList.size
      }

      override fun getNewListSize(): Int {
        return webDavList.size
      }

      override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
      }

      override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return false
      }

    }).dispatchUpdatesTo(this)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebDavHolder {
    return WebDavHolder(ItemWebDavBinding.inflate(LayoutInflater.from(parent.context), parent, false))
  }

  override fun getItemCount(): Int {
    return webDavList.size
  }

  override fun onBindViewHolder(holder: WebDavHolder, position: Int) {
    holder.bind(webDavList[position])
  }

  class WebDavHolder(private val binding: ItemWebDavBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(webDav: WebDav) {
      binding.tvAlisa.text = webDav.alias
      binding.tvServer.text = webDav.server

      val tintColor = ThemeStore.libraryBtnColor
      Theme.tintDrawable(binding.itemButton, R.drawable.icon_player_more, tintColor)
      binding.itemButton.setOnClickListener { v: View? ->
        val context = itemView.context
        val popupMenu = PopupMenu(context, binding.itemButton)
        popupMenu.menuInflater.inflate(R.menu.menu_webdav, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(WebDavPopupListener(context as WebDavActivity, webDav))
        popupMenu.gravity = Gravity.END
        popupMenu.show()
      }
      itemView.setOnClickListener{
        WebDavDetailActivity.start(itemView.context, webDav)
      }
    }
  }
}