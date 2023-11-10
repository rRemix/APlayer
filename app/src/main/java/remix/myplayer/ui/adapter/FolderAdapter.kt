package remix.myplayer.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import remix.myplayer.R
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.databinding.ItemFolderRecycleBinding
import remix.myplayer.misc.menu.LibraryListener
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.isLightTheme
import remix.myplayer.theme.ThemeStore.libraryBtnColor
import remix.myplayer.ui.adapter.FolderAdapter.FolderHolder
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.util.Constants

/**
 * Created by taeja on 16-6-23.
 */
class FolderAdapter(layoutId: Int, private val multiChoice: MultipleChoice<Folder>) : BaseAdapter<Folder, FolderHolder>(layoutId) {

  override fun onBindViewHolder(holder: FolderHolder, position: Int) {
    convert(holder, getItem(position) ?: return, position)
  }

  @SuppressLint("DefaultLocale", "RestrictedApi")
  override fun convert(holder: FolderHolder, folder: Folder?, position: Int) {
    if (folder == null) {
      return
    }
    val context = holder.itemView.context
    //设置文件夹名字 路径名 歌曲数量
    holder.binding.folderName.text = folder.name
    holder.binding.folderPath.text = folder.path
    holder.binding.folderNum.text = String.format("%d首", folder.count)
    //根据主题模式 设置图片
    val tintColor = libraryBtnColor
    Theme.tintDrawable(holder.binding.folderButton, R.drawable.icon_player_more, tintColor)
    holder.binding.folderButton.setOnClickListener { v: View? ->
      val popupMenu = PopupMenu(context, holder.binding.folderButton)
      popupMenu.menuInflater.inflate(R.menu.menu_folder_item, popupMenu.menu)
      popupMenu.setOnMenuItemClickListener(LibraryListener(context,
          folder,
          Constants.FOLDER,
          folder.path))
      popupMenu.gravity = Gravity.END
      popupMenu.show()
    }
    if (onItemClickListener != null) {
      holder.binding.root.setOnClickListener { v: View? -> onItemClickListener?.onItemClick(v, position) }
      holder.binding.root.setOnLongClickListener { v: View? ->
        onItemClickListener?.onItemLongClick(v, position)
        true
      }
    }
    holder.binding.root.isSelected = multiChoice.isPositionCheck(position)
  }

  class FolderHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: ItemFolderRecycleBinding = ItemFolderRecycleBinding.bind(itemView)

  }
}