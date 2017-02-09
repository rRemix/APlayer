package remix.myplayer.listener;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import remix.myplayer.util.Constants;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/22 10:27
 */

public class ShakeDector implements SensorEventListener{
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
            Intent intent = new Intent(Constants.CTL_ACTION);
            intent.putExtra("Control", Constants.NEXT);
            mContext.sendBroadcast(intent);
        }
    };
    private static ShakeDector mInstance;
    private ShakeDector(Context context){
        mContext = context;
    }

    public synchronized static ShakeDector getInstance(Context context){
        if(mInstance == null){
            mInstance = new ShakeDector(context);
        }
        return mInstance;
    }

    public void beginListen(){
        mDetect = true;
        if(mSensorManager == null)
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        if(mSensor == null)
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopListen(){
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mDetect = false;
        mSensorManager.unregisterListener(this,mSensor);
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
