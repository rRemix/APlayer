package remix.myplayer.ui.adapter;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import remix.myplayer.R;
import remix.myplayer.ui.MultipleChoice;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.SPUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/17 16:36
 */

public abstract class HeaderAdapter<M, B extends RecyclerView.ViewHolder> extends BaseAdapter<M, BaseViewHolder> {
    //显示模式 1:列表 2:网格
    public final static int LIST_MODE = 1;
    public final static int GRID_MODE = 2;

    //网格模式下水平和垂直的间距 以间距当作Divider
    static final int GRID_MARGIN_VERTICAL = DensityUtil.dip2px(4);
    static final int GRID_MARGIN_HORIZONTAL = DensityUtil.dip2px(6);

    static final int TYPE_HEADER = 0;
    static final int TYPE_NORMAL = 1;
    protected MultipleChoice mChoice;
    protected RecyclerView mRecyclerView;

    //当前列表模式 1:列表 2:网格
    int mMode = GRID_MODE;

    HeaderAdapter(Context context, int layoutId, MultipleChoice multiChoice, RecyclerView recyclerView) {
        super(context, layoutId);
        this.mChoice = multiChoice;
        this.mRecyclerView = recyclerView;
        String key = this instanceof AlbumAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ALBUM :
                this instanceof ArtistAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ARTIST :
                        this instanceof PlayListAdapter ? SPUtil.SETTING_KEY.MODE_FOR_PLAYLIST :
                                null;
        //其他的列表都是List模式
        this.mMode = key != null ? SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, key, GRID_MODE) : LIST_MODE;

    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_HEADER;
        return mMode;
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
                    int size = getItemViewType(position) == TYPE_HEADER ? gridManager.getSpanCount() : 1;
//                    if(this instanceof AlbumAdapter){
//
//                    }
                    return size;
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
        int newModel = v.getId() == R.id.list_model ? LIST_MODE : GRID_MODE;
        if (newModel == mMode)
            return;
        mMode = newModel;
        //列表模式下隐藏
        headerHolder.mListModelBtn.setColorFilter(mMode == LIST_MODE ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
        headerHolder.mGridModelBtn.setColorFilter(mMode == GRID_MODE ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
        headerHolder.mDivider.setVisibility(mMode == LIST_MODE ? View.VISIBLE : View.GONE);
        //重新设置LayoutManager和adapter并刷新列表
        mRecyclerView.setLayoutManager(mMode == HeaderAdapter.LIST_MODE ? new LinearLayoutManager(mContext) : new GridLayoutManager(mContext, 2));
        mRecyclerView.setAdapter(this);
//        notifyDataSetChanged();
        //保存当前模式
        saveMode();
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
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.mRoot.getLayoutParams();
            if (position % 2 == 1) {
                lp.setMargins(GRID_MARGIN_HORIZONTAL, GRID_MARGIN_VERTICAL,
                        GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL);
            } else {
                lp.setMargins(GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL,
                        GRID_MARGIN_HORIZONTAL, GRID_MARGIN_VERTICAL);
            }
            holder.mRoot.setLayoutParams(lp);
        }
    }
}
