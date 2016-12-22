package remix.myplayer.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.interfaces.OnInflateFinishListener;
import remix.myplayer.lyric.LrcRow;
import remix.myplayer.lyric.LrcView;
import remix.myplayer.lyric.SearchLRC;
import remix.myplayer.model.MP3Item;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 歌词界面Fragment
 */
public class LrcFragment extends BaseFragment {
    //是否找到歌词的几种状态
    private static int UPDATE_LRC = 1;
    private static int NO_LRC = 2;
    private static int NO_NETWORK = 3;
    private static int SEARCHING = 4;
    private OnInflateFinishListener mOnFindListener;
    private MP3Item mInfo;
    @BindView(R.id.lrc_view)
    LrcView mLrcView;
    //歌词列表
    private List<LrcRow> mLrcList;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(mLrcView == null)
                return;
            //是否正在搜索
            if(msg.what == SEARCHING){
                mLrcView.setText("正在搜索");

            } else if(msg.what == UPDATE_LRC) {
                //更新歌词
                mLrcView.setLrcRows(mLrcList);
            } else if (msg.what == NO_LRC) {
                //没有找到歌词
                mLrcView.setText("暂无歌词");
            } else if (msg.what == NO_NETWORK) {
                //没用网络
                mLrcView.setText("请检查网络连接");
            }
        }
    };

    public void setOnInflateFinishListener(OnInflateFinishListener l){
        mOnFindListener = l;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = LrcFragment.class.getSimpleName();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lrc,container,false);

        mUnBinder = ButterKnife.bind(this,rootView);
        if(mOnFindListener != null)
            mOnFindListener.onViewInflateFinish(mLrcView);
        mInfo = (MP3Item)getArguments().getSerializable("MP3Item");

        return rootView;
    }

    public void UpdateLrc(MP3Item mp3Item) {
        if(mp3Item == null)
            return;
        mInfo = mp3Item;

        new DownloadThread().start();
    }


    class DownloadThread extends Thread {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(SEARCHING);
            SearchLRC searchLRC = new SearchLRC(mInfo);
            List<LrcRow> temp = searchLRC.getLrc();
            if(searchLRC.getSongID() == mInfo.getId()){
                mLrcList = temp;
                if(mLrcList == null || mLrcList.size() == 0) {
                    mHandler.sendEmptyMessage(NO_LRC);
                    return;
                }
                mHandler.sendEmptyMessage(UPDATE_LRC);
            }

        }
    }

}
