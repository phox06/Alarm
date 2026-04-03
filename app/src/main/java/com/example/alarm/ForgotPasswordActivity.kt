package com.example.alarm

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var verifiedUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        dbHelper = DatabaseHelper(this)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val btnVerifyEmail = findViewById<Button>(R.id.btnVerifyEmail)
        val layoutResetPassword = findViewById<LinearLayout>(R.id.layoutResetPassword)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmNewPassword = findViewById<EditText>(R.id.etConfirmNewPassword)
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnVerifyEmail.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val username = dbHelper.getUsernameByEmail(email)
            if (username != null) {
                verifiedUsername = username
                layoutResetPassword.visibility = View.VISIBLE
                btnVerifyEmail.visibility = View.GONE
                etEmail.isEnabled = false
                Toast.makeText(this, "Email hợp lệ. Vui lòng nhập mật khẩu mới.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Email không tồn tại trong hệ thống!", Toast.LENGTH_SHORT).show()
            }
        }

        btnResetPassword.setOnClickListener {
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmNewPassword.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ mật khẩu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifiedUsername?.let { username ->
                val result = dbHelper.updatePassword(username, newPassword)
                if (result > 0) {
                    Toast.makeText(this, "Đặt lại mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Đặt lại mật khẩu thất bại!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }
}
