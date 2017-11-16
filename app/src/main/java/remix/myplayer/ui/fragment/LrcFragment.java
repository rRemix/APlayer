package remix.myplayer.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.interfaces.OnInflateFinishListener;
import remix.myplayer.lyric.LrcRow;
import remix.myplayer.lyric.LrcView;
import remix.myplayer.lyric.SearchLRC;
import remix.myplayer.model.mp3.Song;

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
    private Song mInfo;
    @BindView(R.id.lrc_view)
    LrcView mLrcView;
    //歌词
    private List<LrcRow> mLrcList;
    private LrcHandler mHandler;

    public void setOnInflateFinishListener(OnInflateFinishListener l){
        mOnFindListener = l;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
        if(mHandler == null)
            mHandler = new LrcHandler(Looper.getMainLooper(),this);
        if(mOnFindListener != null)
            mOnFindListener.onViewInflateFinish(mLrcView);
        mInfo = (Song)getArguments().getSerializable("Song");
        if(mLrcList != null && mLrcList.size() > 0){
            mHandler.sendEmptyMessage(UPDATE_LRC);
        }

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHandler.removeCallbacksAndMessages(null);
    }

    public void updateLrc(Song song) {
        if(song == null)
            return;
        if(mHandler == null)
            mHandler = new LrcHandler(Looper.getMainLooper(),this);
        mInfo = song;

        new DownloadThread().start();
    }

    public void updateLrc(String lrcPath){
        new DownloadThread(lrcPath).start();
    }

    private void handleInternal(int what){
        if(mLrcView == null)
            return;
        //是否正在搜索
        if(what == SEARCHING){
            mLrcView.setText(getString(R.string.searching));

        } else if(what == UPDATE_LRC) {
            //更新歌词
            mLrcView.setLrcRows(mLrcList);
        } else if (what == NO_LRC) {
            //没有找到歌词
            mLrcView.setText(getString(R.string.no_lrc));
        } else if (what == NO_NETWORK) {
            //没用网络
            mLrcView.setText(getString(R.string.check_network));
        }
    }


    private class DownloadThread extends Thread {
        String mManualPath;
        DownloadThread(String manualPath){
            mManualPath = manualPath;
        }
        DownloadThread(){}
        @Override
        public void run() {
            mHandler.sendEmptyMessage(SEARCHING);
            SearchLRC searchLRC = new SearchLRC(mInfo);
            //manualPath不为空说明为手动设置歌词
            List<LrcRow> temp = searchLRC.getLrc(mManualPath);
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

    private static class LrcHandler extends Handler{
        final WeakReference<LrcFragment> mRef;
        LrcHandler(Looper looper,LrcFragment fragment){
            super(looper);
            mRef = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            if(mRef.get() == null)
                return;
            mRef.get().handleInternal(msg.what);
        }
    }

}
