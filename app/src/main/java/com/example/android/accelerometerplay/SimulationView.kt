package com.example.android.accelerometerplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.properties.Delegates

class SimulationView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyle, defStyleRes), SensorEventListener {

    companion object {
        val NUM_PARTICLES = 5
        val ballDiameter = 0.006f
        var xBound: Float = 0.0f
        var yBound: Float = 0.0f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val mDisplay = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

    private val mAccelerometer: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var mMetersToPixelsX: Float by Delegates.notNull()
    private var mMetersToPixelsY: Float by Delegates.notNull()

    private var mXOrigin: Float = 0f
    private var mYOrigin: Float = 0f
    private var mSensorX: Float = 0f
    private var mSensorY: Float = 0f

    private lateinit var mParticleSystem: ParticleSystem

    private val mBalls = Array(NUM_PARTICLES, { Particle(context) })

    /*It is not necessary to get accelerometer events at a very high
    * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
    * automatic low-pass filter, which "extracts" the gravity component
    * of the acceleration. As an added benefit, we use less power and CPU resources.
    */
    fun startSimulation() = sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)


    fun stopSimulation() = sensorManager.unregisterListener(this)

    init {
        val metrics = DisplayMetrics()
        mDisplay.getMetrics(metrics)

        mMetersToPixelsX = metrics.xdpi / 0.0254f
        mMetersToPixelsY = metrics.ydpi / 0.0254f

        // rescale the mBalls so it's about 0.5 cm on screen
        val mDstWidth = (ballDiameter * mMetersToPixelsX + 0.5f).toInt()
        val mDstHeight = (ballDiameter * mMetersToPixelsY + 0.5f).toInt()

        val w = metrics.widthPixels
        val h = metrics.heightPixels

        mXOrigin = (w - mDstWidth) * 0.5f
        mYOrigin = (h - mDstHeight) * 0.5f

        xBound = (w / mMetersToPixelsX - ballDiameter) * 0.5f
        yBound = (h / mMetersToPixelsY - ballDiameter) * 0.5f

        for (i in mBalls.indices) {
            mBalls[i] = Particle(context)
            if (i == 0)
                mBalls[i].setBackgroundResource(R.drawable.ball_red)
            else
                mBalls[i].setBackgroundResource(R.drawable.ball)
            mBalls[i].setLayerType(View.LAYER_TYPE_HARDWARE, null)
            addView(mBalls[i], ViewGroup.LayoutParams(mDstWidth, mDstHeight))
        }
        mParticleSystem = ParticleSystem(mBalls)

        val opts = BitmapFactory.Options()
        opts.inPreferredConfig = Bitmap.Config.RGB_565

        setWillNotDraw(false) // magic !
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val metrics = DisplayMetrics()
        mDisplay.getMetrics(metrics)

        mMetersToPixelsX = metrics.xdpi / 0.0254f
        mMetersToPixelsY = metrics.ydpi / 0.0254f

        // rescale the mBalls so it's about 0.5 cm on screen
        val mDstWidth = (ballDiameter * mMetersToPixelsX + 0.5f).toInt()
        val mDstHeight = (ballDiameter * mMetersToPixelsY + 0.5f).toInt()
        Log.d("ball w/h", mDstWidth.toString() + "/" + mDstHeight)

        val w = width // metrics.widthPixels
        val h = height //metrics.heightPixels

        mXOrigin = (w - mDstWidth) * 0.5f
        mYOrigin = (h - mDstHeight) * 0.5f

        xBound = (w / mMetersToPixelsX - ballDiameter) * 0.5f
        yBound = (h / mMetersToPixelsY - ballDiameter) * 0.5f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        /*
         * record the accelerometer data, the event's timestamp as well as
         * the current time. The latter is needed so we can calculate the
         * "present" time during rendering. In this application, we need to
         * take into account how the screen is rotated with respect to the
         * sensors (which always return data in a coordinate space aligned
         * to with the screen in its native orientation).
         */

        when (mDisplay.rotation) {
            Surface.ROTATION_0 -> {
                mSensorX = event.values[0]
                mSensorY = event.values[1]
            }
            Surface.ROTATION_90 -> {
                mSensorX = -event.values[1]
                mSensorY = event.values[0]
            }
            Surface.ROTATION_180 -> {
                mSensorX = -event.values[0]
                mSensorY = -event.values[1]
            }
            Surface.ROTATION_270 -> {
                mSensorX = event.values[1]
                mSensorY = -event.values[0]
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        /*
         * Compute the new position of our object, based on accelerometer
         * data and present time.
         */
        val particleSystem = mParticleSystem
        val now = System.currentTimeMillis()
        val xSensor = mSensorX
        val ySensor = mSensorY

        particleSystem.update(xSensor, ySensor, now)

        val xc = mXOrigin
        val yc = mYOrigin
        val xMeters = mMetersToPixelsX
        val yMeters = mMetersToPixelsY
        for (i in mBalls.indices) {
            /*
             * We transform the canvas so that the coordinate system matches
             * the sensors coordinate system with the origin in the center
             * of the screen and the unit is the meter.
             */
            val x = xc + particleSystem.getPosX(i) * xMeters
            val y = yc - particleSystem.getPosY(i) * yMeters
            mBalls[i].translationX = x
            mBalls[i].translationY = y
        }

        // and make sure to redraw asap
        invalidate()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
