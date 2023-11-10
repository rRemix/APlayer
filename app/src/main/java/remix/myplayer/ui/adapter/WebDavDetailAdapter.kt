package remix.myplayer.ui.adapter

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.thegrizzlylabs.sardineandroid.DavResource
import remix.myplayer.R
import remix.myplayer.databinding.ItemWebDavDetailBinding
import remix.myplayer.misc.isAudio
import remix.myplayer.theme.Theme
import timber.log.Timber

class WebDavDetailAdapter : RecyclerView.Adapter<WebDavDetailAdapter.DetailHolder>() {
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

      binding.itemButton.setOnClickListener { v: View? ->
        //TODO
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