package com.hadat.aiyoga.data.firestore.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hadat.aiyoga.data.firestore.model.MapPostModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MapRepository {
    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection("yoga_map_posts")
    private val legacyPostsCollection = db.collection("map_posts")

    suspend fun createPost(post: MapPostModel): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            postsCollection.add(post).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getLatestPosts(limit: Long = 100): List<MapPostModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            val latest = postsCollection
                .limit(limit)
                .get()
                .await()
                .toObjects(MapPostModel::class.java)

            val legacy = legacyPostsCollection
                .limit(limit)
                .get()
                .await()
                .toObjects(MapPostModel::class.java)

            (latest + legacy)
                .distinctBy { it.id + it.userId + it.lat + it.lng + (it.createdAt?.time ?: 0L) }
                .sortedByDescending { it.createdAt?.time ?: 0L }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getPostsNear(lat: Double, lng: Double, latDelta: Double = 0.03, lngDelta: Double = 0.03): List<MapPostModel> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val minLat = lat - latDelta
                val maxLat = lat + latDelta

                postsCollection
                    .whereGreaterThanOrEqualTo("lat", minLat)
                    .whereLessThanOrEqualTo("lat", maxLat)
                    .get()
                    .await()
                    .toObjects(MapPostModel::class.java)
                    .filter { it.lng in (lng - lngDelta)..(lng + lngDelta) }
                    .sortedByDescending { it.createdAt }
            } catch (_: Exception) {
                emptyList()
            }
        }
}

