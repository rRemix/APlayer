package remix.myplayer.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.interfaces.OnInflateFinishListener;
import remix.myplayer.lyric.LrcView;
import remix.myplayer.lyric.SearchLrc;
import remix.myplayer.lyric.bean.LrcRow;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 歌词界面Fragment
 */
public class LrcFragment extends BaseFragment {
    private OnInflateFinishListener mOnFindListener;
    private Song mInfo;
    @BindView(R.id.lrc_view)
    LrcView mLrcView;
//    @BindView(R.id.lrc_recyclerview)
//    RecyclerView mLyricRecyclerView;
//    private LyricAdapter mLyricAdapter;
    //歌词
    private List<LrcRow> mLrcList;

    private Disposable mDisposable;

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
        mInfo = getArguments().getParcelable("Song");

//        mLyricRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
//        mLyricAdapter = new LyricAdapter(mContext,R.layout.item_lyric,mLyricRecyclerView);
//        mLyricRecyclerView.setAdapter(mLyricAdapter);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mDisposable != null && !mDisposable.isDisposed()){
            mDisposable.dispose();
        }
    }

    public void updateLrc(Song song){
        updateLrc(song,false);
    }

    public void updateLrc(Song song,boolean clearCache) {
        mInfo = song;
        getLrc("",clearCache);
    }

    public void updateLrc(String lrcPath){
        getLrc(lrcPath,true);
    }

    @SuppressLint("CheckResult")
    private void getLrc(String manualPath, boolean clearCache) {
        if (mInfo == null) {
            mLrcView.setText(getString(R.string.no_lrc));
            return;
        }

        final int id = mInfo.getId();
        new SearchLrc(mInfo).getLyric(manualPath,clearCache)
                .doOnSubscribe(disposable -> {
                    mLrcView.setText(getString(R.string.searching));
                    mDisposable = disposable;
                })
                .subscribe(lrcRows -> {
                    if (id == mInfo.getId()) {
                        mLrcList = lrcRows;
                        if (mLrcList == null || mLrcList.size() == 0) {
                            mLrcView.setText(getString(R.string.no_lrc));
                            return;
                        }
                        mLrcView.setLrcRows(mLrcList);

//                        mLyricAdapter.setData(mLrcList);
//                        mLyricAdapter.notifyDataSetChanged();
                    }
                }, throwable -> {
                    if (id == mInfo.getId()) {
                        mLrcList = new ArrayList<>();
                        mLrcView.setText(getString(R.string.no_lrc));
                    }
                });

    }

}
