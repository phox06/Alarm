package com.example.alarm

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.Locale

class StopwatchFragment : Fragment(R.layout.fragment_stopwatch) {

    private var isRunning = false
    private var startTime = 0L
    private var timeBuff = 0L
    private var updateTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    
    private lateinit var tvTimer: TextView
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var btnLap: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var rvLaps: RecyclerView
    
    private val lapList = mutableListOf<String>()
    private lateinit var lapAdapter: LapAdapter

    private val runnable = object : Runnable {
        override fun run() {
            updateTime = SystemClock.uptimeMillis() - startTime
            val updatedTime = timeBuff + updateTime
            
            val milliseconds = (updatedTime % 1000) / 10
            val secs = (updatedTime / 1000).toInt()
            val mins = secs / 60
            val hours = mins / 60

            if (isVisible) {
                tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", mins % 60, secs % 60, milliseconds)
            }
            handler.postDelayed(this, 30)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTimer = view.findViewById(R.id.tvTimer)
        btnStart = view.findViewById(R.id.btnStart)
        btnStop = view.findViewById(R.id.btnStop)
        btnLap = view.findViewById(R.id.btnLap)
        btnReset = view.findViewById(R.id.btnReset)
        rvLaps = view.findViewById(R.id.rvLaps)

        lapAdapter = LapAdapter(lapList)
        rvLaps.layoutManager = LinearLayoutManager(requireContext())
        rvLaps.adapter = lapAdapter

        btnStart.setOnClickListener {
            startStopwatch()
        }

        btnStop.setOnClickListener {
            stopStopwatch()
        }

        btnLap.setOnClickListener {
            addLap()
        }

        btnReset.setOnClickListener {
            resetStopwatch()
        }
    }

    private fun startStopwatch() {
        if (!isRunning) {
            startTime = SystemClock.uptimeMillis()
            handler.postDelayed(runnable, 0)
            isRunning = true
            
            btnStart.visibility = View.GONE
            btnStop.visibility = View.VISIBLE
            btnReset.isEnabled = false
            btnLap.isEnabled = true
        }
    }

    private fun stopStopwatch() {
        if (isRunning) {
            timeBuff += updateTime
            handler.removeCallbacks(runnable)
            isRunning = false
            
            btnStart.text = "Tiếp tục"
            btnStart.visibility = View.VISIBLE
            btnStop.visibility = View.GONE
            btnReset.isEnabled = true
            btnLap.isEnabled = false
        }
    }

    private fun addLap() {
        if (isRunning) {
            lapList.add(0, tvTimer.text.toString())
            lapAdapter.notifyItemInserted(0)
            rvLaps.scrollToPosition(0)
        }
    }

    private fun resetStopwatch() {
        handler.removeCallbacks(runnable)
        isRunning = false
        startTime = 0L
        timeBuff = 0L
        updateTime = 0L
        
        tvTimer.text = "00:00:00"
        btnStart.text = "Bắt đầu"
        btnStart.visibility = View.VISIBLE
        btnStop.visibility = View.GONE
        btnReset.isEnabled = true
        btnLap.isEnabled = false
        
        lapList.clear()
        lapAdapter.notifyDataSetChanged()
    }
}

class LapAdapter(private val laps: List<String>) : RecyclerView.Adapter<LapAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLapNumber: TextView = view.findViewById(R.id.tvLapNumber)
        val tvLapTime: TextView = view.findViewById(R.id.tvLapTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lap, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvLapNumber.text = "Vòng ${laps.size - position}"
        holder.tvLapTime.text = laps[position]
    }

    override fun getItemCount(): Int = laps.size
}
