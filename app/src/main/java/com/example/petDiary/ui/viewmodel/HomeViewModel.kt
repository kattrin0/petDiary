package com.example.petDiary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.petDiary.data.repository.EventsRepository
import com.example.petDiary.data.repository.LocalPetProfileRepository  // Импортируем локальный репозиторий
import com.example.petDiary.data.repository.FirestorePetProfileRepository
import com.example.petDiary.domain.model.Event
import com.example.petDiary.domain.model.PetProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // Выбираем репозиторий в зависимости от авторизации
    private val petProfileRepository = if (FirebaseAuth.getInstance().currentUser != null) {
        FirestorePetProfileRepository()
    } else {
        LocalPetProfileRepository(application)
    }

    private val eventsRepository = EventsRepository(application)

    private val _petProfile = MutableLiveData<PetProfile>()
    val petProfile: LiveData<PetProfile> = _petProfile

    private val _todayEvents = MutableLiveData<List<Event>>()
    val todayEvents: LiveData<List<Event>> = _todayEvents

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadPetProfile() {
        viewModelScope.launch {  // <-- Запускаем корутину
            _isLoading.value = true
            try {
                val profile = petProfileRepository.getPetProfile()
                _petProfile.value = profile ?: PetProfile()
            } catch (e: Exception) {
                e.printStackTrace()
                _petProfile.value = PetProfile()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTodayEvents() {
        // Эта функция не suspend, поэтому корутина не нужна
        val events = eventsRepository.getTodayEvents()
            .sortedBy { it.timeHour * 60 + it.timeMinute }
        _todayEvents.value = events
    }

    fun refreshData() {
        loadPetProfile()  // Теперь работает через корутину
        loadTodayEvents()
    }
}