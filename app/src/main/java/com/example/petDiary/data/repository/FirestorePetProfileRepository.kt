package com.example.petDiary.data.repository

import com.example.petDiary.domain.model.PetProfile
import com.example.petDiary.domain.repository.IPetProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestorePetProfileRepository : IPetProfileRepository {

    private val firestore: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val collection = firestore.collection("petProfiles")

    override suspend fun getPetProfile(): PetProfile? {
        val userId = auth.currentUser?.uid ?: return null

        return try {
            val document = collection.document(userId).get().await()
            if (document.exists()) {
                document.toObject(PetProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun savePetProfile(profile: PetProfile) {
        val userId = auth.currentUser?.uid ?: return

        val profileWithUserId = profile.copy(userId = userId)

        try {
            collection.document(userId).set(profileWithUserId).await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun hasSavedData(): Boolean {
        return getPetProfile() != null
    }
}