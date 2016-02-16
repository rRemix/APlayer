package remix.myplayer.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import remix.myplayer.R;
import remix.myplayer.adapters.FolderAdapter;
import remix.myplayer.adapters.SongListAdapter;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/6.
 */
public class SelectedPopupWindow extends Activity {
    private ImageView mAdd;
    private ImageView mRing;
    private ImageView mShare;
    private ImageView mDelete;
    private Button mCancel;
    private TextView mTitle;
    private MP3Info mInfo = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_option);
        int pos = getIntent().getIntExtra("Position",-1);
        if(pos > 0 && pos < Utility.mAllSongList.size() - 1)
            mInfo = new MP3Info(Utility.getMP3InfoById(Utility.mAllSongList.get(pos)));
        if(mInfo == null)
            return;
        mTitle = (TextView)findViewById(R.id.popup_title);
        mTitle.setText(mInfo.getDisplayname());

        //改变高度，并置于底部
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.height = (int) (200 * metrics.densityDpi / 160);
        lp.width = (int) (metrics.widthPixels);
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);

        mAdd = (ImageButton)findViewById(R.id.popup_add);
        mRing= (ImageButton)findViewById(R.id.popup_ring);
        mShare = (ImageButton)findViewById(R.id.popup_share);
        mDelete= (ImageButton)findViewById(R.id.popup_delete);
        mCancel= (Button)findViewById(R.id.popup_cancel);
        //添加到播放列表
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectedPopupWindow.this,AddPopupWindow.class);
                Bundle arg = new Bundle();
                arg.putString("SongName",mInfo.getDisplayname());
                arg.putLong("Id",mInfo.getId());
                intent.putExtras(arg);
                startActivity(intent);
                finish();
            }
        });
        //设置手机铃声
        mRing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setVoice(mInfo.getUrl(), (int) mInfo.getId());
                finish();
            }
        });
        //分享
        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectedPopupWindow.this,SharePopupWindow.class);
                Bundle arg = new Bundle();
                arg.putSerializable("MP3Info",mInfo);
                intent.putExtras(arg);
                startActivity(intent);
                finish();
            }
        });
        //删除
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String result = Utility.deleteSong(mInfo.getUrl(),Utility.DELETE_SINGLE) == true ? "删除成功" : "删除失败";
                Toast.makeText(SelectedPopupWindow.this,result,Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void delete()
    {
        ContentResolver resolver = getContentResolver();
        if(resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,MediaStore.MediaColumns.DATA + "=?",new String[]{mInfo.getUrl()}) > 0)
        {
            //删除播放列表与全部歌曲列表中该歌曲
            for(Long id : Utility.mPlayingList)
            {
                if(mInfo.getId() == id)
                {
                    Utility.mPlayingList.remove(id);
                    break;
                }
            }
            //删除文件夹列表中该歌曲
            //获得歌曲所在文件夹
//            Iterator it = Utility.mFolderList.keySet().iterator();
//            while(it.hasNext())
//            {
//                String Key = (String)it.next();
//                String Name = mInfo.getUrl().substring(0,mInfo.getUrl().lastIndexOf('/'));
//                if(!Key.equals(Name))
//                    continue;
//                ArrayList<MP3Info> list = Utility.mFolderList.get(Key);
//                for(MP3Info mp3Info : list)
//                {
//                    if(mp3Info.getDisplayname().equals(mInfo.getDisplayname()))
//                    {
//                        list.remove(mp3Info);
//                        break;
//                    }
//                }
//                break;
//            }
            //通知适配器刷新
            SongListAdapter.mInstance.notifyDataSetChanged();
            if(FolderAdapter.mInstance != null)
                FolderAdapter.mInstance.notifyDataSetChanged();

            Toast.makeText(getApplicationContext(), "删除成功！", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText( getApplicationContext (),"删除失败！", Toast.LENGTH_SHORT ).show();

    }

    private void setVoice(String path,int Id)
    {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        cv.put(MediaStore.Audio.Media.IS_ALARM, false);
        cv.put(MediaStore.Audio.Media.IS_MUSIC, false);
        // 把需要设为铃声的歌曲更新铃声库
        if(getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv, MediaStore.MediaColumns.DATA + "=?", new String[]{path}) > 0)
        {
            Uri newUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Id);
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, newUri);
            Toast.makeText( getApplicationContext (),"设置铃声成功!", Toast.LENGTH_SHORT ).show();
        }
        else
            Toast.makeText( getApplicationContext (),"设置铃声失败!", Toast.LENGTH_SHORT ).show();
    }

}
