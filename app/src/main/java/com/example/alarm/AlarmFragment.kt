package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Locale

class AlarmFragment : Fragment(R.layout.fragment_alarm), EditAlarmFragment.OnAlarmEditedListener {

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
        }, { alarm ->
            showEditAlarmFragment(alarm)
        })

        rvAlarms.layoutManager = LinearLayoutManager(requireContext())
        rvAlarms.adapter = alarmAdapter

        view.findViewById<FloatingActionButton>(R.id.fabAddAlarm).setOnClickListener {
            showMaterialTimePicker()
        }

        view.findViewById<MaterialButton>(R.id.btnOpenSleepCalc).setOnClickListener {
            showSleepCalculator()
        }
    }

    private fun showSleepCalculator() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_sleep_calculator, null)
        dialog.setContentView(view)

        val npHour = view.findViewById<NumberPicker>(R.id.npSleepHour)
        val npMinute = view.findViewById<NumberPicker>(R.id.npSleepMinute)
        val tvWakeUpTime = view.findViewById<TextView>(R.id.tvWakeUpTime)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveAsAlarm)

        npHour.minValue = 0
        npHour.maxValue = 23
        npMinute.minValue = 0
        npMinute.maxValue = 59

        val calendar = Calendar.getInstance()
        npHour.value = calendar.get(Calendar.HOUR_OF_DAY)
        npMinute.value = calendar.get(Calendar.MINUTE)

        val updateResult = {
            val h = npHour.value
            val m = npMinute.value
            val cycles = 5 

            val calcCalendar = Calendar.getInstance()
            calcCalendar.set(Calendar.HOUR_OF_DAY, h)
            calcCalendar.set(Calendar.MINUTE, m)
            calcCalendar.add(Calendar.MINUTE, cycles * 90)

            tvWakeUpTime.text = String.format(Locale.getDefault(), "%02d:%02d", 
                calcCalendar.get(Calendar.HOUR_OF_DAY), 
                calcCalendar.get(Calendar.MINUTE))
        }

        npHour.setOnValueChangedListener { _, _, _ -> updateResult() }
        npMinute.setOnValueChangedListener { _, _, _ -> updateResult() }

        updateResult()

        btnSave.setOnClickListener {
            val h = npHour.value
            val m = npMinute.value
            val cycles = 5

            val calcCalendar = Calendar.getInstance()
            calcCalendar.set(Calendar.HOUR_OF_DAY, h)
            calcCalendar.set(Calendar.MINUTE, m)
            calcCalendar.add(Calendar.MINUTE, cycles * 90)

            val newAlarm = AlarmItem(
                id = System.currentTimeMillis().toInt(),
                hour = calcCalendar.get(Calendar.HOUR_OF_DAY),
                minute = calcCalendar.get(Calendar.MINUTE),
                daysOfWeek = BooleanArray(7) { true },
                soundUriString = null,
                isEnabled = true
            )
            alarms.add(newAlarm)
            alarmAdapter.notifyDataSetChanged()
            saveAlarms()
            scheduleAlarm(newAlarm)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showMaterialTimePicker() {
        val calendar = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Chọn giờ báo thức")
            .build()

        picker.addOnPositiveButtonClickListener {
            val newAlarm = AlarmItem(
                id = System.currentTimeMillis().toInt(),
                hour = picker.hour,
                minute = picker.minute,
                daysOfWeek = BooleanArray(7) { true },
                soundUriString = null,
                isEnabled = true
            )
            alarms.add(newAlarm)
            alarmAdapter.notifyDataSetChanged()
            saveAlarms()
            scheduleAlarm(newAlarm)
        }

        picker.show(childFragmentManager, "MATERIAL_TIME_PICKER")
    }

    private fun showEditAlarmFragment(alarm: AlarmItem) {
        val fragment = EditAlarmFragment.newInstance(alarm)
        fragment.listener = this
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .add(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onAlarmEdited(alarm: AlarmItem) {
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            alarms[index] = alarm
            alarmAdapter.notifyDataSetChanged()
            saveAlarms()
            if (alarm.isEnabled) {
                scheduleAlarm(alarm)
            } else {
                cancelAlarm(alarm)
            }
        }
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
