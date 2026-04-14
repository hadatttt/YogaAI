package com.hadat.aiyoga.data.firestore.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hadat.aiyoga.sequence.WorkoutSequenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SequenceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val sequenceCollection = db.collection("yoga_sequences")

    suspend fun saveSequence(sequence: WorkoutSequenceModel): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            sequenceCollection.add(sequence).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getMySequences(userId: String): List<WorkoutSequenceModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            sequenceCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(WorkoutSequenceModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}