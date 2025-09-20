package com.example.myapplication.models

data class KopiUser(
    val points: Double = 0.0,
    val noOfKopiRedeemed: Int = 0,
    val walkHistory: List<WalkSession> = emptyList()
)