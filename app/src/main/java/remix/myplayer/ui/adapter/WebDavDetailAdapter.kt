package remix.myplayer.ui.adapter

import android.graphics.PorterDuff
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.promeg.pinyinhelper.Pinyin
import com.thegrizzlylabs.sardineandroid.DavResource
import remix.myplayer.R
import remix.myplayer.databinding.ItemWebDavDetailBinding
import remix.myplayer.db.room.model.WebDav
import remix.myplayer.misc.isAudio
import remix.myplayer.misc.menu.WebDavItemPopupListener
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.base.BaseActivity
import timber.log.Timber

class WebDavDetailAdapter(private val webDav: WebDav) :
  RecyclerView.Adapter<WebDavDetailAdapter.DetailHolder>() {
  var onItemClickListener: OnItemClickListener<DavResource>? = null

  private val davResources = ArrayList<DavResource>()

  fun getWebDavResources() = ArrayList(davResources)

  fun setWebDavResources(newList: List<DavResource>) {
    val oldList = ArrayList(davResources)
    davResources.clear()
    davResources.addAll(newList)

    DiffUtil.calculateDiff(object : DiffUtil.Callback() {
      override fun getOldListSize(): Int {
        return oldList.size
      }

      override fun getNewListSize(): Int {
        return davResources.size
      }

      override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].path == newList[newItemPosition].path
      }

      override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return false
      }

    }).dispatchUpdatesTo(this)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailHolder {
    return DetailHolder(
      ItemWebDavDetailBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
    )
  }

  override fun getItemCount(): Int {
    return davResources.size
  }

  override fun onBindViewHolder(holder: DetailHolder, position: Int) {
    holder.bind(position)
  }

  inner class DetailHolder(private val binding: ItemWebDavDetailBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(pos: Int) {
      val davResource = davResources[pos]

      Timber.v("path: ${davResource.path} contentType: ${davResource.contentType}}")
      binding.tvName.text = davResource.name
      binding.tvPath.text = davResource.path

      val isAudio = davResource.isAudio()
      binding.ivFolder.setColorFilter(
        Theme.resolveColor(itemView.context, R.attr.icon_color),
        PorterDuff.Mode.SRC_IN
      )
      if (davResource.isDirectory) {
        binding.ivFolder.setImageResource(R.drawable.icon_folder)
      } else if (isAudio) {
        binding.ivFolder.setImageResource(R.drawable.icon_music)
      } else {
        binding.ivFolder.setImageResource(R.drawable.icon_file)
      }

      binding.itemButton.setColorFilter(ThemeStore.libraryBtnColor, PorterDuff.Mode.SRC_IN)
      binding.itemButton.setOnClickListener { v: View? ->
        val popupMenu = PopupMenu(itemView.context, binding.itemButton)
        popupMenu.menuInflater.inflate(
          if (davResource.isAudio()) R.menu.menu_webdav_item_audio else R.menu.menu_webdav_item_other,
          popupMenu.menu
        )
        popupMenu.setOnMenuItemClickListener(
          WebDavItemPopupListener(
            itemView.context as BaseActivity,
            webDav,
            davResource,
            object : WebDavItemPopupListener.Callback {
              override fun onDavResourceRemove(removed: DavResource) {
                davResources.forEachIndexed { index, davResource ->
                  if (davResource.path == removed.path) {
                    notifyItemRemoved(index)
                    davResources.removeAt(index)
                    return
                  }
                }
              }
            })
        )
        popupMenu.gravity = Gravity.END
        popupMenu.show()
      }

      binding.root.setOnClickListener {
        if (isAudio || davResource.isDirectory) {
          onItemClickListener?.onItemClick(binding.root, davResource, pos)
        }
      }
    }
  }
}

interface OnItemClickListener<T> {
  fun onItemClick(view: View, data: T, position: Int)
  fun onItemLongClick(view: View, data: T, position: Int)
}