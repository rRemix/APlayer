package remix.myplayer.misc;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import remix.myplayer.R;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2017/11/20.
 */

public class MediaScanner {
    private MediaScannerConnection mConnection;
    private MediaScannerConnection.MediaScannerConnectionClient mClient;
    private Context mContext;
    private File mFile;
    private String mMimeType;
    private MaterialDialog mProgressDialog;
    private MaterialDialog mLoadingDialog = new MaterialDialog.Builder(mContext)
            .cancelable(false)
            .title(R.string.please_wait)
            .content(R.string.scan_getfile_count)
            .progress(true,100)
            .build();
    private MsgHandler mHandler;

    private ObservableEmitter<String> mScanEmitter;

    public MediaScanner(Context context){
        mContext = context;
        mHandler = new MsgHandler(this);

        mProgressDialog = new MaterialDialog.Builder(mContext)
                .cancelable(false)
                .progress(false, mToScanCount,true)
                .dismissListener(dialog -> {
                    mConnection.disconnect();
                    mHandler.remove();
                    ToastUtil.show(mContext,mContext.getString(R.string.scanned_count,mToScanCount));
                })
                .title(R.string.scanning).build();

        mClient = new MediaScannerConnection.MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {
                Observable.create((ObservableOnSubscribe<String>) e -> mScanEmitter = e)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    mProgressDialog.setProgress(++mAlreadyScannedCount);
                    mProgressDialog.setContent(s);
                    if (mAlreadyScannedCount == mToScanCount) {
                        mProgressDialog.dismiss();
                    }
                }, throwable -> {
                    mProgressDialog.dismiss();
                    ToastUtil.show(mContext,R.string.scan_failed,throwable.toString());
                });

                mLoadingDialog.show();
                Observable.create((ObservableOnSubscribe<Integer>) e -> {
                    getFileCount(mFile);
                    e.onNext(mToScanFiles.size());
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .flatMap(count -> {
                    mLoadingDialog.dismiss();
                    if(count == 0){
                        return Observable.error(new Throwable(mContext.getString(R.string.no_audio_file)));
                    }
                    mToScanCount = count;
                    mProgressDialog.setMaxProgress(mToScanCount);
                    mProgressDialog.show();
                    return Observable.fromIterable(mToScanFiles).observeOn(Schedulers.io());
                })
                .subscribe(file -> mConnection.scanFile(file.getAbsolutePath(), "audio/*"),
                        throwable -> {
                    mLoadingDialog.dismiss();
                    ToastUtil.show(mContext,R.string.scan_failed,throwable.toString());
                });
            }


            @Override
            public void onScanCompleted(String path, Uri uri) {
                mScanEmitter.onNext(path);
            }
        };
        mConnection = new MediaScannerConnection(mContext,mClient);
    }

    public void scanFiles(File dir,String mimeType){
        mFile = dir;
        mMimeType = mimeType;
        mConnection.connect();
    }

    private List<File> mToScanFiles = new ArrayList<>();
    private int mToScanCount = 0;
    private int mAlreadyScannedCount = 0;

    private void getFileCount(File file){

        if(file.isFile()){
            String ext = getFileExtension(file.getName());
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if(/**!TextUtils.isEmpty(ext) && SUPPORT_FORMAT.indexOf(ext) != -1*/!TextUtils.isEmpty(mime) && mime.startsWith("audio")){
                mToScanFiles.add(file);
            }
        } else {
            File[] files = file.listFiles();
            if(files == null)
                return;
            for(File temp : files){
                getFileCount(temp);
            }
        }
    }


    private String getFileExtension(String fileName ) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1);
        } else
            return null;
    }

    private static final List<String> SUPPORT_FORMAT = Arrays.asList("m4a","aac","flac","mp3","wav","ogg");
}
