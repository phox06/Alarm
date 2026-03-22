package com.example.alarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var tvCurrentSound: TextView

    private val soundPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? =
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (uri != null) {
                    (activity as? MainActivity)?.setSelectedSoundUri(uri)
                    updateSoundText(uri)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbarSettings)
        val layoutSelectSound = view.findViewById<RelativeLayout>(R.id.layoutSelectSound)
        tvCurrentSound = view.findViewById(R.id.tvCurrentSound)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        layoutSelectSound.setOnClickListener {
            pickSound()
        }

        val currentUri = (activity as? MainActivity)?.getSelectedSoundUri()
        updateSoundText(currentUri)

        return view
    }

    private fun updateSoundText(uri: Uri?) {
        if (uri == null) {
            tvCurrentSound.text = "Không có"
            return
        }
        val ringtone = RingtoneManager.getRingtone(requireContext(), uri)
        tvCurrentSound.text = ringtone.getTitle(requireContext())
    }

    private fun pickSound() {
        val currentUri = (activity as? MainActivity)?.getSelectedSoundUri()
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn âm thanh báo thức")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }
        soundPickerLauncher.launch(intent)
    }
}
