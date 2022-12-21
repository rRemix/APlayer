package remix.myplayer.lyric

import remix.myplayer.lyric.bean.LrcRow

interface ILrcView {
  /**
   * 初始化画笔，颜色，字体大小等设置
   */
  fun init()

  /***
   * 设置数据源
   * @param lrcRows
   */
  fun setLrcRows(lrcRows: List<LrcRow>?)

  /**
   * 指定时间
   *
   * @param progress 时间进度
   * @param fromSeekBarByUser 是否由用户触摸Seekbar触发
   */
  fun seekTo(progress: Int, fromSeekBar: Boolean, fromSeekBarByUser: Boolean)

  /***
   * 设置歌词文字的缩放比例
   * @param newFactor
   */
  fun setLrcScalingFactor(newFactor: Float)

  /**
   * 重置
   */
  fun reset()
}