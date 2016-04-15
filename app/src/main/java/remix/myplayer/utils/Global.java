package remix.myplayer.utils;

/**
 * Created by taeja on 16-4-15.
 */
public class Global {
    /**
     * 耳机是否插入
     */
    private static boolean mIsHeadsetOn = false;
    public static void setHeadsetOn(boolean headsetOn){
        mIsHeadsetOn = headsetOn;
    }
    public static boolean getHeadsetOn(){
        return mIsHeadsetOn;
    }
}
