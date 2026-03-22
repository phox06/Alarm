package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
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
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.Locale

class CountdownFragment : Fragment() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var layoutSetup: ConstraintLayout
    private lateinit var layoutRunning: ConstraintLayout

    private lateinit var tvCountdown: TextView
    private lateinit var progressCountdown: CircularProgressIndicator

    private lateinit var npHours: NumberPicker
    private lateinit var npMinutes: NumberPicker
    private lateinit var npSeconds: NumberPicker

    private lateinit var btnStart: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var btnPauseResume: MaterialButton
    private lateinit var layoutRunningButtons: LinearLayout

    private lateinit var cardPreset1: MaterialCardView
    private lateinit var cardPreset2: MaterialCardView
    private lateinit var cardPreset3: MaterialCardView

    private var countDownTimer: CountDownTimer? = null
    private var totalTimeInMillis: Long = 0
    private var timeLeftInMillis: Long = 0
    private var isTimerRunning = false

    companion object {
        private const val CHANNEL_ID = "COUNTDOWN_CHANNEL"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_countdown, container, false)

        createNotificationChannel()

        rootLayout = view as ConstraintLayout
        layoutSetup = view.findViewById(R.id.layoutSetup)
        layoutRunning = view.findViewById(R.id.layoutRunning)

        tvCountdown = view.findViewById(R.id.tvCountdown)
        progressCountdown = view.findViewById(R.id.progressCountdown)

        npHours = view.findViewById(R.id.npHours)
        npMinutes = view.findViewById(R.id.npMinutes)
        npSeconds = view.findViewById(R.id.npSeconds)

        btnStart = view.findViewById(R.id.btnStartCountdown)
        btnDelete = view.findViewById(R.id.btnDeleteCountdown)
        btnPauseResume = view.findViewById(R.id.btnPauseResumeCountdown)
        layoutRunningButtons = view.findViewById(R.id.layoutRunningButtons)

        cardPreset1 = view.findViewById(R.id.cardPreset1)
        cardPreset2 = view.findViewById(R.id.cardPreset2)
        cardPreset3 = view.findViewById(R.id.cardPreset3)

        setupNumberPickers()

        btnStart.setOnClickListener { startTimer() }

        btnDelete.setOnClickListener { resetToSetup() }

        btnPauseResume.setOnClickListener {
            if (isTimerRunning) {
                pauseTimer()
            } else {
                resumeTimer()
            }
        }

        cardPreset1.setOnClickListener { setPresetTime(10) }
        cardPreset2.setOnClickListener { setPresetTime(15) }
        cardPreset3.setOnClickListener { setPresetTime(30) }

        return view
    }

    private fun getSelectedSoundUri(): Uri? {
        return (activity as? MainActivity)?.getSelectedSoundUri()
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
                setSound(getSelectedSoundUri(), audioAttributes)
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
            .setSound(getSelectedSoundUri())
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
        npHours.maxValue = 99
        npMinutes.minValue = 0
        npMinutes.maxValue = 59
        npSeconds.minValue = 0
        npSeconds.maxValue = 59

        val formatter = NumberPicker.Formatter { String.format("%02d", it) }
        npHours.setFormatter(formatter)
        npMinutes.setFormatter(formatter)
        npSeconds.setFormatter(formatter)
    }

    private fun setPresetTime(minutes: Int) {
        npHours.value = 0
        npMinutes.value = minutes
        npSeconds.value = 0
    }

    private fun startTimer() {
        val h = npHours.value.toLong()
        val m = npMinutes.value.toLong()
        val s = npSeconds.value.toLong()
        totalTimeInMillis = (h * 3600 + m * 60 + s) * 1000
        timeLeftInMillis = totalTimeInMillis

        if (timeLeftInMillis <= 0) {
            Toast.makeText(context, "Please enter a valid time", Toast.LENGTH_SHORT).show()
            return
        }

        val transition = AutoTransition().apply {
            duration = 300
        }
        TransitionManager.beginDelayedTransition(rootLayout, transition)

        layoutSetup.visibility = View.GONE
        btnStart.visibility = View.GONE

        layoutRunning.visibility = View.VISIBLE
        layoutRunningButtons.visibility = View.VISIBLE

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
            duration = 300
        }
        TransitionManager.beginDelayedTransition(rootLayout, transition)

        layoutRunning.visibility = View.GONE
        layoutRunningButtons.visibility = View.GONE

        layoutSetup.visibility = View.VISIBLE
        btnStart.visibility = View.VISIBLE
    }

    private fun updateUI() {
        val hours = (timeLeftInMillis / 1000) / 3600
        val minutes = ((timeLeftInMillis / 1000) % 3600) / 60
        val seconds = (timeLeftInMillis / 1000) % 60

        val timeString = if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }

        tvCountdown.text = timeString
        progressCountdown.progress = (totalTimeInMillis - timeLeftInMillis).toInt()
    }
}
