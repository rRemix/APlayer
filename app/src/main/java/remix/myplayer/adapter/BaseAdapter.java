package remix.myplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;

import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.interfaces.OnItemClickListener;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/19 11:31
 */
public abstract class BaseAdapter<T extends BaseViewHolder> extends RecyclerView.Adapter<T> {
    protected Context mContext;
    protected OnItemClickListener mOnItemClickLitener;
    protected Cursor mCursor;

    public BaseAdapter(Context Context) {
        this.mContext = Context;
    }

    public BaseAdapter(Context context,Cursor cursor){
        this.mContext = context;
        this.mCursor = cursor;
    }

    public void setOnItemClickLitener(OnItemClickListener l) {
        this.mOnItemClickLitener = l;
    }
    public void setCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }
}
