package remix.myplayer.ui.adapter.holder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/7/27 14:56
 */
public class BaseViewHolder extends RecyclerView.ViewHolder {

  public View mRoot;

  public BaseViewHolder(View itemView) {
    super(itemView);
    mRoot = itemView;
  }
}
