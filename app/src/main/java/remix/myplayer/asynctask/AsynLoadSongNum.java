package remix.myplayer.asynctask;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.TextView;

import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/11 14:07
 */
public class AsynLoadSongNum extends AsyncTask<Integer, Integer, Integer> {
    private final TextView mNum;
    private final int mType;
    public AsynLoadSongNum(TextView textView,int type) {
        mNum = textView;
        mType = type;
    }

    @Override
    protected Integer doInBackground(Integer... params) {
        ContentResolver resolver = APlayerApplication.getContext().getContentResolver();
        boolean isAlbum = mType == Constants.ALBUM;
        Cursor cursor = null;
        try {
            cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID},
                    MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection()
                    + " and " + (isAlbum ? MediaStore.Audio.Media.ALBUM_ID  : MediaStore.Audio.Media.ARTIST_ID)+ "=" + params[0],
                    null,null);
            return cursor != null ? cursor.getCount() : 0;
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return 0;
    }

    @Override
    protected void onPostExecute(Integer num) {
        if (mNum != null) {
            if(mType == Constants.ALBUM){
                String album = "";
                if(mNum.getText() != null) {
                    album = mNum.getText().toString();
                }
                mNum.setText(APlayerApplication.getContext().getString(R.string.song_count_2,album,num));
            } else {
                mNum.setText(APlayerApplication.getContext().getString(R.string.song_count_1,num));
            }
        }
    }
}
