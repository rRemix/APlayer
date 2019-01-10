package remix.myplayer.ui.adapter.holder;

import android.view.View;
import android.widget.ImageView;
import butterknife.BindView;
import remix.myplayer.R;

public class HeaderHolder extends BaseViewHolder {

  //列表显示与网格显示切换
  @BindView(R.id.list_model)
  public ImageView mListModeBtn;
  @BindView(R.id.grid_model)
  public ImageView mGridModeBtn;
  @BindView(R.id.divider)
  public View mDivider;

  public HeaderHolder(View itemView) {
    super(itemView);
  }
}
