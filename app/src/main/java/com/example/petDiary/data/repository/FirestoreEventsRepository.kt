package com.example.petDiary.data.repository

import com.example.petDiary.domain.model.Event
import com.example.petDiary.domain.repository.IEventsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.*

class FirestoreEventsRepository : IEventsRepository {

    private val firestore: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val collection = firestore.collection("events")

    override suspend fun getAllEvents(): List<Event> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val querySnapshot = collection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            querySnapshot.toObjects(Event::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun addEvent(event: Event) {
        val userId = auth.currentUser?.uid ?: return
        val eventWithUserId = event.copy(userId = userId)

        try {
            collection.document(event.id.toString()).set(eventWithUserId).await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun updateEvent(event: Event) {
        val userId = auth.currentUser?.uid ?: return
        try {
            collection.document(event.id.toString()).set(event).await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun deleteEvent(eventId: Long) {
        val userId = auth.currentUser?.uid ?: return
        try {
            collection.document(eventId.toString()).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun getTodayEvents(): List<Event> {
        return try {
            val allEvents = getAllEvents()
            val todayFormat = java.text.SimpleDateFormat("d MMMM yyyy", Locale("ru"))
            val todayString = todayFormat.format(Date())

            allEvents.filter { event ->
                event.date == todayString && !event.isCompleted
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getActiveEvents(): List<Event> {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = now.timeInMillis

        return try {
            val allEvents = getAllEvents()
            allEvents
                .filter { it.dateMillis >= todayStart }
                .sortedWith(
                    compareBy(
                        { it.isCompleted },
                        { it.dateMillis },
                        { it.timeHour },
                        { it.timeMinute }
                    )
                )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun removePassedEvents() {
        // Получаем начало сегодняшнего дня (полночь)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calendar.timeInMillis

        try {
            val allEvents = getAllEvents()
            // Удаляем только события, которые строго раньше сегодняшней даты
            val passedEvents = allEvents.filter { it.dateMillis < todayStart }
            passedEvents.forEach { event ->
                collection.document(event.id.toString()).delete().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}