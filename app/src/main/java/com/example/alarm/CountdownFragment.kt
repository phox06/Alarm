package com.example.alarm

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

class CountdownFragment : Fragment() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var layoutTimerContainer: ConstraintLayout
    private lateinit var layoutSetupExtras: View

    private lateinit var progressCountdown: CircularProgressIndicator

    private lateinit var npHours: NumberPicker
    private lateinit var npMinutes: NumberPicker
    private lateinit var npSeconds: NumberPicker

    private lateinit var btnStart: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var btnPauseResume: MaterialButton
    private lateinit var btnSelectSound: MaterialButton
    private lateinit var layoutRunningButtons: LinearLayout

    private lateinit var cardRecent1: MaterialCardView
    private lateinit var cardRecent2: MaterialCardView
    private lateinit var cardRecent3: MaterialCardView
    private lateinit var tvRecentTime1: TextView
    private lateinit var tvRecentTime2: TextView
    private lateinit var tvRecentTime3: TextView
    private lateinit var btnDeleteRecent1: ImageButton
    private lateinit var btnDeleteRecent2: ImageButton
    private lateinit var btnDeleteRecent3: ImageButton

    private var countDownTimer: CountDownTimer? = null
    private var totalTimeInMillis: Long = 0
    private var timeLeftInMillis: Long = 0
    private var isTimerRunning = false
    private var selectedSoundUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private var recentTimes = mutableListOf<Long>()

    private val soundPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? =
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (uri != null) {
                    selectedSoundUri = uri
                    Toast.makeText(requireContext(), "Đã chọn âm thanh", Toast.LENGTH_SHORT).show()
                }
            }
        }

    companion object {
        private const val CHANNEL_ID = "COUNTDOWN_CHANNEL"
        private const val NOTIFICATION_ID = 1001
        private const val PREFS_NAME = "CountdownPrefs"
        private const val KEY_RECENT_TIMES = "recent_times"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_countdown, container, false)

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadRecentTimes()

        createNotificationChannel()

        rootLayout = view as ConstraintLayout
        layoutTimerContainer = view.findViewById(R.id.layoutTimerContainer)
        layoutSetupExtras = view.findViewById(R.id.layoutSetupExtras)

        progressCountdown = view.findViewById(R.id.progressCountdown)

        npHours = view.findViewById(R.id.npHours)
        npMinutes = view.findViewById(R.id.npMinutes)
        npSeconds = view.findViewById(R.id.npSeconds)

        btnStart = view.findViewById(R.id.btnStartCountdown)
        btnDelete = view.findViewById(R.id.btnDeleteCountdown)
        btnPauseResume = view.findViewById(R.id.btnPauseResumeCountdown)
        btnSelectSound = view.findViewById(R.id.btnSelectSound)
        layoutRunningButtons = view.findViewById(R.id.layoutRunningButtons)

        cardRecent1 = view.findViewById(R.id.cardRecent1)
        cardRecent2 = view.findViewById(R.id.cardRecent2)
        cardRecent3 = view.findViewById(R.id.cardRecent3)
        tvRecentTime1 = view.findViewById(R.id.tvRecentTime1)
        tvRecentTime2 = view.findViewById(R.id.tvRecentTime2)
        tvRecentTime3 = view.findViewById(R.id.tvRecentTime3)
        btnDeleteRecent1 = view.findViewById(R.id.btnDeleteRecent1)
        btnDeleteRecent2 = view.findViewById(R.id.btnDeleteRecent2)
        btnDeleteRecent3 = view.findViewById(R.id.btnDeleteRecent3)

        setupNumberPickers()
        updateRecentCardsUI()

        btnStart.setOnClickListener { startTimer() }

        btnDelete.setOnClickListener { resetToSetup() }

        btnPauseResume.setOnClickListener {
            if (isTimerRunning) {
                pauseTimer()
            } else {
                resumeTimer()
            }
        }

        btnSelectSound.setOnClickListener { pickSound() }

        cardRecent1.setOnClickListener { useRecentTime(0) }
        cardRecent2.setOnClickListener { useRecentTime(1) }
        cardRecent3.setOnClickListener { useRecentTime(2) }

        btnDeleteRecent1.setOnClickListener { deleteRecentTime(0) }
        btnDeleteRecent2.setOnClickListener { deleteRecentTime(1) }
        btnDeleteRecent3.setOnClickListener { deleteRecentTime(2) }

        return view
    }

    private fun pickSound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn âm thanh báo thức")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedSoundUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }
        soundPickerLauncher.launch(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Countdown Timer"
            val descriptionText = "Notifications for finished timers"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(selectedSoundUri, audioAttributes)
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        createNotificationChannel()

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Countdown")
            .setContentText("Hết giờ!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(selectedSoundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        with(NotificationManagerCompat.from(requireContext())) {
            try {
                notify(NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        vibrate()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
        }
    }

    private fun setupNumberPickers() {
        npHours.minValue = 0
        npHours.maxValue = 24
        npHours.wrapSelectorWheel = true
        
        npMinutes.minValue = 0
        npMinutes.maxValue = 59
        npMinutes.wrapSelectorWheel = true
        
        npSeconds.minValue = 0
        npSeconds.maxValue = 59
        npSeconds.wrapSelectorWheel = true

        val formatter = NumberPicker.Formatter { String.format("%02d", it) }
        npHours.setFormatter(formatter)
        npMinutes.setFormatter(formatter)
        npSeconds.setFormatter(formatter)
    }

    private fun saveRecentTime(millis: Long) {
        if (millis <= 0) return
        recentTimes.remove(millis)
        recentTimes.add(0, millis)
        if (recentTimes.size > 3) {
            recentTimes = recentTimes.subList(0, 3)
        }
        val json = gson.toJson(recentTimes)
        sharedPreferences.edit().putString(KEY_RECENT_TIMES, json).apply()
        updateRecentCardsUI()
    }

    private fun loadRecentTimes() {
        val json = sharedPreferences.getString(KEY_RECENT_TIMES, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Long>>() {}.type
            recentTimes = gson.fromJson(json, type)
        }
    }

    private fun deleteRecentTime(index: Int) {
        if (index < recentTimes.size) {
            recentTimes.removeAt(index)
            val json = gson.toJson(recentTimes)
            sharedPreferences.edit().putString(KEY_RECENT_TIMES, json).apply()
            updateRecentCardsUI()
        }
    }

    private fun updateRecentCardsUI() {
        val textViews = listOf(tvRecentTime1, tvRecentTime2, tvRecentTime3)
        val cards = listOf(cardRecent1, cardRecent2, cardRecent3)

        for (i in 0 until 3) {
            if (i < recentTimes.size) {
                val millis = recentTimes[i]
                val h = (millis / 1000) / 3600
                val m = ((millis / 1000) % 3600) / 60
                val s = (millis / 1000) % 60
                textViews[i].text = String.format(Locale.getDefault(), "%02d : %02d : %02d", h, m, s)
                cards[i].visibility = View.VISIBLE
            } else {
                cards[i].visibility = View.GONE
            }
        }
    }

    private fun useRecentTime(index: Int) {
        if (index < recentTimes.size) {
            val millis = recentTimes[index]
            val h = (millis / 1000) / 3600
            val m = ((millis / 1000) % 3600) / 60
            val s = (millis / 1000) % 60
            
            npHours.value = h.toInt()
            npMinutes.value = m.toInt()
            npSeconds.value = s.toInt()
        }
    }

    private fun startTimer() {
        val h = npHours.value.toLong()
        val m = npMinutes.value.toLong()
        val s = npSeconds.value.toLong()
        totalTimeInMillis = (h * 3600 + m * 60 + s) * 1000
        timeLeftInMillis = totalTimeInMillis

        if (timeLeftInMillis <= 0) {
            Toast.makeText(context, "Vui lòng nhập thời gian hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        saveRecentTime(totalTimeInMillis)

        val transition = AutoTransition().apply {
            duration = 400
        }
        TransitionManager.beginDelayedTransition(rootLayout, transition)

        // Di chuyển bộ đếm vào giữa
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)
        constraintSet.connect(layoutTimerContainer.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(layoutTimerContainer.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.applyTo(rootLayout)

        layoutSetupExtras.visibility = View.GONE
        btnStart.visibility = View.GONE

        layoutRunningButtons.visibility = View.VISIBLE
        progressCountdown.visibility = View.VISIBLE
        
        npHours.isEnabled = false
        npMinutes.isEnabled = false
        npSeconds.isEnabled = false

        progressCountdown.max = totalTimeInMillis.toInt()

        runTimer()
    }

    private fun runTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 10) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateUI()
            }

            override fun onFinish() {
                isTimerRunning = false
                timeLeftInMillis = 0
                updateUI()
                showNotification()
                resetToSetup()
            }
        }.start()

        isTimerRunning = true
        btnPauseResume.text = "Tạm dừng"
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        btnPauseResume.text = "Tiếp tục"
    }

    private fun resumeTimer() {
        runTimer()
    }

    private fun resetToSetup() {
        countDownTimer?.cancel()
        isTimerRunning = false
        timeLeftInMillis = 0

        val transition = AutoTransition().apply {
            duration = 400
        }
        TransitionManager.beginDelayedTransition(rootLayout, transition)

        // Đưa bộ đếm về vị trí cũ (trên cùng)
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)
        constraintSet.clear(layoutTimerContainer.id, ConstraintSet.BOTTOM)
        constraintSet.connect(layoutTimerContainer.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 32 * resources.displayMetrics.density.toInt())
        constraintSet.applyTo(rootLayout)

        layoutRunningButtons.visibility = View.GONE
        progressCountdown.visibility = View.INVISIBLE
        layoutSetupExtras.visibility = View.VISIBLE
        btnStart.visibility = View.VISIBLE
        
        npHours.isEnabled = true
        npMinutes.isEnabled = true
        npSeconds.isEnabled = true
    }

    private fun updateUI() {
        val hours = (timeLeftInMillis / 1000) / 3600
        val minutes = ((timeLeftInMillis / 1000) % 3600) / 60
        val seconds = (timeLeftInMillis / 1000) % 60

        npHours.value = hours.toInt()
        npMinutes.value = minutes.toInt()
        npSeconds.value = seconds.toInt()

        progressCountdown.progress = (totalTimeInMillis - timeLeftInMillis).toInt()
    }
}
