package com.example.alarm
import java.util.Calendar
object AlarmUtils {
    fun getNextAlarmTimeMillis(alarm: AlarmItem): Long {
        val calendar = Calendar.getInstance()
        val currentDayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY is 1, so we map to 0-6
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Set the target time on the calendar
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
        calendar.set(Calendar.MINUTE, alarm.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Check if the alarm is for today and the time hasn't passed yet
        if (alarm.daysOfWeek[currentDayIndex] &&
            (alarm.hour > currentHour || (alarm.hour == currentHour && alarm.minute > currentMinute))) {
            return calendar.timeInMillis
        }

        // If not today, find the next active day
        // We start checking from tomorrow (1 day later) up to 7 days later
        for (i in 1..7) {
            val nextDayIndex = (currentDayIndex + i) % 7
            if (alarm.daysOfWeek[nextDayIndex]) {
                calendar.add(Calendar.DAY_OF_YEAR, i)
                return calendar.timeInMillis
            }
        }

        return -1L // No days selected
    }
}
