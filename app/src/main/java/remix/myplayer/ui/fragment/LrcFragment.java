package remix.myplayer.ui.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.bean.netease.NLrcResponse;
import remix.myplayer.bean.netease.NSongSearchResponse;
import remix.myplayer.interfaces.OnInflateFinishListener;
import remix.myplayer.lyric.DefaultLrcParser;
import remix.myplayer.lyric.LrcRow;
import remix.myplayer.lyric.LrcView;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.misc.cache.DiskLruCache;
import remix.myplayer.request.network.HttpClient;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.Util;

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
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mDisposable != null && !mDisposable.isDisposed()){
            mDisposable.dispose();
        }
    }

    public void updateLrc(Song song) {
        mInfo = song;
        getLrc("");
    }

    public void updateLrc(String lrcPath){
        getLrc(lrcPath);
    }

    private void getLrc(String manualPath){
        if(mInfo == null){
            mLrcView.setText(getString(R.string.no_lrc));
            return;
        }
        final DefaultLrcParser lrcParser = new DefaultLrcParser();

        //网易歌词
        Observable<List<LrcRow>> neteaseObservable = HttpClient.getNeteaseApiservice()
                .getNeteaseSearch(mInfo.getArtist() + "-" + mInfo.getTitle(),0,1,1)
                .flatMap(body -> HttpClient.getInstance()
                        .getNeteaseLyric(new Gson().fromJson(body.string(),NSongSearchResponse.class).result.songs.get(0).id)
                        .map(body1 -> {
                            NLrcResponse lrcResponse = new Gson().fromJson(body1.string(),NLrcResponse.class);
                            List<LrcRow> original = lrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(lrcResponse.lrc.lyric.getBytes(Charset.forName("utf8"))), Charset.forName("utf8"))),false,mInfo.getTitle(),mInfo.getArtist() + "/original");
                            //有翻译 尝试合并
                            if(lrcResponse.tlyric != null && !TextUtils.isEmpty(lrcResponse.tlyric.lyric)){
                                List<LrcRow> translate = lrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(lrcResponse.tlyric.lyric.getBytes(Charset.forName("utf8"))), Charset.forName("utf8"))),false,mInfo.getTitle(),mInfo.getArtist() + "/translate");
                                if(translate != null && translate.size() > 0){
                                    LrcRow translateFirstRow = translate.get(0);
                                    int delta = 0;
                                    boolean find = false;
                                    for(int i = 0; i < original.size();i++){
                                        if(translateFirstRow.getTime() != original.get(i).getTime() && !find){
                                            delta++;
                                        }else {
                                            find = true;
                                            if(i - delta < translate.size())
                                                original.get(i).setContent(original.get(i).getContent() + "" + translate.get(i - delta).getContent());
                                        }
                                    }
                                }
                            }
                            return original;
                        }))
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                });
        //本地歌词
        Observable<List<LrcRow>> localObservable = Observable.create(e -> {
            String localPath = getlocalLrcPath();
            if(!TextUtils.isEmpty(localPath)){
                e.onNext(lrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new FileInputStream(localPath))),true, mInfo.getTitle(),mInfo.getArtist()));
            }
            e.onComplete();
        });
        //根据在线和本地的优先级 确定最后一级
        boolean onlineFirst = SPUtil.getValue(APlayerApplication.getContext(),"Setting","OnlineLrc",false);
        Observable<List<LrcRow>> last = Observable.concat(onlineFirst ? neteaseObservable : localObservable ,onlineFirst ? localObservable : neteaseObservable).firstOrError().toObservable();

        final int songId = mInfo.getId();
        Observable.create((ObservableOnSubscribe<List<LrcRow>>) e -> {
            //手动设置的歌词
            if(!TextUtils.isEmpty(manualPath)){
                e.onNext(lrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new FileInputStream(manualPath))),true, mInfo.getTitle(),mInfo.getArtist()));
            }
            e.onComplete();
        }).switchIfEmpty(Observable.create(e -> {
            //缓存
            DiskLruCache.Snapshot snapShot = DiskCache.getLrcDiskCache().get(Util.hashKeyForDisk(mInfo.getTitle() + "/" + mInfo.getArtist()));
            if(snapShot != null){
                e.onNext(lrcParser.getLrcRows(new BufferedReader(new InputStreamReader(snapShot.getInputStream(0))),false, mInfo.getTitle(),mInfo.getArtist()));
            }
            e.onComplete();
        })).switchIfEmpty(last)
        .compose(RxUtil.applyScheduler())
        .doOnSubscribe(disposable -> {
            mLrcView.setText(getString(R.string.searching));
            mDisposable = disposable;
        })
        .subscribe(lrcRows -> {
            if(songId == mInfo.getId()){
                mLrcList = lrcRows;
                if(mLrcList == null || mLrcList.size() == 0) {
                    mLrcView.setText(getString(R.string.no_lrc));
                    return;
                }
                mLrcView.setLrcRows(mLrcList);
            }
        }, throwable -> {
            if(songId == mInfo.getId()){
                mLrcList = new ArrayList<>();
                mLrcView.setText(getString(R.string.no_lrc));
            }
        });

    }

    /**
     * 搜索本地所有歌词文件
     * @return
     */
    private String getlocalLrcPath() {
        //查找本地目录
        String searchPath =  SPUtil.getValue(APlayerApplication.getContext(),"Setting","LrcSearchPath","");
        if(mInfo == null)
            return "";
        if(!TextUtils.isEmpty(searchPath)){
            //已设置歌词路径
            Util.searchFile(mInfo.getDisplayname(),mInfo.getTitle(),mInfo.getArtist(), new File(searchPath));
            if(!TextUtils.isEmpty(Global.CurrentLrcPath)){
                return Global.CurrentLrcPath;
            }
        } else{
            //没有设置歌词路径 搜索所有歌词文件
            Cursor allLrcFiles = null;
            try {
                allLrcFiles = APlayerApplication.getContext().getContentResolver().
                        query(MediaStore.Files.getContentUri("external"),
                                null,
                                MediaStore.Files.FileColumns.DATA + " like ? or " +
                                        MediaStore.Files.FileColumns.DATA + " like ? or " +
                                        MediaStore.Files.FileColumns.DATA + " like ? ",
                                new String[]{"%lyric%","%Lyric%","%.lrc"},
                                null);
                if(allLrcFiles == null || !(allLrcFiles.getCount() > 0))
                    return "";
                while (allLrcFiles.moveToNext()){
                    File file = new File(allLrcFiles.getString(allLrcFiles.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                    if (file.exists() && file.canRead()) {
                        if(Util.isRightLrc(file, mInfo.getDisplayname(),mInfo.getTitle(),mInfo.getArtist())){
                            return file.getAbsolutePath();
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            } finally {
                if(allLrcFiles != null && !allLrcFiles.isClosed())
                    allLrcFiles.close();
            }
        }

        return "";
    }
}
