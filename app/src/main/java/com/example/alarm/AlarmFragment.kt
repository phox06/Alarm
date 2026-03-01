package com.example.alarm
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.*
import android.provider.Settings
import androidx.core.net.toUri

class AlarmFragment : Fragment(R.layout.fragment_alarm) {

    private var selectedSoundUri: Uri? = null


    private val dayMapping = arrayOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )

    private val pickSoundLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                selectedSoundUri =
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                Toast.makeText(requireContext(), "Sound Selected!", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timePicker = view.findViewById<TimePicker>(R.id.timePicker)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupDays)
        val btnSet = view.findViewById<Button>(R.id.btnSetAlarm)
        val btnSound = view.findViewById<Button>(R.id.btnPickSound)

        btnSound.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            pickSoundLauncher.launch(intent)
        }

        btnSet.setOnClickListener {
            // 1. Get Selected Days
            val selectedDays = mutableListOf<Int>()
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as Chip
                if (chip.isChecked) {
                    selectedDays.add(dayMapping[i])
                }
            }
            val alarmManager =
                requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Permission is denied. Redirect user to settings.
                Toast.makeText(
                    requireContext(),
                    "Please grant Exact Alarm permission to use this feature.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${requireContext().packageName}".toUri()
                }
                startActivity(intent)
                return@setOnClickListener // Stop executing here until they grant it
            }
            if (selectedDays.isEmpty()) {
                scheduleSingleAlarm(timePicker.hour, timePicker.minute)
            } else {
                scheduleRepeatingAlarm(timePicker.hour, timePicker.minute, selectedDays)
            }
        }
    }

    private fun scheduleSingleAlarm(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        setSystemAlarm(calendar.timeInMillis)
        Toast.makeText(requireContext(), "Alarm set for once!", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleRepeatingAlarm(hour: Int, minute: Int, activeDays: List<Int>) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)


        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val now = System.currentTimeMillis()

        if (activeDays.contains(currentDay) && calendar.timeInMillis > now) {
           //
        } else {
            var daysToAdd = 1
            while (daysToAdd <= 7) {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // Move to next day
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                if (activeDays.contains(dayOfWeek)) {
                    break
                }
                daysToAdd++
            }
        }

        setSystemAlarm(calendar.timeInMillis)
        Toast.makeText(requireContext(), "Alarm set for next occurrence!", Toast.LENGTH_SHORT)
            .show()
    }

    private fun setSystemAlarm(triggerTime: Long) {
        val context = requireContext()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("SOUND_URI", selectedSoundUri?.toString())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
        } catch (e: SecurityException) {
            Toast.makeText(context, "Failed to set alarm: Permission denied", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }
}