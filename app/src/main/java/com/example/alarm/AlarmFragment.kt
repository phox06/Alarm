package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
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