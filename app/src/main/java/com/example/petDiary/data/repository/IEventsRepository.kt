package com.example.petDiary.domain.repository

import com.example.petDiary.domain.model.Event

interface IEventsRepository {
    suspend fun getAllEvents(): List<Event>
    suspend fun addEvent(event: Event)
    suspend fun updateEvent(event: Event)
    suspend fun deleteEvent(eventId: Long)
    suspend fun getTodayEvents(): List<Event>
    suspend fun getActiveEvents(): List<Event>
    suspend fun removePassedEvents()
}