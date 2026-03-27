package com.example.alarm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class AccountFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var ivAvatar: ImageView
    private var currentUsername: String = ""

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                try {
                    val contentResolver = requireContext().contentResolver
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(selectedImageUri, takeFlags)

                    ivAvatar.setImageURI(selectedImageUri)
                    dbHelper.updateAvatar(currentUsername, selectedImageUri.toString())
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Không thể lưu ảnh này", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        dbHelper = DatabaseHelper(requireContext())
        
        ivAvatar = view.findViewById(R.id.ivAvatar)
        val tvUsername = view.findViewById<TextView>(R.id.tvAccountUsername)
        val tvAlarmCount = view.findViewById<TextView>(R.id.tvAlarmCount)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        val btnChangePassword = view.findViewById<Button>(R.id.btnChangePassword)

        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        currentUsername = sharedPreferences.getString("current_user", "") ?: ""
        
        if (currentUsername == "Guest") {
            tvUsername.text = "Bạn đang dùng quyền khách"
            btnChangePassword.visibility = View.GONE
            btnLogout.text = "Đăng nhập / Đăng ký"
            btnLogout.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark, null))
        } else {
            tvUsername.text = currentUsername
            btnChangePassword.visibility = View.VISIBLE
            btnLogout.text = "Đăng xuất"
            btnLogout.setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
        }

        // Load avatar
        val avatarUriString = dbHelper.getAvatar(currentUsername)
        if (avatarUriString != null) {
            try {
                ivAvatar.setImageURI(Uri.parse(avatarUriString))
            } catch (e: Exception) {
                ivAvatar.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }

        val alarms = dbHelper.getAlarmsByUser(currentUsername)
        tvAlarmCount.text = "Số lượng báo thức: ${alarms.size}"

        ivAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            selectImageLauncher.launch(intent)
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        btnLogout.setOnClickListener {
            sharedPreferences.edit().remove("current_user").apply()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etOld = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNew = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = dialogView.findViewById<EditText>(R.id.etConfirmNewPassword)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Cập nhật") { dialog, _ ->
                val oldPass = etOld.text.toString()
                val newPass = etNew.text.toString()
                val confirmPass = etConfirm.text.toString()

                if (dbHelper.checkUserLogin(currentUsername, oldPass)) {
                    if (newPass == confirmPass && newPass.isNotEmpty()) {
                        dbHelper.updatePassword(currentUsername, newPass)
                        Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Mật khẩu mới không khớp!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Mật khẩu cũ không chính xác!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
