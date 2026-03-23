package com.example.alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class AccountFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        dbHelper = DatabaseHelper(requireContext())
        
        val tvUsername = view.findViewById<TextView>(R.id.tvAccountUsername)
        val tvAlarmCount = view.findViewById<TextView>(R.id.tvAlarmCount)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("current_user", "Người dùng")
        
        tvUsername.text = username

        val alarms = dbHelper.getAlarmsByUser(username ?: "")
        tvAlarmCount.text = "Số lượng báo thức: ${alarms.size}"

        btnLogout.setOnClickListener {
            // Xóa user hiện tại
            sharedPreferences.edit().remove("current_user").apply()
            
            // Quay lại màn hình đăng nhập
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
