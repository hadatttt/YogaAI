package com.hadat.firestore.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hadat.firestore.model.User
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // CREATE / UPDATE: Lưu thông tin user
    suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            usersCollection.document(user.uid).set(user).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // READ: Lấy thông tin user bằng UID
    suspend fun getUser(uid: String): User? = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // CHECK: Kiểm tra user đã tồn tại chưa
    suspend fun isUserExists(uid: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val document = usersCollection.document(uid).get().await()
            document.exists()
        } catch (e: Exception) {
            false
        }
    }
}