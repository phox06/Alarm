package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class AlarmFragment : Fragment(R.layout.fragment_alarm) {

    private lateinit var alarmAdapter: AlarmAdapter
    private val alarms = mutableListOf<AlarmItem>()
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("Alarms", Context.MODE_PRIVATE)
        loadAlarms()

        val rvAlarms = view.findViewById<RecyclerView>(R.id.rvAlarms)
        alarmAdapter = AlarmAdapter(alarms, { alarm, isEnabled ->
            alarm.isEnabled = isEnabled
            if (isEnabled) {
                scheduleAlarm(alarm)
            } else {
                cancelAlarm(alarm)
            }
            saveAlarms()
        }, { alarm ->
            cancelAlarm(alarm)
            alarms.remove(alarm)
            alarmAdapter.notifyDataSetChanged()
            saveAlarms()
        })

        rvAlarms.layoutManager = LinearLayoutManager(requireContext())
        rvAlarms.adapter = alarmAdapter

        view.findViewById<FloatingActionButton>(R.id.fabAddAlarm).setOnClickListener {
            showTimePickerDialog()
        }

        view.findViewById<MaterialButton>(R.id.btnOpenSleepCalc).setOnClickListener {
            showSleepCalculator()
        }
    }

    private fun showSleepCalculator() {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_sleep_calculator, null)
        
        val npSleepHour = view.findViewById<NumberPicker>(R.id.npSleepHour)
        val npSleepMinute = view.findViewById<NumberPicker>(R.id.npSleepMinute)
        val sliderCycles = view.findViewById<Slider>(R.id.sliderCycles)
        val tvWakeUpTime = view.findViewById<TextView>(R.id.tvWakeUpTime)
        val btnSaveAsAlarm = view.findViewById<MaterialButton>(R.id.btnSaveAsAlarm)

        // Setup NumberPickers
        val calendar = Calendar.getInstance()
        npSleepHour.minValue = 0
        npSleepHour.maxValue = 23
        npSleepHour.value = calendar.get(Calendar.HOUR_OF_DAY)
        npSleepHour.wrapSelectorWheel = true
        
        npSleepMinute.minValue = 0
        npSleepMinute.maxValue = 59
        npSleepMinute.value = calendar.get(Calendar.MINUTE)
        npSleepMinute.wrapSelectorWheel = true

        val formatter = NumberPicker.Formatter { String.format("%02d", it) }
        npSleepHour.setFormatter(formatter)
        npSleepMinute.setFormatter(formatter)

        fun updateResult() {
            val calcCalendar = Calendar.getInstance()
            calcCalendar.set(Calendar.HOUR_OF_DAY, npSleepHour.value)
            calcCalendar.set(Calendar.MINUTE, npSleepMinute.value)
            
            val cycles = sliderCycles.value.toInt()
            // Công thức: Giờ thức dậy = Giờ đi ngủ + (90 phút * Số chu kỳ) + 10 phút
            val totalMinutesToAdd = (cycles * 90) + 10
            calcCalendar.add(Calendar.MINUTE, totalMinutesToAdd)
            
            val h = calcCalendar.get(Calendar.HOUR_OF_DAY)
            val m = calcCalendar.get(Calendar.MINUTE)
            tvWakeUpTime.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
        }

        val pickerListener = NumberPicker.OnValueChangeListener { _, _, _ -> updateResult() }
        npSleepHour.setOnValueChangedListener(pickerListener)
        npSleepMinute.setOnValueChangedListener(pickerListener)

        sliderCycles.addOnChangeListener { _, _, _ ->
            updateResult()
        }

        btnSaveAsAlarm.setOnClickListener {
            val timeParts = tvWakeUpTime.text.split(":")
            val h = timeParts[0].toInt()
            val m = timeParts[1].toInt()
            
            val newAlarm = AlarmItem(
                id = System.currentTimeMillis().toInt(),
                hour = h,
                minute = m,
                daysOfWeek = BooleanArray(7) { true },
                soundUriString = null,
                isEnabled = true
            )
            alarms.add(newAlarm)
            alarmAdapter.notifyDataSetChanged()
            saveAlarms()
            scheduleAlarm(newAlarm)
            dialog.dismiss()
            Toast.makeText(requireContext(), "Đã thêm báo thức lúc ${tvWakeUpTime.text}", Toast.LENGTH_SHORT).show()
        }

        updateResult()
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
            val newAlarm = AlarmItem(
                id = System.currentTimeMillis().toInt(),
                hour = hourOfDay,
                minute = minute,
                daysOfWeek = BooleanArray(7) { true },
                soundUriString = null,
                isEnabled = true
            )
            alarms.add(newAlarm)
            alarmAdapter.notifyDataSetChanged()
            saveAlarms()
            scheduleAlarm(newAlarm)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun scheduleAlarm(alarm: AlarmItem) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${requireContext().packageName}".toUri()
                }
                startActivity(intent)
                return
            }
        }

        val triggerTime = AlarmUtils.getNextAlarmTimeMillis(alarm)
        if (triggerTime != -1L) {
            val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                putExtra("SOUND_URI", alarm.soundUriString)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(), alarm.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(alarm: AlarmItem) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun saveAlarms() {
        val json = gson.toJson(alarms)
        sharedPreferences.edit().putString("alarm_list", json).apply()
    }

    private fun loadAlarms() {
        val json = sharedPreferences.getString("alarm_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<AlarmItem>>() {}.type
            val loadedAlarms: MutableList<AlarmItem> = gson.fromJson(json, type)
            alarms.clear()
            alarms.addAll(loadedAlarms)
        }
    }
}
