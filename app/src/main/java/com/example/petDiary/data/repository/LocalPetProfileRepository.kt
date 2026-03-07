package com.example.petDiary.data.repository

import android.content.Context
import com.example.petDiary.data.datasource.PetProfileDataSource
import com.example.petDiary.domain.model.PetProfile
import com.example.petDiary.domain.repository.IPetProfileRepository

class LocalPetProfileRepository(
    private val context: Context
) : IPetProfileRepository {  // <- implements interface

    private val dataSource = PetProfileDataSource(context)

    override suspend fun getPetProfile(): PetProfile? {
        val profile = dataSource.loadPetProfile()
        return if (profile.name.isNotEmpty()) profile else null
    }

    override suspend fun savePetProfile(profile: PetProfile) {
        dataSource.savePetProfile(profile)
    }

    override suspend fun hasSavedData(): Boolean {
        return dataSource.hasSavedData()
    }
}