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

class AlarmFragment : Fragment(R.layout.fragment_alarm) {

    private var selectedSoundUri: Uri? = null

    // Helper: Map Chip index to Calendar Day (Sun=1, Mon=2...)
    // Our Chips: 0=Mo, 1=Tu, 2=We, 3=Th, 4=Fr, 5=Sa, 6=Su
    // Calendar:  2=Mo, 3=Tu, 4=We, 5=Th, 6=Fr, 7=Sa, 1=Su
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                        data = Uri.parse("package:${requireContext().packageName}")
                    }
                    startActivity(intent)
                    return@setOnClickListener // Stop executing here until they grant it
                }
            }
            if (selectedDays.isEmpty()) {
                // Default: If no days selected, fire once (Tomorrow/Today)
                scheduleSingleAlarm(timePicker.hour, timePicker.minute)
            } else {
                // Complex: Schedule the *next closest* active day
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

        // If time passed, move to tomorrow
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

        // Find the soonest day in the list
        // 1. Check if today is in the list AND time hasn't passed
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val now = System.currentTimeMillis()

        if (activeDays.contains(currentDay) && calendar.timeInMillis > now) {
            // It's today and time is in the future. Ready to go.
        } else {
            // Loop for next 7 days to find the match
            var daysToAdd = 1
            while (daysToAdd <= 7) {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // Move to next day
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                if (activeDays.contains(dayOfWeek)) {
                    break // Found the next active day!
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
            // Pass the logic to Service if you want it to reschedule automatically
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            // We wrapped this in a try-catch just in case the permission is revoked right as it fires
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