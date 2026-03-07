package com.example.petDiary.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.petDiary.R
import com.example.petDiary.ui.viewmodel.AuthViewModel
import com.example.petDiary.ui.viewmodel.MainViewModel

class SettingsDialog : DialogFragment() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var mainViewModel: MainViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val builder = AlertDialog.Builder(requireContext())
        val items = arrayOf("Светлая тема", "Темная тема", "Системная тема", "Выйти из аккаунта")

        builder.setTitle("Настройки")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> mainViewModel.saveThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                    1 -> mainViewModel.saveThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
                    2 -> mainViewModel.saveThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    3 -> showLogoutConfirmation()
                }
            }

        return builder.create()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Да") { _, _ ->
                authViewModel.signOut()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    companion object {
        const val TAG = "SettingsDialog"
    }
}