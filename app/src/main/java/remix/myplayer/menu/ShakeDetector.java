package remix.myplayer.menu;

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
    private static final int UPDATE_INTERVAL_TIME = 50;
    private static final int SPEED_THRESHOLD = 30;

    private long mLastUpdateTime;
    private float mLastX;
    private float mLastY;
    private float mLastZ;
    private boolean mDetect = false;
    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mContext.sendBroadcast(new Intent(MusicService.ACTION_CMD)
                    .putExtra("Control", Constants.NEXT));
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
        if(mSensor == null)
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopListen(){
        mDetect = false;

        if(mSensorManager == null)
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        if(mSensor == null){
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        mSensorManager.unregisterListener(this,mSensor);
        mHandler.removeCallbacks(mRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!mDetect)
            return;
        long currentUpdateTime = System.currentTimeMillis();
        long timeInterval = currentUpdateTime - mLastUpdateTime;
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
            mHandler.removeCallbacks(mRunnable);
            mHandler.postDelayed(mRunnable,UPDATE_INTERVAL_TIME);
        }
        mLastX = x;
        mLastY = y;
        mLastZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
