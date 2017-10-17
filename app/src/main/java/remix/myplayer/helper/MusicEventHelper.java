package remix.myplayer.helper;

import java.util.ArrayList;


/**
 * Created by Remix on 2016/12/22.
 */

public class MusicEventHelper {
    private static ArrayList<MusicEventCallback> mMusicEventCallbacks = new ArrayList<>();

    public static void onMediaStoreChanged() {
        for(MusicEventCallback callback : mMusicEventCallbacks){
            callback.onMediaStoreChanged();
        }
    }

    public static void addCallback(MusicEventCallback musicEventCallback){
        if(!mMusicEventCallbacks.contains(musicEventCallback)){
            mMusicEventCallbacks.add(musicEventCallback);
        }
    }

    public static void removeCallback(MusicEventCallback musicEventCallback){
        if(mMusicEventCallbacks.contains(musicEventCallback))
            mMusicEventCallbacks.remove(musicEventCallback);
    }

    public interface MusicEventCallback {
        void onMediaStoreChanged();
    }
}
