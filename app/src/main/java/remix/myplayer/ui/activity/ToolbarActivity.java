package remix.myplayer.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.dialog.TimerDialog;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.XmlUtil;


/**
 * Created by taeja on 16-3-15.
 */
public class ToolbarActivity extends BaseAppCompatActivity {
    public interface OnMultiItemClick{
        void OnAddToPlayingList(MultiChoice multiChoice,int type);
        void OnAddToPlayList(MultiChoice multiChoice,int type);
        void OnDelete(MultiChoice multiChoice,int type);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected <T extends View> T findView(int id){
        return (T)findViewById(id);
    }

    protected void initToolbar(Toolbar toolbar,String title){
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(Color.parseColor("#ffffffff"));
//        toolbar.setBackgroundColor(ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY));
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.common_btn_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickNavigation();
            }
        });
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_search:
                        startActivity(new Intent(ToolbarActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(ToolbarActivity.this, TimerDialog.class));
                        break;
                    case R.id.toolbar_delete:
                        Toast.makeText(ToolbarActivity.this,"删除",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.toolbar_add_playing:
                        Toast.makeText(ToolbarActivity.this,"添加到正在播放列表 ",Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.toolbar_add_playlist:
                        Toast.makeText(ToolbarActivity.this,"添加到播放列表",Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });
    }

    protected void onClickNavigation(){
        finish();
    }


    protected void Test(final MultiChoice choice){
        OnMultiItemClick onMultiItemClick = new OnMultiItemClick() {
            @Override
            public void OnAddToPlayingList(MultiChoice multiChoice, int type) {
                int num = 0;
                ArrayList<Integer> idList = new ArrayList<>();
                switch (choice.TYPE){
                    case Constants.SONG:
                        for(Object arg : choice.mSelectedArg){
                            if (arg instanceof Integer)
                                idList.add((Integer) arg);
                        }
                        break;
                    case Constants.ALBUM:
                    case Constants.ARTIST:
                    case Constants.FOLDER:
                    case Constants.PLAYLIST:
                        for(Object arg : choice.mSelectedArg){
                            ArrayList<Integer> tempList = DBUtil.getSongIdListByArg(arg,choice.TYPE);
                            if(tempList != null && tempList.size() > 0)
                                idList.addAll(DBUtil.getSongIdListByArg(arg,choice.TYPE));
                        }
                        break;
                }
                num = XmlUtil.addSongsToPlayingList(idList);
                if(num > 0){
                    Toast.makeText(ToolbarActivity.this,
                            getResources().getString(R.string.add_multi_song_playinglist_success,num)
                            ,Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ToolbarActivity.this,R.string.add_song_playinglist_error,Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void OnAddToPlayList(MultiChoice multiChoice, int type) {
                int num = 0;
                ArrayList<Integer> idList = new ArrayList<>();
                switch (choice.TYPE){
                    case Constants.SONG:
                        for(Object arg : choice.mSelectedArg){
                            if (arg instanceof Integer)
                                idList.add((Integer) arg);
                        }
                        break;
                    case Constants.ALBUM:
                    case Constants.ARTIST:
                    case Constants.FOLDER:
                    case Constants.PLAYLIST:
                        for(Object arg : choice.mSelectedArg){
                            ArrayList<Integer> tempList = DBUtil.getSongIdListByArg(arg,choice.TYPE);
                            if(tempList != null && tempList.size() > 0)
                                idList.addAll(DBUtil.getSongIdListByArg(arg,choice.TYPE));
                        }
                        break;
                }
                num = XmlUtil.addSongsToPlayList("我的收藏",DBUtil.getPlayListItemListByIds(idList));
                if(num > 0){
                    Toast.makeText(ToolbarActivity.this,
                            getResources().getString(R.string.add_multi_song_playlist_success,num)
                            ,Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ToolbarActivity.this,R.string.add_song_playinglist_error,Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void OnDelete(MultiChoice multiChoice, int type) {
                int num = 0;
                ArrayList<Integer> idList = new ArrayList<>();
                switch (choice.TYPE){
                    case Constants.SONG:
                        for(Object arg : choice.mSelectedArg){
                            if (arg instanceof Integer)
                                idList.add((Integer) arg);
                        }
                        break;
                    case Constants.ALBUM:
                    case Constants.ARTIST:
                    case Constants.FOLDER:
                    case Constants.PLAYLIST:
                        for(Object arg : choice.mSelectedArg){
                            ArrayList<Integer> tempList = DBUtil.getSongIdListByArg(arg,choice.TYPE);
                            if(tempList != null && tempList.size() > 0)
                                idList.addAll(tempList);
                        }
                        break;
                }
                for(Integer id : idList){
                    if( DBUtil.deleteSong(id,Constants.SONG))
                        num++;
                }
                Log.d("111222","num: " + num);
                if(num > 0){
                    Toast.makeText(ToolbarActivity.this,
                            getResources().getString(R.string.delete_success),Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ToolbarActivity.this,R.string.delete_error,Toast.LENGTH_SHORT).show();
                }
            }
        };

        onMultiItemClick.OnDelete(choice,1);
    }
}
