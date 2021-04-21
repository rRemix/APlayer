package remix.myplayer.ui.adapter

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.github.promeg.pinyinhelper.Pinyin
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ItemSongRecycleBinding
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.helper.SortOrder
import remix.myplayer.misc.menu.SongPopupListener
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.service.Command
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.accentColor
import remix.myplayer.theme.ThemeStore.highLightTextColor
import remix.myplayer.theme.ThemeStore.libraryBtnColor
import remix.myplayer.theme.ThemeStore.textColorPrimary
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScroller
import remix.myplayer.util.Constants
import remix.myplayer.util.ImageUriUtil
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.SETTING_KEY
import remix.myplayer.util.ToastUtil
import java.util.*

/**
 * Created by taeja on 16-6-24.
 */
@SuppressLint("RestrictedApi")
open class ChildHolderAdapter(layoutId: Int, private val type: Int, private val arg: String, multiChoice: MultipleChoice<Song>, recyclerView: RecyclerView)
  : HeaderAdapter<Song, BaseViewHolder>(layoutId, multiChoice, recyclerView), FastScroller.SectionIndexer {

  private var lastPlaySong = getCurrentSong()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return if (viewType == TYPE_HEADER) SongAdapter.HeaderHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_header_1, parent, false)) else ChildHolderViewHolder(
        ItemSongRecycleBinding.inflate(LayoutInflater.from(parent.context), parent,
            false))
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    super.onViewRecycled(holder)
    disposeLoad(holder)
  }

  override fun convert(holder: BaseViewHolder, data: Song?, position: Int) {
    val context = holder.itemView.context
    if (position == 0) {
      val headerHolder = holder as SongAdapter.HeaderHolder
      //没有歌曲时隐藏
      if (dataList.size == 0) {
        headerHolder.binding.root.visibility = View.GONE
        return
      }
      headerHolder.binding.root.visibility = View.VISIBLE
      headerHolder.binding.playShuffleButton.setImageDrawable(
          Theme.tintVectorDrawable(context, R.drawable.ic_shuffle_white_24dp,
              accentColor)
      )
      headerHolder.binding.tvShuffleCount.text = context.getString(R.string.play_random, itemCount - 1)

      //显示当前排序方式
      headerHolder.binding.root.setOnClickListener { v: View? ->
        //设置正在播放列表
        if (dataList.isEmpty()) {
          ToastUtil.show(context, R.string.no_song)
          return@setOnClickListener
        }
        setPlayQueue(dataList, MusicUtil.makeCmdIntent(Command.NEXT, true))
      }
      return
    }

    if (data == null) {
      return
    }
    val holder = holder as ChildHolderViewHolder
    if (data.id < 0 || (data.title == context.getString(R.string.song_lose_effect))) {
      holder.binding.songTitle.setText(R.string.song_lose_effect)
      holder.binding.songButton.visibility = View.INVISIBLE
    } else {
      holder.binding.songButton.visibility = View.VISIBLE

      //封面
      holder.binding.songHeadImage.tag = setImage(holder.binding.songHeadImage, ImageUriUtil.getSearchRequestWithAlbumType(data), ImageUriRequest.SMALL_IMAGE_SIZE, position)

      //高亮
      if (getCurrentSong().id == data.id) {
        lastPlaySong = data
        holder.binding.songTitle.setTextColor(highLightTextColor)
        holder.binding.indicator.visibility = View.VISIBLE
      } else {
        holder.binding.songTitle.setTextColor(textColorPrimary)
        holder.binding.indicator.visibility = View.GONE
      }
      holder.binding.indicator.setBackgroundColor(highLightTextColor)

      //设置标题
      holder.binding.songTitle.text = data.showName

      //艺术家与专辑
      holder.binding.songOther.text = String.format("%s-%s", data.artist, data.album)
      //设置按钮着色
      val tintColor = libraryBtnColor
      Theme.tintDrawable(holder.binding.songButton, R.drawable.icon_player_more, tintColor)
      holder.binding.songButton.setOnClickListener { v: View? ->
        if (choice.isActive) {
          return@setOnClickListener
        }
        val popupMenu = PopupMenu(context, holder.binding.songButton, Gravity.END)
        popupMenu.menuInflater.inflate(R.menu.menu_song_item, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(
            SongPopupListener((context as AppCompatActivity), data, type == Constants.PLAYLIST,
                arg))
        popupMenu.show()
      }
    }
    if (onItemClickListener != null) {
      holder.binding.root.setOnClickListener { v: View ->
        if (holder.adapterPosition - 1 < 0) {
          ToastUtil.show(context, R.string.illegal_arg)
          return@setOnClickListener
        }
        if (data.id > 0) {
          onItemClickListener?.onItemClick(v, holder.adapterPosition - 1)
        }
      }
      holder.binding.root.setOnLongClickListener { v: View ->
        if (holder.adapterPosition - 1 < 0) {
          ToastUtil.show(context, R.string.illegal_arg)
          return@setOnLongClickListener true
        }
        onItemClickListener?.onItemLongClick(v, holder.adapterPosition - 1)
        true
      }
    }
    holder.binding.root.isSelected = choice.isPositionCheck(position - 1)
  }

  override fun getSectionText(position: Int): String {
    if (position in 1..dataList.size) {
      val settingKey = when (type) {
        Constants.ALBUM -> SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER
        Constants.ARTIST -> SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER
        Constants.FOLDER -> SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER
        Constants.PLAYLIST -> SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER
        else -> null
      }
      if (settingKey != null) {
        val data = dataList[position - 1]
        val key = when (SPUtil.getValue(
          App.context,
          SETTING_KEY.NAME,
          settingKey,
          SortOrder.SONG_A_Z
        )) {
          SortOrder.SONG_A_Z, SortOrder.SONG_Z_A -> data.title
          SortOrder.ARTIST_A_Z, SortOrder.ARTIST_Z_A -> data.artist
          SortOrder.ALBUM_A_Z, SortOrder.ALBUM_Z_A -> data.album
          SortOrder.DISPLAY_NAME_A_Z, SortOrder.DISPLAY_NAME_Z_A -> data.displayName
          else -> ""
        }
        if (key.isNotEmpty())
          return Pinyin.toPinyin(key[0]).toUpperCase(Locale.getDefault()).substring(0, 1)
      }
    }
    return ""
  }

  fun updatePlayingSong() {
    val currentSong = getCurrentSong()
    if (currentSong.id == -1 || currentSong.id == lastPlaySong.id) {
      return
    }
    if (dataList.contains(currentSong)) {
      // 找到新的高亮歌曲
      val index = dataList.indexOf(currentSong) + 1
      val lastIndex = dataList.indexOf(lastPlaySong) + 1
      var newHolder: ChildHolderViewHolder? = null
      if (recyclerView.findViewHolderForAdapterPosition(index) is ChildHolderViewHolder) {
        newHolder = recyclerView.findViewHolderForAdapterPosition(index) as ChildHolderViewHolder?
      }
      var oldHolder: ChildHolderViewHolder? = null
      if (recyclerView
              .findViewHolderForAdapterPosition(lastIndex) is ChildHolderViewHolder) {
        oldHolder = recyclerView
            .findViewHolderForAdapterPosition(lastIndex) as ChildHolderViewHolder?
      }
      if (newHolder != null) {
        newHolder.binding.songTitle.setTextColor(highLightTextColor)
        newHolder.binding.indicator.visibility = View.VISIBLE
      }
      if (oldHolder != null) {
        oldHolder.binding.songTitle.setTextColor(textColorPrimary)
        oldHolder.binding.indicator.visibility = View.GONE
      }
      lastPlaySong = currentSong
    }
  }

  internal class ChildHolderViewHolder(val binding: ItemSongRecycleBinding) : BaseViewHolder(binding.root)
}