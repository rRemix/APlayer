package remix.myplayer.helper;

import java.util.ArrayList;

import remix.myplayer.util.LogUtil;


/**
 * Created by Remix on 2016/12/22.
 */

public class DeleteHelper {
    private static ArrayList<Callback> mCallbacks = new ArrayList<>();

    public static void delete() {
        LogUtil.d("DeleteHelper","CallbackCount:" + mCallbacks.size());
        for(int i = 0 ; i < mCallbacks.size();i++){
            mCallbacks.get(i).OnDelete();
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
        void OnDelete();
    }
}
