package remix.myplayer.broadcastreceivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.activities.ChildHolderActivity;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.activities.SearchActivity;
import remix.myplayer.services.MusicService;

/**
 * Created by taeja on 16-2-16.
 */
public class ExitReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            try{
                context.unregisterReceiver(MainActivity.mInstance.mNotifyReceiver);
                MusicService.mInstance.stopSelf();
                System.exit(0);
            }catch (Exception e){
                e.printStackTrace();
            }

//            if(MainActivity.mInstance != null)
//                MainActivity.mInstance.finish();
//            if(AudioHolderActivity.mInstance != null)
//                AudioHolderActivity.mInstance.finish();
//            if(ChildHolderActivity.mInstance != null)
//                ChildHolderActivity.mInstance.finish();
//            if(PlayListActivity.mInstance != null)
//                PlayListActivity.mInstance.finish();
//            if(SearchActivity.mInstance != null)
//                SearchActivity.mInstance.finish();
//            if(MusicService.mInstance != null)
//                MusicService.mInstance.stopSelf();
//            AudioHolderActivity.mInstance.finish();
//            ChildHolderActivity.mInstance.finish();
//            MainActivity.mInstance.finish();
//            PlayListActivity.mInstance.finish();
//            SearchActivity.mInstance.finish();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
