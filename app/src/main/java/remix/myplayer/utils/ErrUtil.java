package remix.myplayer.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Remix on 2016/3/15.
 */
public class ErrUtil {

    private static File mFile;
    private static final String TAG = "DBUtil";
    private static Context mContext;
    public static void setContext(Context context){
        mContext = context;
    }

    public static void writeError(String error) {
        if(mFile == null){
            mFile = new File(Environment.getExternalStorageDirectory() + "/Err.txt");
        }
        FileOutputStream fos = null;
        try {
            if(!mFile.exists())
                mFile.createNewFile();
            fos = new FileOutputStream(mFile,true);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日  HH:mm:ss    ");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            String time = formatter.format(curDate);
            String msg = time + error + "\r\n";
            fos.write(msg.getBytes());
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }
}
