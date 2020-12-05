package remix.myplayer.ui.adapter.holder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import butterknife.ButterKnife;

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
    ButterKnife.bind(this, itemView);
    mRoot = itemView;
  }
}
