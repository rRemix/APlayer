package remix.myplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.interfaces.ModeChangeCallback;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/17 16:36
 */

public abstract class HeaderAdapter extends BaseAdapter<BaseViewHolder> {
    static final int TYPE_HEADER = 0;
    static final int TYPE_NORMAL = 1;
    protected MultiChoice mMultiChoice;
    ModeChangeCallback mModeChangeCallback;
    //当前列表模式 1:列表 2:网格
    int ListModel = 2;

    HeaderAdapter(Context context, Cursor cursor, MultiChoice multiChoice) {
        super(context,cursor);
        this.mMultiChoice = multiChoice;
    }

    public void setModeChangeCallback(ModeChangeCallback modeChangeCallback){
        mModeChangeCallback = modeChangeCallback;
    }

    public abstract BaseViewHolder onCreateHolder(ViewGroup parent, final int viewType);
    public abstract void onBind(BaseViewHolder viewHolder, int position);

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return onCreateHolder(parent,viewType);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        onBind(holder,position);
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0)
            return TYPE_HEADER;
        return ListModel;
    }

    @Override
    public int getItemCount() {
        return mCursor != null && mCursor.getCount() > 0 ? super.getItemCount() + 1 : 0;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if(manager instanceof GridLayoutManager) {
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
     * @param headerHolder
     * @param v
     */
    void switchMode(AlbumAdater.HeaderHolder headerHolder, View v){
        int newModel = v.getId() == R.id.list_model ? Constants.LIST_MODEL : Constants.GRID_MODEL;
        if(newModel == ListModel)
            return;
        ListModel = newModel;
        headerHolder.mListModelBtn.setColorFilter(ListModel == Constants.LIST_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
        headerHolder.mGridModelBtn.setColorFilter(ListModel == Constants.GRID_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
        headerHolder.mDivider.setVisibility(ListModel == Constants.LIST_MODEL ? View.VISIBLE : View.GONE);
        saveMode();
        if(mModeChangeCallback != null){
            mModeChangeCallback.OnModeChange(ListModel);
        }
    }

    protected void saveMode(){}
}
