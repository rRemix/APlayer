package remix.myplayer.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.LinkedList;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.model.LrcItem;
import remix.myplayer.model.MP3Item;
import remix.myplayer.ui.customview.LrcView;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.lrc.SearchLRC;

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
    private MP3Item mInfo;
    @BindView(R.id.lrc_view)
    LrcView mLrcView;
    //歌词列表
    private LinkedList<LrcItem> mLrcList;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(mLrcView == null)
                return;
            //是否正在搜索
            mLrcView.setIsSearching(msg.what == SEARCHING);
            if(msg.what == UPDATE_LRC) {
                //更新歌词
                if(mLrcList != null) {
                    mLrcView.UpdateLrc(mLrcList);
                }
            } else if (msg.what == NO_LRC) {
                //没有找到歌词
                mLrcView.UpdateLrc(null);
            } else if (msg.what == NO_NETWORK) {
                //没用网络
                mLrcView.UpdateLrc(null);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lrc,container,false);
        mUnBinder = ButterKnife.bind(this,rootView);

        mInfo = (MP3Item)getArguments().getSerializable("MP3Item");
        UpdateLrc(mInfo);
        return rootView;
    }

    public void UpdateLrc(MP3Item mp3Item) {
        if(mp3Item == null)
            return;
        new DownloadThread(mp3Item.getTitle(), mp3Item.getArtist()).start();
    }

    class DownloadThread extends Thread {
        private String mName;
        private String mArtist;
        public DownloadThread(String name,String artist) {
            mName = name;
            mArtist = artist;
        }
        @Override
        public void run() {
            if(!CommonUtil.isNetWorkConnected()) {
                mHandler.sendEmptyMessage(NO_NETWORK);
                return;
            }
            mHandler.sendEmptyMessage(SEARCHING);
            mLrcList = new SearchLRC(mName,mArtist).getLrc();
            if(mLrcList == null) {
                mHandler.sendEmptyMessage(NO_LRC);
                return;
            }
            mHandler.sendEmptyMessage(UPDATE_LRC);
        }

    }


}
