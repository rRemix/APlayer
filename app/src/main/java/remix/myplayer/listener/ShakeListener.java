package remix.myplayer.listener;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import remix.myplayer.application.APlayerApplication;
import remix.myplayer.util.Constants;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/22 10:27
 */

public class ShakeListener {
    private SensorManager mSensorManager;
    private Context mContext;
    private SensorListener mSensorListener;
    private static final int UPTATE_INTERVAL_TIME = 50;
    private static final int SPEED_THRESHOLD = 30;

    private long mLastUpdateTime;
    private float mLastX;
    private float mLastY;
    private float mLastZ;
    //每500ms最多响应一次操作
    private boolean mHasMessage = false;

    private static ShakeListener mInstance;
    private ShakeListener(Context context){
        mContext = context;
    }

    public synchronized static ShakeListener getInstance(Context context){
        if(mInstance == null){
            mInstance = new ShakeListener(context);
        }
        return mInstance;
    }

    public void beginListen(){
        if(mSensorManager == null)
            mSensorManager = (SensorManager) APlayerApplication.getContext().getSystemService(Context.SENSOR_SERVICE);
        if(mSensorListener == null)
            mSensorListener = new SensorListener();
        mSensorManager.registerListener(mSensorListener,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopListen(){
        if(mSensorManager != null && mSensorListener != null)
            mSensorManager.unregisterListener(mSensorListener);
    }

    private class SensorListener implements SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
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
            if(speed > SPEED_THRESHOLD && !mHasMessage){
                mHasMessage = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Constants.CTL_ACTION);
                        intent.putExtra("Control", Constants.NEXT);
                        mContext.sendBroadcast(intent);
                        mHasMessage = false;
                    }
                },500);
            }
            mLastX = x;
            mLastY = y;
            mLastZ = z;
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}
