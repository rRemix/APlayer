package remix.myplayer.ui.adapter

import android.content.Context
import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.item_lyric_priority.view.*
import remix.myplayer.bean.misc.LyricPriority
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.util.SPUtil


class LyricPriorityAdapter(context: Context?, layoutId: Int) : BaseAdapter<LyricPriority, LyricPriorityAdapter.LyricPriorityHolder>(layoutId) {

  init {
    val temp: ArrayList<LyricPriority> = Gson().fromJson(SPUtil.getValue(context, SPUtil.LYRIC_KEY.NAME, SPUtil.LYRIC_KEY.PRIORITY_LYRIC, SPUtil.LYRIC_KEY.DEFAULT_PRIORITY),
        object : TypeToken<List<LyricPriority>>() {}.type)

    val all = listOf(
        LyricPriority.EMBEDDED,
        LyricPriority.LOCAL,
        LyricPriority.KUGOU,
        LyricPriority.NETEASE,
        LyricPriority.QQ,
        LyricPriority.IGNORE)
    if (temp.size < all.size) {
      if (!temp.contains(LyricPriority.QQ)) {
        temp.add(2,LyricPriority.QQ)
      }
      if (!temp.contains(LyricPriority.IGNORE)) {
        temp.add(temp.size, LyricPriority.IGNORE)
      }

    }

    setDataList(temp)
  }

  override fun convert(holder: LyricPriorityHolder, lyricPriority: LyricPriority?, position: Int) {
    if(lyricPriority == null){
      return
    }

    holder.view.item_title?.text = lyricPriority.desc
    holder.view.setOnClickListener {

    }
  }

  class LyricPriorityHolder(val view: View) : BaseViewHolder(view)
}
