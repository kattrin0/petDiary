package com.example.petDiary.data.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class YandexDiskService(private val context: Context) {

    private val TAG = "YandexDiskService"
    init {
        Log.d(TAG, "========== YandexDiskService ИНИЦИАЛИЗИРОВАН ==========")
        Log.d(TAG, "Токен (первые 10 символов): ${OAUTH_TOKEN.take(10)}...")
        Log.d(TAG, "Токен длина: ${OAUTH_TOKEN.length}")
        Log.d(TAG, "BASE_URL: $BASE_URL")
        Log.d(TAG, "APP_FOLDER: $APP_FOLDER")
    }

    init {
        Log.d(TAG, "YandexDiskService создан")
        Log.d(TAG, "Токен (первые 10 символов): ${OAUTH_TOKEN.take(10)}...")
    }
    companion object {

        private const val OAUTH_TOKEN = "y0__xDR2YD2Axi11T4g9vWB1RYwpfzn6QfhNewQKFNv49Q_7oKf2YNNhIysdQ" // Замените на ваш реальный токен

        // Базовая папка для фото приложения
        private const val APP_FOLDER = "petdiary_photos"

        // API endpoints
        private const val BASE_URL = "https://cloud-api.yandex.net/v1/disk"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Загрузка фото на Яндекс.Диск
     */
    suspend fun uploadPhoto(localPath: String): String? =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "========== ЗАГРУЗКА ФОТО ==========")
                Log.d(TAG, "localPath: $localPath")

                val file = File(localPath)
                Log.d(TAG, "Файл существует: ${file.exists()}")
                Log.d(TAG, "Размер файла: ${file.length()} байт")

                if (!file.exists()) {
                    Log.e(TAG, "✗ Файл не найден: $localPath")
                    return@withContext null
                }

                // Проверяем токен
                Log.d(TAG, "Используем токен: ${OAUTH_TOKEN.take(10)}...")

                // 1. Создаем папку
                Log.d(TAG, "Шаг 1: создаем папку...")
                createFolder()

                // 2. Получаем ссылку для загрузки
                val fileName = "${System.currentTimeMillis()}.jpg"
                val remotePath = "$APP_FOLDER/$fileName"
                Log.d(TAG, "Шаг 2: получаем ссылку для remotePath: $remotePath")

                val uploadLink = getUploadLink(remotePath)
                if (uploadLink == null) {
                    Log.e(TAG, "✗ Не удалось получить ссылку для загрузки")
                    return@withContext null
                }
                Log.d(TAG, "✓ Получена ссылка: ${uploadLink.take(50)}...")

                // 3. Загружаем файл
                Log.d(TAG, "Шаг 3: загружаем файл...")
                val uploadSuccess = uploadFile(uploadLink, file)
                if (!uploadSuccess) {
                    Log.e(TAG, "✗ Ошибка загрузки файла")
                    return@withContext null
                }
                Log.d(TAG, "✓ Файл загружен")

                // 4. Публикуем файл
                Log.d(TAG, "Шаг 4: публикуем файл и получаем ссылку...")
                val publicUrl = publishFile(remotePath)

                if (publicUrl != null) {
                    Log.d(TAG, "✓ Файл загружен, публичная ссылка: $publicUrl")
                    return@withContext publicUrl
                } else {
                    Log.e(TAG, "✗ Не удалось получить публичную ссылку")
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e(TAG, "✗ Ошибка загрузки", e)
                null
            }
        }

    /**
     * Создание папки на Яндекс.Диске
     */
    private suspend fun createFolder() {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/resources?path=$APP_FOLDER")
                .put(RequestBody.create(null, ""))
                .addHeader("Authorization", "OAuth $OAUTH_TOKEN")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 409) { // 409 = уже существует
                Log.d(TAG, "✓ Папка создана или уже существует")
            } else {
                Log.e(TAG, "✗ Ошибка создания папки: ${response.code}")
            }
            response.close()
        } catch (e: Exception) {
            Log.e(TAG, "✗ Ошибка создания папки", e)
        }
    }
    suspend fun testConnection() {
        Log.d(TAG, "========== ТЕСТ ПОДКЛЮЧЕНИЯ ==========")
        Log.d(TAG, "Токен: ${OAUTH_TOKEN.take(10)}...")

        try {
            val request = Request.Builder()
                .url("$BASE_URL")
                .get()
                .addHeader("Authorization", "OAuth $OAUTH_TOKEN")
                .build()

            val response = client.newCall(request).execute()
            Log.d(TAG, "Тест подключения: код ответа ${response.code}")
            Log.d(TAG, "Успешно: ${response.isSuccessful}")
            response.close()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка теста подключения", e)
        }
    }

    /**
     * Получение ссылки для загрузки файла
     */
    private suspend fun getUploadLink(remotePath: String): String? {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/resources/upload?path=$remotePath&overwrite=true")
                .get()
                .addHeader("Authorization", "OAuth $OAUTH_TOKEN")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Ошибка получения ссылки: ${response.code}")
                    return@use null
                }

                val json = JSONObject(response.body?.string() ?: return@use null)
                val href = json.getString("href")
                Log.d(TAG, "Получена ссылка для загрузки")
                href
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Ошибка получения ссылки", e)
            null
        }
    }

    /**
     * Загрузка файла по полученной ссылке
     */
    private suspend fun uploadFile(uploadLink: String, file: File): Boolean {
        return try {
            val request = Request.Builder()
                .url(uploadLink)
                .put(file.asRequestBody("image/jpeg".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "✓ Файл загружен")
                    true
                } else {
                    Log.e(TAG, "✗ Ошибка загрузки файла: ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Ошибка загрузки файла", e)
            false
        }
    }

    /**
     * Публикация файла и получение публичной ссылки
     */
    private suspend fun publishFile(remotePath: String): String? {
        return try {
            // Публикуем файл
            val publishRequest = Request.Builder()
                .url("$BASE_URL/resources/publish?path=$remotePath")
                .put(RequestBody.create(null, ""))
                .addHeader("Authorization", "OAuth $OAUTH_TOKEN")
                .build()

            client.newCall(publishRequest).execute().close()

            // Ждем немного для обработки
            Thread.sleep(500)

            // Получаем информацию о файле с публичной ссылкой
            val infoRequest = Request.Builder()
                .url("$BASE_URL/resources?path=$remotePath")
                .get()
                .addHeader("Authorization", "OAuth $OAUTH_TOKEN")
                .build()

            client.newCall(infoRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Ошибка получения информации: ${response.code}")
                    return@use null
                }

                val json = JSONObject(response.body?.string() ?: return@use null)
                val publicUrl = json.optString("public_url")
                if (publicUrl.isNullOrEmpty()) {
                    Log.e(TAG, "Публичная ссылка не найдена")
                    null
                } else {
                    publicUrl
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Ошибка публикации", e)
            null
        }
    }

}