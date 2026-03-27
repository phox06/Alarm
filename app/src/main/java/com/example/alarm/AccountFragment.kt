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
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
                // Lưu quyền truy cập Uri lâu dài
                val contentResolver = requireContext().contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(selectedImageUri, takeFlags)

                // Cập nhật giao diện và database
                ivAvatar.setImageURI(selectedImageUri)
                dbHelper.updateAvatar(currentUsername, selectedImageUri.toString())
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

        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        currentUsername = sharedPreferences.getString("current_user", "") ?: ""
        
        tvUsername.text = currentUsername

        // Load avatar từ database
        val avatarUriString = dbHelper.getAvatar(currentUsername)
        if (avatarUriString != null) {
            ivAvatar.setImageURI(Uri.parse(avatarUriString))
        }

        val alarms = dbHelper.getAlarmsByUser(currentUsername)
        tvAlarmCount.text = "Số lượng báo thức: ${alarms.size}"

        // Click vào ảnh để chọn ảnh mới
        ivAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            selectImageLauncher.launch(intent)
        }

        btnLogout.setOnClickListener {
            sharedPreferences.edit().remove("current_user").apply()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
