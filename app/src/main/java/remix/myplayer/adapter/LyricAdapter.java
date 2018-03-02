package remix.myplayer.adapter;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.lyric.bean.LrcRow;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.LogUtil;

/**
 * Created by Remix on 2018/3/1.
 */

public class LyricAdapter extends BaseAdapter<LrcRow,LyricAdapter.LrcViewHolder>{
    private RecyclerView mReclcyerView;
    private LinearLayoutManager mLinearLayoutManager;
    private final int OTHER_COLOR = ColorUtil.getColor(ThemeStore.isDay() ? R.color.lrc_normal_day : R.color.lrc_normal_night);
    private final int HIGHLIGHT_COLOR = ColorUtil.getColor(ThemeStore.isDay() ? R.color.lrc_highlight_day : R.color.lrc_highlight_night);

    public LyricAdapter(Context context, int layoutId, RecyclerView recyclerView) {
        super(context, layoutId);
        mReclcyerView = recyclerView;
        mLinearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
    }

    @Override
    protected void convert(LrcViewHolder holder, LrcRow lrcRow, int position) {
        holder.mText.setText(String.format("%s%s", lrcRow.getContent(), !TextUtils.isEmpty(lrcRow.getTranslate()) ? "\n" + lrcRow.getTranslate() : ""));
        int first = mLinearLayoutManager.findFirstVisibleItemPosition();
        int last = mLinearLayoutManager.findLastVisibleItemPosition();
        LogUtil.d("LyricAdapter","first: " + first + " last: " + last + " position: " + position);
        if(first > 0 && last > 0 && position == (first + last) / 2){
            holder.mText.setTextColor(HIGHLIGHT_COLOR);
            holder.mText.setTextSize(16);
        } else {
            holder.mText.setTextColor(OTHER_COLOR);
            holder.mText.setTextSize(14);
        }

    }

    static class LrcViewHolder extends BaseViewHolder{
        @BindView(R.id.item_content)
        TextView mText;
        @BindView(R.id.item_indicator)
        View mIndicator;

        public LrcViewHolder(View itemView) {
            super(itemView);
        }
    }
}
