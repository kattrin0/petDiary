package com.example.petDiary.data.repository

import android.content.Context
import com.example.petDiary.data.datasource.ThemeDataSource

class ThemeRepository(private val context: Context) {
    private val dataSource = ThemeDataSource(context)

    fun getThemeMode(): Int {
        return dataSource.getThemeMode()
    }

    fun saveThemeMode(mode: Int) {
        dataSource.saveThemeMode(mode)
    }
}

