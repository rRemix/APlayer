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
import remix.myplayer.databinding.LayoutHeader1Binding
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
import remix.myplayer.util.ImageUriUtil
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil
import java.util.*

/**
 * 全部歌曲和最近添加页面所用adapter
 */
/**
 * Created by Remix on 2016/4/11.
 */
class SongAdapter(layoutId: Int, multiChoice: MultipleChoice<Song>, recyclerView: RecyclerView)
  : HeaderAdapter<Song, BaseViewHolder>(layoutId, multiChoice, recyclerView), FastScroller.SectionIndexer {

  private var lastPlaySong = getCurrentSong()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return if (viewType == TYPE_HEADER) HeaderHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_header_1, parent, false))
    else SongViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_song_recycle, parent, false))
  }

  override fun onViewRecycled(holder: BaseViewHolder) {
    super.onViewRecycled(holder)
    disposeLoad(holder)
  }

  @SuppressLint("RestrictedApi")
  override fun convert(holder: BaseViewHolder, song: Song?, position: Int) {
    val context = holder.itemView.context
    if (position == 0) {
      val headerHolder = holder as HeaderHolder
      //没有歌曲时隐藏
      if (dataList.size == 0) {
        headerHolder.binding.root.visibility = View.GONE
        return
      } else {
        headerHolder.binding.root.visibility = View.VISIBLE
      }
      headerHolder.binding.playShuffleButton.setImageDrawable(
          Theme.tintVectorDrawable(context, R.drawable.ic_shuffle_white_24dp,
              accentColor)
      )
      headerHolder.binding.tvShuffleCount.text = context.getString(R.string.play_random, itemCount - 1)
      headerHolder.binding.root.setOnClickListener { v: View? ->
        val intent = MusicUtil.makeCmdIntent(Command.NEXT, true)
        if (dataList.isEmpty()) {
          ToastUtil.show(context, R.string.no_song)
          return@setOnClickListener
        }
        setPlayQueue(dataList, intent)
      }
      return
    }
    if (holder !is SongViewHolder || song == null) {
      return
    }

    //封面
    holder.binding.songHeadImage.tag = setImage(holder.binding.songHeadImage, ImageUriUtil.getSearchRequestWithAlbumType(song), ImageUriRequest.SMALL_IMAGE_SIZE, position)

//        //是否为无损
//        if(!TextUtils.isEmpty(song.getDisplayName())){
//            String prefix = song.getDisplayName().substring(song.getDisplayName().lastIndexOf(".") + 1);
//            holder.mSQ.setVisibility(prefix.equals("flac") || prefix.equals("ape") || prefix.equals("wav")? View.VISIBLE : View.GONE);
//        }

    //高亮
    if (getCurrentSong().id == song.id) {
      lastPlaySong = song
      holder.binding.songTitle.setTextColor(highLightTextColor)
      holder.binding.indicator.visibility = View.VISIBLE
    } else {
      holder.binding.songTitle.setTextColor(textColorPrimary)
      holder.binding.indicator.visibility = View.GONE
    }
    holder.binding.indicator.setBackgroundColor(highLightTextColor)

    //标题
    holder.binding.songTitle.text = song.showName

    //艺术家与专辑
    holder.binding.songOther.text = String.format("%s-%s", song.artist, song.album)

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
          SongPopupListener((context as AppCompatActivity), song, false, ""))
      popupMenu.show()
    }
    holder.binding.itemRoot.setOnClickListener { v: View? ->
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg)
        return@setOnClickListener
      }
      onItemClickListener?.onItemClick(v, position - 1)
    }
    holder.binding.itemRoot.setOnLongClickListener { v: View? ->
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg)
        return@setOnLongClickListener true
      }
      onItemClickListener?.onItemLongClick(v, position - 1)
      true
    }
    holder.binding.itemRoot.isSelected = choice.isPositionCheck(position - 1)
  }

  override fun getSectionText(position: Int): String {
    if (position in 1..dataList.size) {
      val data = dataList[position - 1]
      val key = when (SPUtil.getValue(
        App.getContext(),
        SPUtil.SETTING_KEY.NAME,
        SPUtil.SETTING_KEY.SONG_SORT_ORDER,
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
    return ""
  }

  /**
   * 更新高亮歌曲
   */
  fun updatePlayingSong() {
    val currentSong = getCurrentSong()
    if (currentSong.id == -1 || currentSong.id == lastPlaySong.id) {
      return
    }
    if (dataList.contains(currentSong)) {
      // 找到新的高亮歌曲
      val index = dataList.indexOf(currentSong) + 1
      val lastIndex = dataList.indexOf(lastPlaySong) + 1
      var newHolder: SongViewHolder? = null
      if (recyclerView.findViewHolderForAdapterPosition(index) is SongViewHolder) {
        newHolder = recyclerView.findViewHolderForAdapterPosition(index) as SongViewHolder?
      }
      var oldHolder: SongViewHolder? = null
      if (recyclerView.findViewHolderForAdapterPosition(lastIndex) is SongViewHolder) {
        oldHolder = recyclerView.findViewHolderForAdapterPosition(lastIndex) as SongViewHolder?
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

  internal class SongViewHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: ItemSongRecycleBinding = ItemSongRecycleBinding.bind(itemView)
  }

  internal class HeaderHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: LayoutHeader1Binding = LayoutHeader1Binding.bind(itemView)

  }

}