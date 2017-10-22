package remix.myplayer.helper;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Remix on 2016/12/22.
 */

public class MusicEventHelper {
    private static ArrayList<MusicEventCallback> mMusicEventCallbacks = new ArrayList<>();

    public static List<MusicEventCallback> getCallbacks(){
        return mMusicEventCallbacks;
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
        void onPermissionChanged(boolean has);
        void onPlayListChanged();
    }
}
