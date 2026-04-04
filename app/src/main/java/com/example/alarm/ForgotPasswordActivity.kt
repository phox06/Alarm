package com.example.alarm

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var verifiedUsername: String? = null
    private var generatedCode: String? = null
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        dbHelper = DatabaseHelper(this)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val btnSendCode = findViewById<Button>(R.id.btnSendCode)
        
        val layoutVerifyCode = findViewById<LinearLayout>(R.id.layoutVerifyCode)
        val etVerificationCode = findViewById<EditText>(R.id.etVerificationCode)
        val btnVerifyCode = findViewById<Button>(R.id.btnVerifyCode)

        val layoutResetPassword = findViewById<LinearLayout>(R.id.layoutResetPassword)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmNewPassword = findViewById<EditText>(R.id.etConfirmNewPassword)
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnSendCode.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val username = dbHelper.getUsernameByEmail(email)
            if (username != null) {
                verifiedUsername = username
                userEmail = email
                
                // Tạo mã xác nhận 6 số
                val code = (100000..999999).random().toString()
                generatedCode = code

                btnSendCode.isEnabled = false
                btnSendCode.text = "Đang gửi..."

                EmailSender.sendVerificationCode(email, code) { success ->
                    runOnUiThread {
                        if (success) {
                            layoutVerifyCode.visibility = View.VISIBLE
                            etEmail.isEnabled = false
                            btnSendCode.visibility = View.GONE
                            Toast.makeText(this, "Mã xác nhận đã được gửi đến email của bạn.", Toast.LENGTH_LONG).show()
                        } else {
                            btnSendCode.isEnabled = true
                            btnSendCode.text = "Gửi mã xác nhận"
                            Toast.makeText(this, "Gửi email thất bại. Vui lòng thử lại sau!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Email không tồn tại trong hệ thống!", Toast.LENGTH_SHORT).show()
            }
        }

        btnVerifyCode.setOnClickListener {
            val inputCode = etVerificationCode.text.toString().trim()
            if (inputCode == generatedCode) {
                layoutVerifyCode.visibility = View.GONE
                layoutResetPassword.visibility = View.VISIBLE
                Toast.makeText(this, "Xác nhận thành công! Vui lòng đặt mật khẩu mới.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Mã xác nhận không chính xác!", Toast.LENGTH_SHORT).show()
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
