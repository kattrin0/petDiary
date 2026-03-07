package com.example.petDiary.data.repository

import android.content.Context
import com.example.petDiary.domain.model.Event
import com.example.petDiary.domain.repository.IEventsRepository
import com.google.firebase.auth.FirebaseAuth

class EventsRepository(
    private val context: Context
) : IEventsRepository {

    private val repository: IEventsRepository by lazy {
        if (FirebaseAuth.getInstance().currentUser != null) {
            FirestoreEventsRepository()
        } else {
            LocalEventsRepository(context)
        }
    }

    override suspend fun getAllEvents(): List<Event> = repository.getAllEvents()

    override suspend fun addEvent(event: Event) = repository.addEvent(event)

    override suspend fun updateEvent(event: Event) = repository.updateEvent(event)

    override suspend fun deleteEvent(eventId: Long) = repository.deleteEvent(eventId)

    override suspend fun getTodayEvents(): List<Event> = repository.getTodayEvents()

    override suspend fun getActiveEvents(): List<Event> = repository.getActiveEvents()

    override suspend fun removePassedEvents() = repository.removePassedEvents()
}