package remix.myplayer.helper;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import remix.myplayer.App;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.LogUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/22 10:27
 */

public class ShakeDetector extends ContextWrapper implements SensorEventListener{
    private SensorManager mSensorManager;
    private Sensor mSensor;

    //发送命令间隔
    private static final int UPDATE_INTERVAL_TIME = 100;
    //加速度阈值
    private static final int SPEED_THRESHOLD = 400;
    //检测间隔
    private static final int DETECTION_THRESHOLD = 100;

    private long mLastUpdateTime;
    private float mLastX;
    private float mLastY;
    private float mLastZ;
    private boolean mDetect = false;
    private Handler mHandler = new Handler();
    private Runnable mRunnable = () -> sendBroadcast(new Intent(MusicService.ACTION_CMD)
            .putExtra("Control", Command.NEXT));

    private static ShakeDetector mInstance;
    private ShakeDetector(Context context){
        super(context);
    }

    public synchronized static ShakeDetector getInstance(){
        if(mInstance == null){
            mInstance = new ShakeDetector(App.getContext());
        }
        return mInstance;
    }

    public void beginListen(){
        mDetect = true;
        //已经开始监听
        if(mSensor != null)
            return;
        if(mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if(mSensor == null)
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void stopListen(){
        mDetect = false;
        if(mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if(mSensor == null){
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        mSensorManager.unregisterListener(this,mSensor);
        mSensorManager = null;
        mSensor = null;
        mHandler.removeCallbacks(mRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!mDetect)
            return;
        long currentUpdateTime = System.currentTimeMillis();
        long timeInterval = currentUpdateTime - mLastUpdateTime;
        if(timeInterval < DETECTION_THRESHOLD){
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
        double speed = (Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / timeInterval) * 1000;
        LogUtil.d("ShakeDetector","speed: " + speed + " interval: " + timeInterval + " sensorType: " + event.sensor.getType());
        if(speed > SPEED_THRESHOLD){
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
