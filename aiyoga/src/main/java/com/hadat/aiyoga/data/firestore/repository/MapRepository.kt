package com.hadat.aiyoga.data.firestore.repository

import com.google.firebase.firestore.CollectionReference
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
        getPostsInBounds(
            southLat = lat - latDelta,
            northLat = lat + latDelta,
            westLng = lng - lngDelta,
            eastLng = lng + lngDelta
        )

    suspend fun getPostsInBounds(
        southLat: Double,
        northLat: Double,
        westLng: Double,
        eastLng: Double,
        limit: Long = 200
    ): List<MapPostModel> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val minLat = minOf(southLat, northLat).coerceAtLeast(-90.0)
                val maxLat = maxOf(southLat, northLat).coerceAtMost(90.0)
                val normalizedWestLng = westLng.coerceIn(-180.0, 180.0)
                val normalizedEastLng = eastLng.coerceIn(-180.0, 180.0)

                val latest = getPostsInBoundsFromCollection(
                    postsCollection,
                    minLat,
                    maxLat,
                    normalizedWestLng,
                    normalizedEastLng,
                    limit
                )
                val legacy = getPostsInBoundsFromCollection(
                    legacyPostsCollection,
                    minLat,
                    maxLat,
                    normalizedWestLng,
                    normalizedEastLng,
                    limit
                )

                (latest + legacy)
                    .distinctBy { it.id + it.userId + it.lat + it.lng + (it.createdAt?.time ?: 0L) }
                    .sortedByDescending { it.createdAt?.time ?: 0L }
                    .take(limit.toInt())
            } catch (_: Exception) {
                emptyList()
            }
        }

    private suspend fun getPostsInBoundsFromCollection(
        collection: CollectionReference,
        minLat: Double,
        maxLat: Double,
        westLng: Double,
        eastLng: Double,
        limit: Long
    ): List<MapPostModel> {
        // Firestore only supports the range query on latitude here; longitude is filtered locally.
        return collection
            .whereGreaterThanOrEqualTo("lat", minLat)
            .whereLessThanOrEqualTo("lat", maxLat)
            .limit(limit)
            .get()
            .await()
            .toObjects(MapPostModel::class.java)
            .filter { isLngInBounds(it.lng, westLng, eastLng) }
    }

    private fun isLngInBounds(lng: Double, westLng: Double, eastLng: Double): Boolean {
        return if (westLng <= eastLng) {
            lng in westLng..eastLng
        } else {
            lng >= westLng || lng <= eastLng
        }
    }
}

