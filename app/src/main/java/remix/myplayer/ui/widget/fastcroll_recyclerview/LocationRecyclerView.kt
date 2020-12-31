package remix.myplayer.ui.widget.fastcroll_recyclerview

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.ui.activity.MainActivity

class LocationRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FastScrollRecyclerView(context, attrs, defStyleAttr) {
  private var move = false
  private var pos = -1

  init {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        //在这里进行第二次滚动（最后的100米！）
        if (move) {
          move = false
          //获取要置顶的项在当前屏幕的位置，mIndex是记录的要置顶项在RecyclerView中的位置
          val n = pos - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
          if (n in 0 until childCount) {
            //获取要置顶的项顶部离RecyclerView顶部的距离
            val top = getChildAt(n).top
            //最后的移动
            scrollBy(0, top)
          }
        }
      }
    })
  }


  fun smoothScrollToCurrentSong(data: List<Song>) {
    val currentId = MusicServiceRemote.getCurrentSong().id
    if (currentId < 0)
      return
    smoothScrollTo(data, currentId)
  }

  /**
   * 滚动到指定位置
   *
   * @param data
   */
  private fun smoothScrollTo(data: List<Song>, currentId: Int) {
    for (i in data.indices) {
      if (data[i].id == currentId) {
        pos = i
        break
      }
    }
    // 第一个holder是随机播放 忽略
    if(context is MainActivity){
      pos += 1
    }
    val layoutManager = layoutManager as LinearLayoutManager
    val firstItem = layoutManager.findFirstVisibleItemPosition()
    val lastItem = layoutManager.findLastVisibleItemPosition()
    //然后区分情况
    when {
      pos <= firstItem -> //当要置顶的项在当前显示的第一个项的前面时
        scrollToPosition(pos)
      pos <= lastItem -> {
        //当要置顶的项已经在屏幕上显示时
        val top = getChildAt(pos - firstItem).top
        scrollBy(0, top)
      }
      else -> {
        //当要置顶的项在当前显示的最后一项的后面时
        scrollToPosition(pos)
        //这里这个变量是用在RecyclerView滚动监听里面的
        move = true
      }
    }
    if (pos >= 0) {
      getLayoutManager()?.scrollToPosition(pos)
    }
  }
}