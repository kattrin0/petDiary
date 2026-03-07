package com.example.petDiary.data.repository

import android.content.Context
import com.example.petDiary.data.datasource.EventsDataSource
import com.example.petDiary.domain.model.Event
import com.example.petDiary.domain.repository.IEventsRepository
import java.util.*

class LocalEventsRepository(
    private val context: Context
) : IEventsRepository {

    private val dataSource = EventsDataSource(context)

    override suspend fun getAllEvents(): List<Event> {
        return dataSource.loadEvents()
    }

    override suspend fun addEvent(event: Event) {
        val events = dataSource.loadEvents().toMutableList()
        events.add(event)
        dataSource.saveEvents(events)
    }

    override suspend fun updateEvent(event: Event) {
        val events = dataSource.loadEvents().toMutableList()
        val index = events.indexOfFirst { it.id == event.id }
        if (index != -1) {
            events[index] = event
            dataSource.saveEvents(events)
        }
    }

    override suspend fun deleteEvent(eventId: Long) {
        val events = dataSource.loadEvents().toMutableList()
        events.removeAll { it.id == eventId }
        dataSource.saveEvents(events)
    }

    override suspend fun getTodayEvents(): List<Event> {
        val todayFormat = java.text.SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        val todayString = todayFormat.format(Date())
        return getAllEvents().filter { it.date == todayString && !it.isCompleted }
    }

    override suspend fun getActiveEvents(): List<Event> {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = now.timeInMillis

        return getAllEvents()
            .filter { it.dateMillis >= todayStart }
            .sortedWith(
                compareBy(
                    { it.isCompleted },
                    { it.dateMillis },
                    { it.timeHour },
                    { it.timeMinute }
                )
            )
    }

    override suspend fun removePassedEvents() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calendar.timeInMillis

        val events = dataSource.loadEvents().filter { it.dateMillis >= todayStart }
        dataSource.saveEvents(events)
    }
}