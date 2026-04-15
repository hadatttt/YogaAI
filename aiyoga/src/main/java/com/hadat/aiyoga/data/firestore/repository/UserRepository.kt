package com.hadat.aiyoga.data.firestore.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hadat.aiyoga.data.firestore.model.User
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            usersCollection.document(user.uid).set(user).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getUser(uid: String): User? = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isUserExists(uid: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val document = usersCollection.document(uid).get().await()
            document.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateProfile(uid: String, displayName: String, photoUrl: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val data = mapOf(
                "displayName" to displayName,
                "photoUrl" to photoUrl
            )
            usersCollection.document(uid).update(data).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}