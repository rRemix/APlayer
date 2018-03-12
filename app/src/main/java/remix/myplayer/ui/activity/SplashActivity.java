package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import com.umeng.analytics.MobclickAgent;

import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;

/**
 * Created by taeja on 16-6-8.
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        final boolean isFirst = SPUtil.getValue(getApplicationContext(), SPUtil.SETTING_KEY.SETTING_NAME, "First", true);

        new Thread(){
            @Override
            public void run() {
                //第一次启动软件
                if(isFirst){
                    //保存默认主题设置
                    SPUtil.putValue(SplashActivity.this,SPUtil.SETTING_KEY.SETTING_NAME,"ThemeMode", ThemeStore.DAY);
                    SPUtil.putValue(SplashActivity.this,SPUtil.SETTING_KEY.SETTING_NAME,"ThemeColor",ThemeStore.THEME_BLUE);
                    //添加我的收藏列表
                    Global.PlayQueueID = PlayListUtil.addPlayList(Constants.PLAY_QUEUE);
                    SPUtil.putValue(SplashActivity.this,SPUtil.SETTING_KEY.SETTING_NAME,"PlayQueueID",Global.PlayQueueID);
                    Global.MyLoveID = PlayListUtil.addPlayList(getString(R.string.my_favorite));
                    SPUtil.putValue(SplashActivity.this,SPUtil.SETTING_KEY.SETTING_NAME,"MyLoveID",Global.MyLoveID);
                }else {
                    Global.PlayQueueID = SPUtil.getValue(SplashActivity.this,SPUtil.SETTING_KEY.SETTING_NAME,"PlayQueueID",-1);
                    Global.MyLoveID = SPUtil.getValue(SplashActivity.this,SPUtil.SETTING_KEY.SETTING_NAME,"MyLoveID",-1);
                    Global.PlayQueue = PlayListUtil.getIDList(Global.PlayQueueID);
                    Global.PlayList = PlayListUtil.getAllPlayListInfo();
//                    Global.RecentlyID = SPUtil.getValue(SplashActivity.this,SPUtil.SETTING_KEY.SETTING_NAME,"RecentlyID",-1);
                }
//                /** 更新最近添加列表 */
//                //不是第一次打开软件，先删除原来的数据
//                if(!isFirst){
//                    long startListen = System.currentTimeMillis();
//                    PlayListUtil.deleteMultiSongs(PlayListUtil.getIDList(Global.RecentlyID),Global.RecentlyID);
//                    PlayListUtil.deletePlayList(SPUtil.getValue(SplashActivity.this,SPUtil.SETTING_KEY.SETTING_NAME,"RecentlyID",-1));
//                    long time = System.currentTimeMillis() - startListen;
//                    LogUtil.d("DELETE","time:" + time);
//                }
//                //新建最近添加列表
//                Global.RecentlyID = PlayListUtil.addPlayList(Constants.RECENTLY);
//                SPUtil.putValue(SplashActivity.this,SPUtil.SETTING_KEY.SETTING_NAME,"RecentlyID", Global.RecentlyID);
//                //获得今天日期
//                Calendar today = Calendar.getInstance();
//                today.setTime(new Date());
//                //查询最近七天添加的歌曲
//                Cursor cursor = null;
//                try {
//                    cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                            new String[]{MediaStore.Audio.Media._ID},
//                            MediaStore.Audio.Media.DATE_ADDED + ">=" + (today.getTimeInMillis() / 1000 - (3600 * 24 * 7)),
//                            null,
//                            null);
//                    if(cursor != null && cursor.getCount() > 0){
//                        ArrayList<Integer> IDList = new ArrayList<>();
//                        while (cursor.moveToNext()){
//                            IDList.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
//                        }
//                        PlayListUtil.addMultiSongs(IDList,Constants.RECENTLY,Global.RecentlyID);
//                    }
//                }catch (Exception e){
//                    e.printStackTrace();
//                } finally {
//                    if(cursor != null && !cursor.isClosed())
//                        cursor.close();
//                }
            }
        }.start();

        startActivity(new Intent(this,MainActivity.class));

    }


    @Override
    protected void setStatusBar() {
        StatusBarUtil.setTransparent(this);
    }

    public void onResume() {
        MobclickAgent.onPageStart(SplashActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(SplashActivity.class.getSimpleName());
        super.onPause();
    }

}
