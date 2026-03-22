package com.example.alarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale

class EditAlarmFragment : Fragment() {

    private lateinit var alarmItem: AlarmItem
    private lateinit var tvEditTime: TextView
    private lateinit var tvEditSoundValue: TextView
    private lateinit var layoutDays: LinearLayout
    
    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0
    private var selectedSoundUri: Uri? = null
    private val selectedDays = BooleanArray(7)

    interface OnAlarmEditedListener {
        fun onAlarmEdited(alarm: AlarmItem)
    }

    var listener: OnAlarmEditedListener? = null

    companion object {
        fun newInstance(alarm: AlarmItem): EditAlarmFragment {
            val fragment = EditAlarmFragment()
            fragment.alarmItem = alarm
            fragment.selectedHour = alarm.hour
            fragment.selectedMinute = alarm.minute
            fragment.selectedSoundUri = alarm.soundUriString?.let { Uri.parse(it) }
            alarm.daysOfWeek.copyInto(fragment.selectedDays)
            return fragment
        }
    }

    private val soundPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? =
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (uri != null) {
                    selectedSoundUri = uri
                    updateSoundText(uri)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_alarm, container, false)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbarEditAlarm)
        tvEditTime = view.findViewById(R.id.tvEditTime)
        tvEditSoundValue = view.findViewById(R.id.tvEditSoundValue)
        layoutDays = view.findViewById(R.id.layoutDays)
        val layoutEditSound = view.findViewById<RelativeLayout>(R.id.layoutEditSound)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveEdit)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelEdit)

        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }

        updateTimeText()
        updateSoundText(selectedSoundUri)
        setupDayButtons()

        tvEditTime.setOnClickListener { showTimePicker() }
        layoutEditSound.setOnClickListener { pickSound() }

        btnSave.setOnClickListener {
            val editedAlarm = alarmItem.copy(
                hour = selectedHour,
                minute = selectedMinute,
                daysOfWeek = selectedDays.copyOf(),
                soundUriString = selectedSoundUri?.toString(),
                isEnabled = true
            )
            listener?.onAlarmEdited(editedAlarm)
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun updateTimeText() {
        tvEditTime.text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
    }

    private fun updateSoundText(uri: Uri?) {
        if (uri == null) {
            tvEditSoundValue.text = "Mặc định"
            return
        }
        val ringtone = RingtoneManager.getRingtone(requireContext(), uri)
        tvEditSoundValue.text = ringtone.getTitle(requireContext())
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(selectedHour)
            .setMinute(selectedMinute)
            .setTitleText("Chọn giờ")
            .build()

        picker.addOnPositiveButtonClickListener {
            selectedHour = picker.hour
            selectedMinute = picker.minute
            updateTimeText()
        }
        picker.show(childFragmentManager, "EDIT_TIME_PICKER")
    }

    private fun pickSound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn âm thanh")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedSoundUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }
        soundPickerLauncher.launch(intent)
    }

    private fun setupDayButtons() {
        val days = arrayOf("S", "M", "T", "W", "T", "F", "S")
        layoutDays.removeAllViews()
        for (i in 0..6) {
            val tv = TextView(requireContext()).apply {
                text = days[i]
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16, 0, 16)
                textSize = 16.sp
                val unselectedColor = ContextCompat.getColor(context, android.R.color.darker_gray)
                val selectedColor = ContextCompat.getColor(context, R.color.white)
                
                setTextColor(if (selectedDays[i]) selectedColor else unselectedColor)
                setBackgroundResource(if (selectedDays[i]) R.drawable.bg_day_selected else 0)
                setOnClickListener {
                    selectedDays[i] = !selectedDays[i]
                    setTextColor(if (selectedDays[i]) selectedColor else unselectedColor)
                    setBackgroundResource(if (selectedDays[i]) R.drawable.bg_day_selected else 0)
                }
            }
            layoutDays.addView(tv)
        }
    }
    
    private val Int.sp: Float get() = this * resources.displayMetrics.scaledDensity
}
