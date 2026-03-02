package com.example.alarm
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class StopwatchFragment : Fragment(R.layout.fragment_stopwatch) {

    private var isRunning = false
    private var startTime = 0L
    private var timeBuff = 0L
    private var updateTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvTimer: TextView
    private lateinit var tvLaps: TextView

    private val runnable = object : Runnable {
        @SuppressLint("DefaultLocale")
        override fun run() {
            updateTime = SystemClock.uptimeMillis() - startTime
            val updatedTime = timeBuff + updateTime
            val secs = (updatedTime / 1000).toInt()
            val mins = secs / 60
            val milliseconds = (updatedTime % 1000).toInt()

            if (isVisible) {
                tvTimer.text = String.format("%02d:%02d:%03d", mins, secs % 60, milliseconds)
            }
            handler.postDelayed(this, 0)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTimer = view.findViewById(R.id.tvTimer)
        tvLaps = view.findViewById(R.id.tvLaps)

        view.findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!isRunning) {
                startTime = SystemClock.uptimeMillis()
                handler.postDelayed(runnable, 0)
                isRunning = true
            }
        }

        view.findViewById<Button>(R.id.btnStop).setOnClickListener {
            if (isRunning) {
                timeBuff += updateTime
                handler.removeCallbacks(runnable)
                isRunning = false
            }
        }

        view.findViewById<Button>(R.id.btnLap).setOnClickListener {
            if (isRunning) {
                tvLaps.append("\n${tvTimer.text}")
            }
        }

        view.findViewById<Button>(R.id.btnReset).setOnClickListener {
            handler.removeCallbacks(runnable)
            isRunning = false
            startTime = 0L
            timeBuff = 0L
            updateTime = 0L
            tvTimer.text = "00:00:000"
            tvLaps.text = "Lap Times:\n"
        }
    }
}