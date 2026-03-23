package com.example.alarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Locale

class EditAlarmFragment : Fragment() {

    private lateinit var alarmItem: AlarmItem
    private lateinit var tvEditTime: TextView
    private lateinit var tvEditSoundValue: TextView
    private lateinit var tvRepeatValue: TextView
    
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
                val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
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
        tvRepeatValue = view.findViewById(R.id.tvRepeatValue)
        val layoutRepeat = view.findViewById<RelativeLayout>(R.id.layoutRepeat)
        val layoutEditSound = view.findViewById<RelativeLayout>(R.id.layoutEditSound)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveEdit)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelEdit)

        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }

        updateTimeText()
        updateSoundText(selectedSoundUri)
        updateRepeatText()

        tvEditTime.setOnClickListener { showTimePicker() }
        layoutEditSound.setOnClickListener { pickSound() }
        layoutRepeat.setOnClickListener { showRepeatDialog() }

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
        try {
            val ringtone = RingtoneManager.getRingtone(requireContext(), uri)
            tvEditSoundValue.text = ringtone?.getTitle(requireContext()) ?: "Mặc định"
        } catch (e: Exception) {
            tvEditSoundValue.text = "Mặc định"
        }
    }

    private fun updateRepeatText() {
        val days = arrayOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
        val selected = mutableListOf<String>()
        for (i in selectedDays.indices) {
            if (selectedDays[i]) {
                selected.add(days[i])
            }
        }

        tvRepeatValue.text = when {
            selected.isEmpty() -> "Không bao giờ"
            selected.size == 7 -> "Hàng ngày"
            selected.size == 5 && !selectedDays[0] && !selectedDays[6] -> "Ngày thường"
            selected.size == 2 && selectedDays[0] && selectedDays[6] -> "Cuối tuần"
            else -> selected.joinToString(", ")
        }
    }

    private fun showRepeatDialog() {
        val days = arrayOf("Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy")
        val checkedItems = selectedDays.copyOf()

        AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Lặp lại")
            .setMultiChoiceItems(days, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Xong") { _, _ ->
                checkedItems.copyInto(selectedDays)
                updateRepeatText()
            }
            .setNegativeButton("Hủy", null)
            .show()
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
}