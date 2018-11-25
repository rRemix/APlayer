package remix.myplayer.ui.widget.fastcroll_recyclerview

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote

class LocationRecyclerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FastScrollRecyclerView(context, attrs, defStyleAttr) {
    private var mMove = false
    private var mPos = -1

    init {
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                //在这里进行第二次滚动（最后的100米！）
                if (mMove) {
                    mMove = false
                    //获取要置顶的项在当前屏幕的位置，mIndex是记录的要置顶项在RecyclerView中的位置
                    val n = mPos - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (n in 0..(childCount - 1)) {
                        //获取要置顶的项顶部离RecyclerView顶部的距离
                        val top = getChildAt(n).getTop()
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
                mPos = i
                break
            }
        }
        val layoutManager = layoutManager as LinearLayoutManager
        val firstItem = layoutManager.findFirstVisibleItemPosition()
        val lastItem = layoutManager.findLastVisibleItemPosition()
        //然后区分情况
        if (mPos <= firstItem) {
            //当要置顶的项在当前显示的第一个项的前面时
            scrollToPosition(mPos)
        } else if (mPos <= lastItem) {
            //当要置顶的项已经在屏幕上显示时
            val top = getChildAt(mPos - firstItem).getTop()
            scrollBy(0, top)
        } else {
            //当要置顶的项在当前显示的最后一项的后面时
            scrollToPosition(mPos)
            //这里这个变量是用在RecyclerView滚动监听里面的
            mMove = true
        }
        if (mPos >= 0) {
            getLayoutManager().scrollToPosition(mPos)
        }
    }
}