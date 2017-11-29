package remix.myplayer.listener;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import remix.myplayer.service.MusicService;
import remix.myplayer.util.Constants;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/22 10:27
 */

public class ShakeDetector implements SensorEventListener{
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Context mContext;
    private static final int UPTATE_INTERVAL_TIME = 50;
    private static final int SPEED_THRESHOLD = 30;

    private long mLastUpdateTime;
    private float mLastX;
    private float mLastY;
    private float mLastZ;
    //每500ms最多响应一次操作
    private final int TIME_DELAY = 500;
    private boolean mDetect = false;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(MusicService.ACTION_CMD);
            intent.putExtra("Control", Constants.NEXT);
            mContext.sendBroadcast(intent);
        }
    };
    private static ShakeDetector mInstance;
    private ShakeDetector(Context context){
        mContext = context;
    }

    public synchronized static ShakeDetector getInstance(Context context){
        if(mInstance == null){
            mInstance = new ShakeDetector(context);
        }
        return mInstance;
    }

    public void beginListen(){
        mDetect = true;
        //已经开始监听？
        if(mSensor != null)
            return;
        if(mSensorManager == null)
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(mSensor != null) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stopListen(){
        mDetect = false;
        if(mSensor != null){
            if(mSensorManager == null)
                mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            mSensorManager.unregisterListener(this,mSensor);
            mSensorManager = null;
            mSensor = null;
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!mDetect)
            return;
        long currentUpdateTime = System.currentTimeMillis();
        long timeInterval = currentUpdateTime - mLastUpdateTime;
        if (timeInterval < UPTATE_INTERVAL_TIME) {
            return;
        }
        mLastUpdateTime = currentUpdateTime;
        // 计算传感器差值
        float[] values = event.values;
        float x = values[0];
        float y = values[1];
        float z = values[2];
        float deltaX = x - mLastX;
        float deltaY = y - mLastY;
        float deltaZ = z - mLastZ;
        double speed = (Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / timeInterval) * 100;
        if(speed > SPEED_THRESHOLD ){
            new Handler().removeCallbacks(mRunnable);
            new Handler().postDelayed(mRunnable,TIME_DELAY);
        }
        mLastX = x;
        mLastY = y;
        mLastZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
