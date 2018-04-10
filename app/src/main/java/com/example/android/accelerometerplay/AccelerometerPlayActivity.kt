package com.example.android.accelerometerplay

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager

/**
 * using the accelerometer to integrate the device's acceleration to a position
 * using the Verlet method. This is illustrated with a very simple particle system
 * comprised of a few iron balls freely moving
 */

class AccelerometerPlayActivity : Activity() {

    private lateinit var mSimulationView: SimulationView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.main)
        mSimulationView = findViewById(R.id.simView) as SimulationView
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
