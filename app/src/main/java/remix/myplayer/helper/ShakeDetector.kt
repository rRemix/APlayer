package remix.myplayer.helper

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import remix.myplayer.App
import remix.myplayer.service.Command
import remix.myplayer.util.Util.sendCMDLocalBroadcast

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/22 10:27
 */

class ShakeDetector private constructor() : SensorEventListener, Runnable {
  private var sensorManager: SensorManager? = null
  private var sensor: Sensor? = null

  private var lastDetectTime: Long = 0
  private var lastPostTime: Long = 0
  private var lastX: Float = 0.toFloat()
  private var lastY: Float = 0.toFloat()
  private var lastZ: Float = 0.toFloat()
  private var begin = false
  private val handler = Handler(Looper.getMainLooper())

  fun beginListen() {
    if (begin)
      return
    begin = true
    sensorManager = App.getContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
    sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
  }

  fun stopListen() {
    begin = false
    sensorManager?.unregisterListener(this, sensor)
    sensorManager = null
    sensor = null
    handler.removeCallbacks(this)
  }

  override fun onSensorChanged(event: SensorEvent) {
    if (!begin)
      return
    val currentTime = System.currentTimeMillis()
    val detectInterval = currentTime - lastDetectTime
    if (detectInterval < DETECTION_THRESHOLD) {
      return
    }
    lastDetectTime = currentTime
    // 计算传感器差值
    val values = event.values
    val x = values[0]
    val y = values[1]
    val z = values[2]
    val deltaX = x - lastX
    val deltaY = y - lastY
    val deltaZ = z - lastZ
    val speed = Math.sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()) / detectInterval * 1000
    if (speed > SPEED_THRESHOLD) {
      val postInterval = currentTime - lastPostTime
      if (postInterval > POST_THRESHOLD) {
        handler.removeCallbacks(this)
        handler.post(this)
        lastPostTime = currentTime
      }
    }
    lastX = x
    lastY = y
    lastZ = z

  }

  override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

  }

  override fun run() {
    sendCMDLocalBroadcast(Command.NEXT)
  }

  companion object {
    //加速度阈值
    private const val SPEED_THRESHOLD = 300
    //检测间隔
    private const val DETECTION_THRESHOLD = 100
    //发送指令间隔
    private const val POST_THRESHOLD = 1000L

    @JvmStatic
    @Synchronized
    fun getInstance() = Holder.INSTANCE

  }

  private object Holder {
    val INSTANCE = ShakeDetector()
  }
}
