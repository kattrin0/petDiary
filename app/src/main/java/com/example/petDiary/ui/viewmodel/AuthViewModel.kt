package com.example.petDiary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.actionCodeSettings
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val prefs = application.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isAuthenticated = MutableLiveData<Boolean>()
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        _isAuthenticated.value = auth.currentUser != null
    }

    fun sendSignInLink(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val actionCodeSettings = actionCodeSettings {
                    url = "https://petdiary-d6042.firebaseapp.com"
                    handleCodeInApp = true
                    setAndroidPackageName("com.example.petDiary", true, "1")
                }

                auth.sendSignInLinkToEmail(email, actionCodeSettings).await()

                // Сохраняем email
                prefs.edit().putString("email_for_signin", email).apply()

                _error.value = "Ссылка отправлена на $email"

            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun handleSignInLink(link: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (auth.isSignInWithEmailLink(link)) {
                    val email = prefs.getString("email_for_signin", null)
                    if (email != null) {
                        auth.signInWithEmailLink(email, link).await()
                        _isAuthenticated.postValue(auth.currentUser != null)
                        _error.value = "Вход выполнен успешно!"
                    } else {
                        _error.value = "Email не найден"
                    }
                } else {
                    _error.value = "Неверная ссылка"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка входа: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.signOut()
                checkAuthState()
                prefs.edit().remove("email_for_signin").apply()
                _error.value = "Вы вышли из аккаунта"
            } catch (e: Exception) {
                _error.value = "Ошибка при выходе: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}