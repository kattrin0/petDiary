package com.example.petDiary.domain.repository

import com.example.petDiary.domain.model.PetProfile

interface IPetProfileRepository {
    suspend fun getPetProfile(): PetProfile?
    suspend fun savePetProfile(profile: PetProfile)
    suspend fun hasSavedData(): Boolean
}