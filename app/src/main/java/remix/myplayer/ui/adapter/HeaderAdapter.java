package remix.myplayer.ui.adapter;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import remix.myplayer.R;
import remix.myplayer.misc.interfaces.ModeChangeCallback;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/17 16:36
 */

public abstract class HeaderAdapter<M, B extends RecyclerView.ViewHolder> extends BaseAdapter<M, BaseViewHolder> {

    static final int TYPE_HEADER = 0;
    static final int TYPE_NORMAL = 1;
    protected MultiChoice mMultiChoice;
    private ModeChangeCallback mModeChangeCallback;
    //当前列表模式 1:列表 2:网格
    int ListModel = 2;

    HeaderAdapter(Context context, int layoutId, MultiChoice multiChoice) {
        super(context, layoutId);
        this.mMultiChoice = multiChoice;
    }

    public void setModeChangeCallback(ModeChangeCallback modeChangeCallback) {
        mModeChangeCallback = modeChangeCallback;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_HEADER;
        return ListModel;
    }

    @Override
    protected M getItem(int position) {
        return mDatas != null ? position == 0 ? null : position - 1 < mDatas.size() ? mDatas.get(position - 1) : null : null;
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
     * 列表模式切换
     *
     * @param headerHolder
     * @param v
     */
    void switchMode(AlbumAdapter.HeaderHolder headerHolder, View v) {
        int newModel = v.getId() == R.id.list_model ? Constants.LIST_MODEL : Constants.GRID_MODEL;
        if (newModel == ListModel)
            return;
        ListModel = newModel;
        headerHolder.mListModelBtn.setColorFilter(ListModel == Constants.LIST_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
        headerHolder.mGridModelBtn.setColorFilter(ListModel == Constants.GRID_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
        headerHolder.mDivider.setVisibility(ListModel == Constants.LIST_MODEL ? View.VISIBLE : View.GONE);
        saveMode();
        if (mModeChangeCallback != null) {
            mModeChangeCallback.OnModeChange(ListModel);
        }
    }

    protected void saveMode() {
    }
}
