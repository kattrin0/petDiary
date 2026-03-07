package com.example.petDiary.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.petDiary.data.repository.EventsRepository
import com.example.petDiary.data.service.NotificationService
import com.example.petDiary.domain.model.Event
import kotlinx.coroutines.launch

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val eventsRepository = EventsRepository(application)
    private val notificationService = NotificationService(application)

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events

    private val _todayDate = MutableLiveData<String>()
    val todayDate: LiveData<String> = _todayDate

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        notificationService.createNotificationChannel()
        loadTodayDate()
        refreshEvents()
    }

    private fun loadTodayDate() {
        val dateFormat = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("ru"))
        val today = dateFormat.format(java.util.Date())
        _todayDate.value = "Сегодня: $today"
    }

    fun refreshEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                eventsRepository.removePassedEvents()
                val activeEvents = eventsRepository.getActiveEvents()
                _events.value = activeEvents
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки событий: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addEvent(event: Event) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                eventsRepository.addEvent(event)
                if (event.time?.isNotEmpty() == true && event.timeHour > 0) {
                    notificationService.scheduleNotification(event)
                }
                refreshEvents()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Ошибка добавления события: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                eventsRepository.updateEvent(event)
                refreshEvents()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Ошибка обновления события: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                notificationService.cancelNotification(event)
                eventsRepository.deleteEvent(event.id)
                refreshEvents()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Ошибка удаления события: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleEventComplete(event: Event) {
        val updatedEvent = event.copy(isCompleted = !event.isCompleted)
        updateEvent(updatedEvent)
    }

    fun clearError() {
        _error.value = null
    }
}