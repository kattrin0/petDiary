package com.example.petDiary.data.repository

import android.content.Context
import com.example.petDiary.data.datasource.PetProfileDataSource
import com.example.petDiary.domain.model.PetProfile

class PetProfileRepository(private val context: Context) {
    private val dataSource = PetProfileDataSource(context)

    fun getPetProfile(): PetProfile {
        return dataSource.loadPetProfile()
    }

    fun savePetProfile(profile: PetProfile) {
        dataSource.savePetProfile(profile)
    }

    fun hasSavedData(): Boolean {
        return dataSource.hasSavedData()
    }
}

