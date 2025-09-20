package com.example.myapplication.models

import com.google.firebase.firestore.PropertyName

data class Friend(
    @PropertyName("friendId")
    val friendId: String = "",

    @PropertyName("friendName")
    val friendName: String = "",

    @PropertyName("friendEmail")
    val friendEmail: String = "",

    @PropertyName("addedAt")
    val addedAt: Long = 0L,

    @PropertyName("status")
    val status: String = "PENDING", // PENDING, ACCEPTED, BLOCKED

    @PropertyName("totalWalksTogether")
    val totalWalksTogether: Int = 0,

    @PropertyName("lastWalkedTogether")
    val lastWalkedTogether: Long = 0L
) {
    // No-argument constructor required for Firestore
    constructor() : this("", "", "", 0L, "PENDING", 0, 0L)
}