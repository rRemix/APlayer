package remix.myplayer.misc.interfaces;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/29 10:02
 */
public interface OnMultiItemClickListener {

  void OnAddToPlayQueue();

  void OnAddToPlayList();

  void OnDelete(boolean deleteSource);
}
