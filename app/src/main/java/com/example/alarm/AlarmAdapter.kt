package com.example.alarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AlarmAdapter(
    private val alarms: List<AlarmItem>,
    private val onToggle: (AlarmItem, Boolean) -> Unit,
    private val onDelete: (AlarmItem) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAlarmTime: TextView = view.findViewById(R.id.tvAlarmTime)
        val tvAlarmDays: TextView = view.findViewById(R.id.tvAlarmDays)
        val switchAlarm: SwitchCompat = view.findViewById(R.id.switchAlarm)
        val btnDeleteAlarm: ImageButton = view.findViewById(R.id.btnDeleteAlarm)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]

        val timeString = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
        holder.tvAlarmTime.text = timeString

        holder.switchAlarm.setOnCheckedChangeListener(null)
        holder.switchAlarm.isChecked = alarm.isEnabled

        holder.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            onToggle(alarm, isChecked)
        }

        holder.btnDeleteAlarm.setOnClickListener {
            onDelete(alarm)
        }

        // Simple representation of days
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val activeDays = alarm.daysOfWeek.indices.filter { alarm.daysOfWeek[it] }.map { days[it] }
        holder.tvAlarmDays.text =
            if (activeDays.size == 7) "Every day" else activeDays.joinToString(", ")
    }

    override fun getItemCount(): Int = alarms.size
}
