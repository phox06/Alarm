package com.example.alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Tự động đăng nhập
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        if (sharedPreferences.contains("current_user")) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)
        dbHelper = DatabaseHelper(this)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnSkip = findViewById<Button>(R.id.btnSkip)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (dbHelper.checkUserLogin(username, password)) {
                sharedPreferences.edit().putString("current_user", username).apply()
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } else {
                Toast.makeText(this, "Sai tên đăng nhập hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        btnSkip.setOnClickListener {
            sharedPreferences.edit().putString("current_user", "Guest").apply()
            Toast.makeText(this, "Sử dụng với quyền khách", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
