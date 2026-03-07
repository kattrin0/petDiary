package com.example.petDiary.domain.model

data class PetProfile(
    val name: String = "",
    val breed: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val weight: String = "",
    val color: String = "",
    val chipNumber: String = "",
    val isSterilized: Boolean = false,
    val notes: String = "",
    val photoPath: String? = null,
    val userId: String? = null
)

