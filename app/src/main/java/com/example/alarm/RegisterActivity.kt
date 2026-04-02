package com.example.alarm

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kiểm tra xem user đã tồn tại chưa
            if (dbHelper.checkUser(username)) {
                Toast.makeText(this, "Tên đăng nhập đã tồn tại!", Toast.LENGTH_SHORT).show()
            } else if (dbHelper.checkUserByEmail(email)) {
                Toast.makeText(this, "Email này đã được sử dụng!", Toast.LENGTH_SHORT).show()
            } else {
                val result = dbHelper.addUser(username, password, email)
                if (result != -1L) {
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                    finish() // Quay lại màn hình đăng nhập
                } else {
                    Toast.makeText(this, "Đăng ký thất bại!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }
}
