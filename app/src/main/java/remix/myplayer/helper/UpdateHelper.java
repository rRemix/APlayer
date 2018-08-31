package remix.myplayer.helper;

import java.util.ArrayList;

import remix.myplayer.bean.mp3.Song;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/23 10:06
 */

public class UpdateHelper {
    private static ArrayList<Callback> mCallbacks = new ArrayList<>();

    public static void update(Song song, boolean isPlay) {
        for(int i = 0 ; i < mCallbacks.size();i++){
            if(mCallbacks.get(i) != null)
                mCallbacks.get(i).UpdateUI(song,isPlay);
        }
    }

    public static void addCallback(Callback callback){
        if(!mCallbacks.contains(callback)){
            mCallbacks.add(callback);
        }
    }

    public static void removeCallback(Callback callback){
        if(mCallbacks.contains(callback))
            mCallbacks.remove(callback);
    }

    public interface Callback{
        void UpdateUI(Song Song, boolean isPlay);
    }
}
