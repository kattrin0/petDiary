package com.example.petDiary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.petDiary.data.repository.FirestorePetProfileRepository
import com.example.petDiary.data.repository.LocalPetProfileRepository
import com.example.petDiary.domain.model.PetProfile
import com.example.petDiary.domain.repository.IPetProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IPetProfileRepository = if (FirebaseAuth.getInstance().currentUser != null) {
        FirestorePetProfileRepository()
    } else {
        LocalPetProfileRepository(application)
    }

    private val _petProfile = MutableLiveData<PetProfile>()
    val petProfile: LiveData<PetProfile> = _petProfile

    private val _hasSavedData = MutableLiveData<Boolean>()
    val hasSavedData: LiveData<Boolean> = _hasSavedData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadPetProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = repository.getPetProfile()
                _petProfile.value = profile ?: PetProfile()
                _hasSavedData.value = profile != null
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun savePetProfile(profile: PetProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.savePetProfile(profile)
                _petProfile.value = profile
                _hasSavedData.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Ошибка сохранения: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshProfile() {
        loadPetProfile()
    }

    fun clearError() {
        _error.value = null
    }
}