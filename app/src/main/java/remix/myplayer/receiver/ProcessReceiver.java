package remix.myplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.afollestad.materialdialogs.MaterialDialog;

import remix.myplayer.R;
import remix.myplayer.application.Application;
import remix.myplayer.util.Constants;

/**
 * @ClassName ProcessReceiver
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/24 15:31
 */

public class ProcessReceiver extends BroadcastReceiver {
    private MaterialDialog mMdDialog;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.PROCESSING:
                    if(mMdDialog != null)
                        mMdDialog.show();
                    break;
                case Constants.STOP_PROCESS:
                    if(mMdDialog != null && mMdDialog.isShowing())
                        mMdDialog.dismiss();
            }
        }
    };
    @Override
    public void onReceive(Context context, Intent intent) {
        if(mMdDialog == null){
            mMdDialog = new MaterialDialog.Builder(Application.getContext())
                    .title("处理中")
                    .content("请等待")
                    .progress(true, 0)
                    .progressIndeterminateStyle(true)
                    .build();
        }
        if(intent != null)
            mHandler.sendEmptyMessage(intent.getIntExtra("Process",Constants.STOP_PROCESS));
    }
}
