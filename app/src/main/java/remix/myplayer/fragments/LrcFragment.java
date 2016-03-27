package remix.myplayer.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.LinkedList;

import remix.myplayer.R;
import remix.myplayer.infos.LrcInfo;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.ui.customviews.LrcView;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.SearchLRC;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 歌词界面Fragment
 */
public class LrcFragment extends Fragment {
    //是否找到歌词的三种状态
    private static int UPDATE_LRC = 1;
    private static int NO_LRC = 2;
    private static int NO_NETWORK = 3;
    private MP3Info mInfo;
    private LrcView mLrcView;
    //歌词列表
    private LinkedList<LrcInfo> mLrcList;
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == UPDATE_LRC) {
                //更新歌词
                if(mLrcList != null && mLrcView != null) {
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
        mInfo = (MP3Info)getArguments().getSerializable("MP3Info");
        mLrcView = (LrcView)rootView.findViewById(R.id.lrc_text);
        UpdateLrc(mInfo);
        return rootView;
    }

    public void UpdateLrc(MP3Info mp3Info) {
        if(mp3Info == null)
            return;
        new DownloadThread(mp3Info.getDisplayname(),mp3Info.getArtist()).start();
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

            mLrcList = new SearchLRC(mName,mArtist).getLrc();
            if(mLrcList == null) {
                mHandler.sendEmptyMessage(NO_LRC);
                return;
            }
            mHandler.sendEmptyMessage(UPDATE_LRC);
        }

    }


}
