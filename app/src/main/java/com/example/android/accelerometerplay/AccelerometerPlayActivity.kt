package com.example.android.accelerometerplay

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView

/**
 * using the accelerometer to integrate the device's acceleration to a position
 * using the Verlet method. This is illustrated with a very simple particle system
 * comprised of a few iron balls freely moving
 */

class AccelerometerPlayActivity : Activity() {

    private lateinit var mSimulationView: SimulationView

    private lateinit var mAccelerationView: TextView

    private lateinit var mVelocityView: TextView

    private lateinit var mDistanceView: TextView

    var xDistance = 0f

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.main)
        mSimulationView = findViewById(R.id.simView) as SimulationView
        mAccelerationView = findViewById(R.id.textAcceleration) as TextView
        mVelocityView = findViewById(R.id.textVelocity) as TextView
        mDistanceView = findViewById(R.id.textDistance) as TextView

        mSimulationView.setOnEventReceivedInterface(object : SimulationView.OnEventReceivedInterface {
            override fun onCalibrate(start: Boolean) {
                mAccelerationView.text = "Calibration " + start
            }

            override fun onZEvent(dTime: Long, xLinearAcc: Float, xVelocity: Float) {
            }

            override fun onYEvent(dTime: Long, xLinearAcc: Float, xVelocity: Float) {
            }

            override fun onXEvent(dTime: Long, xLinearAcc: Float, xVelocity: Float) {
                xDistance += xVelocity * dTime * 1000 * 1000
                mAccelerationView.text = "a:" + xLinearAcc
                mVelocityView.text = "v:" + xVelocity + "m/s"
                mDistanceView.text = "d:" + xDistance + "m"
            }
        })
    }

    override fun onResume() {
        super.onResume()
        mSimulationView.startSimulation()
    }

    override fun onPause() {
        super.onPause()
        mSimulationView.stopSimulation()
    }

}
