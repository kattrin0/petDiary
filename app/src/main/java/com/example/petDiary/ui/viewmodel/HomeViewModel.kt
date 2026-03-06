package com.example.petDiary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.petDiary.data.repository.EventsRepository
import com.example.petDiary.data.repository.PetProfileRepository
import com.example.petDiary.domain.model.Event
import com.example.petDiary.domain.model.PetProfile

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val petProfileRepository = PetProfileRepository(application)
    private val eventsRepository = EventsRepository(application)

    private val _petProfile = MutableLiveData<PetProfile>()
    val petProfile: LiveData<PetProfile> = _petProfile

    private val _todayEvents = MutableLiveData<List<Event>>()
    val todayEvents: LiveData<List<Event>> = _todayEvents

    fun loadPetProfile() {
        _petProfile.value = petProfileRepository.getPetProfile()
    }

    fun loadTodayEvents() {
        val events = eventsRepository.getTodayEvents()
            .sortedBy { it.timeHour * 60 + it.timeMinute }
        _todayEvents.value = events
    }

    fun refreshData() {
        loadPetProfile()
        loadTodayEvents()
    }
}

