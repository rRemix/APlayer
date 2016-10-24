package remix.myplayer.test;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import remix.myplayer.R;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2016/3/26.
 */
public class DownUtil{
    private static Context mContext;
    private static DownUtil mInstance;
    private final int START = 0;
    private final int STOP = 1;
    private final int NONETWORK = 2;
    private final int ERROR = 3;
    private ProgressDialog mProgressDialog;
    private Handler mHandler = new Handler(mContext.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case START:
                    mProgressDialog = ProgressDialog.show(mContext,"请稍候","处理中",true,false);
                    break;
                case NONETWORK:
                    ToastUtil.show(mContext,"请检查网络连接");
                    break;
                case STOP:
                    if(mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                    break;
                case ERROR:
                    if(mProgressDialog != null && mProgressDialog.isShowing()){
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                    ToastUtil.show(mContext, R.string.share_error);
            }

        }
    };


    private DownUtil(){}

    public static DownUtil getInstance(){
        if(mInstance == null)
            mInstance = new DownUtil();
        return mInstance;
    }

    public static void init(Context context){
        mContext = context;
    }

    public File getImageFile(String imageUrl,String imageName){
        if(!isNetWorkConnected()){
            mHandler.sendEmptyMessage(NONETWORK);
            return null;
        }
        mHandler.sendEmptyMessage(START);
        File mImageFile = null;
        FileOutputStream fos = null;
        try {
            mImageFile = new File(mContext.getCacheDir()  + "/" + imageName + ".png");
            fos = new FileOutputStream(mImageFile);
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();
            byte[] bs = new byte[1024];
            int len = 0;
            // 开始读取
            while ((len = is.read(bs)) != -1) {
                fos.write(bs);
            }
            if(fos != null)
                fos.flush();
        } catch (Exception e) {
            Message msg = new Message();
            msg.obj = e.toString();
            msg.what = ERROR;
            mHandler.sendMessage(msg);
            e.printStackTrace();
        } finally {
            mHandler.sendEmptyMessage(STOP);
            if(fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return mImageFile;
    }

    /**
     * 判断网路是否连接
     * @return
     */
    public boolean isNetWorkConnected() {
        if(mContext != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetWorkInfo = mConnectivityManager.getActiveNetworkInfo();
            if(mNetWorkInfo != null)
                return mNetWorkInfo.isAvailable() && mNetWorkInfo.isConnected();
        }
        return false;
    }
}
