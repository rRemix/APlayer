package remix.myplayer.asynctask;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.TextView;

import remix.myplayer.application.Application;
import remix.myplayer.util.Constants;

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
        ContentResolver resolver = Application.getContext().getContentResolver();
        boolean isAlbuum = mType == Constants.ALBUM;
        Cursor cursor = null;
        try {
            cursor = resolver.query(isAlbuum ? MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI : MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    isAlbuum ? new String[]{MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS} :new String[]{MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS},
                    isAlbuum ? MediaStore.Audio.Albums._ID + "=" + params[0] : MediaStore.Audio.Artists._ID + "=" + params[0],
                    null,null);
            if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
                return cursor.getInt(cursor.getColumnIndex(isAlbuum ? MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS : MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS));
            } else {
                return 0;
            }
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
                mNum.setText("");
                mNum.setText(album + "    " + num + "首");
            } else {
                mNum.setText(num + "首");
            }
        }
    }
}
