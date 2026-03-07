package com.example.petDiary.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    private const val MAX_IMAGE_SIZE = 500 // Максимальный размер по ширине или высоте
    private const val COMPRESS_QUALITY = 50 // Качество сжатия JPEG (0-100)

    /**
     * Уменьшает размер Bitmap до максимально допустимого
     * Сохраняет пропорции изображения
     */
    fun resizeBitmap(bitmap: Bitmap, maxSize: Int = MAX_IMAGE_SIZE): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Если изображение уже меньше максимального размера, возвращаем оригинал
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        // Вычисляем новые размеры с сохранением пропорций
        val ratio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        // Создаем уменьшенную копию
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Конвертирует Bitmap в Base64 строку с предварительным уменьшением размера
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        // Сначала уменьшаем изображение
        val resizedBitmap = resizeBitmap(bitmap)

        val byteArrayOutputStream = ByteArrayOutputStream()
        // Сжимаем изображение
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        // Если использовали уменьшенную копию и это не оригинал, освобождаем память
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Конвертирует Bitmap в Base64 строку с кастомными параметрами
     */
    fun bitmapToBase64(bitmap: Bitmap, maxSize: Int, quality: Int): String {
        val resizedBitmap = resizeBitmap(bitmap, maxSize)

        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Конвертирует Base64 строку обратно в Bitmap
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Проверяет, является ли строка валидной Base64 строкой изображения
     */
    fun isValidBase64Image(base64String: String?): Boolean {
        if (base64String.isNullOrEmpty()) return false
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            decodedBytes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Проверяет, является ли строка путем к файлу (для обратной совместимости)
     */
    fun isFilePath(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        return path.startsWith("/") || path.contains("pet_photo_")
    }

    /**
     * Получает размер Base64 строки в килобайтах
     */
    fun getBase64SizeInKb(base64String: String): Double {
        return base64String.length * 0.75 / 1024 // Base64 добавляет ~33% накладных расходов
    }
}