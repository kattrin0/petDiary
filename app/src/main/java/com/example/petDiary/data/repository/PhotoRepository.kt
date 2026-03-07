package com.example.petDiary.data.repository

import android.content.Context
import android.util.Log
import com.example.petDiary.data.service.YandexDiskService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PhotoRepository(private val context: Context) {

    private val TAG = "PhotoRepository"
    private val yandexDiskService = YandexDiskService(context)

    companion object {
        private const val PHOTO_DIR = "pet_photos"
        private const val PREFS_NAME = "photo_mapping"
    }

    /**
     * Сохранение фото: локально для всех, в облако только для авторизованных
     */
    suspend fun savePhoto(localPath: String, isAuthorized: Boolean): String? =
        withContext(Dispatchers.IO) {

            try {
                Log.d(TAG, "savePhoto: $localPath, isAuthorized: $isAuthorized")

                // 1. Проверяем исходный файл
                val sourceFile = File(localPath)
                if (!sourceFile.exists()) {
                    Log.e(TAG, "Файл не существует: $localPath")
                    return@withContext null
                }

                // 2. Всегда сохраняем локальную копию
                val localFileName = saveLocalCopy(localPath)
                Log.d(TAG, "Локальная копия: $localFileName")

                // 3. Для авторизованных пользователей загружаем в облако
                if (isAuthorized) {
                    Log.d(TAG, "Загрузка на Яндекс.Диск...")
                    yandexDiskService.testConnection() // ТЕСТ
                    val cloudUrl = yandexDiskService.uploadPhoto(localPath)

                    if (cloudUrl != null) {
                        Log.d(TAG, "Загружено на Яндекс.Диск: $cloudUrl")
                        savePhotoMapping(localFileName, cloudUrl)
                        return@withContext cloudUrl
                    } else {
                        Log.e(TAG, "Ошибка загрузки на Яндекс.Диск")
                    }
                }

                // Для неавторизованных или при ошибке - возвращаем локальный путь
                return@withContext localFileName

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка в savePhoto", e)
                null
            }
        }

    /**
     * Сохранение локальной копии файла
     */
    private fun saveLocalCopy(sourcePath: String): String {
        val photoDir = File(context.filesDir, PHOTO_DIR)
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }

        val fileName = "photo_${System.currentTimeMillis()}.jpg"
        val destination = File(photoDir, fileName)

        File(sourcePath).copyTo(destination, overwrite = true)
        Log.d(TAG, "Сохранено локально: ${destination.absolutePath}")

        return destination.absolutePath
    }

    /**
     * Сохранение соответствия между локальным файлом и облачным URL
     */
    private fun savePhotoMapping(localName: String, cloudUrl: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(localName, cloudUrl).apply()
    }

    /**
     * Получение пути к фото (для загрузки из облака при необходимости)
     */
    suspend fun getPhotoPath(photoIdentifier: String): String? =
        withContext(Dispatchers.IO) {
            try {
                // Если это URL с Яндекс.Диска
                if (photoIdentifier.startsWith("https://")) {
                    Log.d(TAG, "Это URL с Яндекс.Диска, возвращаем как есть")
                    // Glide умеет загружать по URL, возвращаем оригинал
                    return@withContext photoIdentifier
                }

                // Если это локальный файл
                val localFile = File(photoIdentifier)
                if (localFile.exists()) {
                    return@withContext localFile.absolutePath
                }

                return@withContext null

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка получения фото", e)
                null
            }
        }
}