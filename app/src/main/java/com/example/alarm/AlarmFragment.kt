package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar

class AlarmFragment : Fragment(R.layout.fragment_alarm), EditAlarmFragment.OnAlarmEditedListener {

    private lateinit var alarmAdapter: AlarmAdapter
    private val alarms = mutableListOf<AlarmItem>()
    private lateinit var dbHelper: DatabaseHelper
    private var currentUsername: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        
        // Lấy username hiện tại
        val userPrefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        currentUsername = userPrefs.getString("current_user", "") ?: ""

        loadAlarms()

        val rvAlarms = view.findViewById<RecyclerView>(R.id.rvAlarms)
        alarmAdapter = AlarmAdapter(alarms, { alarm, isEnabled ->
            alarm.isEnabled = isEnabled
            dbHelper.updateAlarm(alarm) // Cập nhật vào DB
            if (isEnabled) {
                scheduleAlarm(alarm)
            } else {
                cancelAlarm(alarm)
            }
        }, { alarm ->
            cancelAlarm(alarm)
            dbHelper.deleteAlarm(alarm.id) // Xóa khỏi DB
            alarms.remove(alarm)
            alarmAdapter.notifyDataSetChanged()
        }, { alarm ->
            showEditAlarmFragment(alarm)
        })

        rvAlarms.layoutManager = LinearLayoutManager(requireContext())
        rvAlarms.adapter = alarmAdapter

        view.findViewById<FloatingActionButton>(R.id.fabAddAlarm).setOnClickListener {
            showMaterialTimePicker()
        }
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
                id = 0, // Database sẽ tự tăng ID
                hour = picker.hour,
                minute = picker.minute,
                daysOfWeek = BooleanArray(7) { true },
                soundUriString = null,
                isEnabled = true
            )
            
            // Lưu vào DB và lấy ID thực tế
            val newId = dbHelper.addAlarm(currentUsername, newAlarm)
            val alarmWithId = newAlarm.copy(id = newId.toInt())
            
            alarms.add(alarmWithId)
            alarmAdapter.notifyDataSetChanged()
            scheduleAlarm(alarmWithId)
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
            dbHelper.updateAlarm(alarm) // Cập nhật vào DB
            alarmAdapter.notifyDataSetChanged()
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

    private fun loadAlarms() {
        alarms.clear()
        alarms.addAll(dbHelper.getAlarmsByUser(currentUsername))
    }
}
