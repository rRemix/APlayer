package remix.myplayer.ui.adapter;

import static remix.myplayer.misc.ExtKt.isPortraitOrientation;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import remix.myplayer.R;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.adapter.holder.HeaderHolder;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.SPUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/17 16:36
 */

public abstract class HeaderAdapter<M, B extends RecyclerView.ViewHolder> extends
    BaseAdapter<M, BaseViewHolder> {

  //显示模式 1:列表 2:网格
  public final static int LIST_MODE = 1;
  public final static int GRID_MODE = 2;

  //网格模式下水平和垂直的间距 以间距当作Divider
  private static final int GRID_MARGIN_VERTICAL = DensityUtil.dip2px(4);
  private static final int GRID_MARGIN_HORIZONTAL = DensityUtil.dip2px(6);

  static final int TYPE_HEADER = 0;
  static final int TYPE_NORMAL = 1;

  protected MultipleChoice mChoice;
  protected RecyclerView mRecyclerView;

  //当前列表模式 1:列表 2:网格
  int mMode = GRID_MODE;

  HeaderAdapter(Context context, int layoutId, MultipleChoice multiChoice,
      RecyclerView recyclerView) {
    super(context, layoutId);
    this.mChoice = multiChoice;
    this.mRecyclerView = recyclerView;
    String key = this instanceof AlbumAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ALBUM :
        this instanceof ArtistAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ARTIST :
            this instanceof PlayListAdapter ? SPUtil.SETTING_KEY.MODE_FOR_PLAYLIST :
                null;
    //其他的列表都是List模式
    this.mMode =
        key != null ? SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, key, GRID_MODE) : LIST_MODE;
  }

  @Override
  public int getItemViewType(int position) {
    if (position == 0) {
      return TYPE_HEADER;
    }
    return mMode;
  }

  @Override
  protected M getItem(int position) {
    return mDatas != null ? position == 0 ? null
        : position - 1 < mDatas.size() ? mDatas.get(position - 1) : null : null;
  }

  @Override
  public int getItemCount() {
    return mDatas != null ? super.getItemCount() + 1 : 0;
  }

  @Override
  public void onAttachedToRecyclerView(RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
    if (manager instanceof GridLayoutManager) {
      final GridLayoutManager gridManager = ((GridLayoutManager) manager);
      gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
          return getItemViewType(position) == TYPE_HEADER ? gridManager.getSpanCount() : 1;
        }
      });
    }
  }


  /**
   * 初始化列表模式切换的按钮
   */
  void setUpModeButton(HeaderHolder headerHolder) {
    if (mDatas == null || mDatas.size() == 0) {
      headerHolder.mRoot.setVisibility(View.GONE);
      return;
    }
    //设置图标

    headerHolder.mDivider
        .setVisibility(mMode == HeaderAdapter.LIST_MODE ? View.VISIBLE : View.GONE);
    headerHolder.mGridModeBtn.setOnClickListener(v -> switchMode(headerHolder, v));
    headerHolder.mListModeBtn.setOnClickListener(v -> switchMode(headerHolder, v));
    headerHolder.mDivider.setVisibility(mMode == LIST_MODE ? View.VISIBLE : View.GONE);
    tintModeButton(headerHolder);
  }

  /**
   * 列表模式切换
   */
  void switchMode(HeaderHolder headerHolder, View v) {
    int newModel = v.getId() == R.id.list_model ? LIST_MODE : GRID_MODE;
    if (newModel == mMode) {
      return;
    }
    mMode = newModel;
    setUpModeButton(headerHolder);
    //重新设置LayoutManager和adapter并刷新列表
    mRecyclerView.setLayoutManager(mMode == LIST_MODE ? new LinearLayoutManager(mContext)
        : new GridLayoutManager(mContext, 2));
    mRecyclerView.setAdapter(this);
    //保存当前模式
    saveMode();
  }

  private void tintModeButton(HeaderHolder headerHolder) {
    headerHolder.mListModeBtn.setImageDrawable(
        Theme.tintVectorDrawable(mContext, R.drawable.ic_format_list_bulleted_white_24dp,
            mMode == LIST_MODE ? ThemeStore.getAccentColor()
                : ColorUtil.getColor(R.color.default_model_button_color))
    );

    headerHolder.mGridModeBtn.setImageDrawable(
        Theme.tintVectorDrawable(mContext, R.drawable.ic_apps_white_24dp,
            mMode == GRID_MODE ? ThemeStore.getAccentColor()
                : ColorUtil.getColor(R.color.default_model_button_color))
    );

  }

  private void saveMode() {
    String key = this instanceof AlbumAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ALBUM :
        this instanceof ArtistAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ARTIST :
            SPUtil.SETTING_KEY.MODE_FOR_PLAYLIST;
    SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, key, mMode);
  }

  void setMarginForGridLayout(BaseViewHolder holder, int position) {
    //设置margin,当作Divider
    if (mMode == GRID_MODE && holder.mRoot != null) {
      ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.mRoot
          .getLayoutParams();
      if (isPortraitOrientation(mContext)) { //竖屏
        if (position % 2 == 1) {
          lp.setMargins(GRID_MARGIN_HORIZONTAL, GRID_MARGIN_VERTICAL,
              GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL);
        } else {
          lp.setMargins(GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL,
              GRID_MARGIN_HORIZONTAL, GRID_MARGIN_VERTICAL);
        }
      } else { //横屏
        lp.setMargins(GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL / 2,
            GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL / 2);
      }

      holder.mRoot.setLayoutParams(lp);
    }
  }
}
