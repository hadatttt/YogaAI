package com.hadat.aiyoga.firestore.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "photoUrl" to photoUrl
        )
    }
}