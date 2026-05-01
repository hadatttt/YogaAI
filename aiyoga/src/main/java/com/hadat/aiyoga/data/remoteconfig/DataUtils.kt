package com.hadat.aiyoga.data.remoteconfig

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hadat.aiyoga.R
import com.hadat.aiyoga.detailyoga.YogaPoseAngleModel
import com.hadat.aiyoga.detailyoga.YogaPoseDetailModel
import com.hadat.aiyoga.home.CategoryModel
import com.hadat.aiyoga.utils.service.AppPreferences
import com.hadat.aiyoga.utils.yogautils.YogaMetModel
import com.hadat.aiyoga.yoga_ai.YogaPoseModel

/**
 * Quản lý dữ liệu Yoga từ Firebase Remote Config.
 * Áp dụng Singleton pattern và Thread-safety.
 */
@SuppressLint("StaticFieldLeak")
object YogaDataUtils {
    private const val TAG = "YogaDebug" // Đổi tag để bạn dễ lọc trong Logcat

    // Keys Remote Config
    private const val KEY_MET = "data_yoga_met"
    private const val KEY_POSE = "data_yoga_image"
    private const val KEY_DETAIL = "data_yoga_details"
    private const val KEY_ANGLE = "data_yoga_angles"

    private val remoteConfig by lazy { Firebase.remoteConfig }
    private val gson = Gson()

    @Volatile
    var isDataReady = false
        private set

    private var cachedPoses: List<YogaPoseModel>? = null
    private var cachedDetails: List<YogaPoseDetailModel>? = null
    private var cachedAngles: List<YogaPoseAngleModel>? = null
    private var cachedMetMap: Map<Int, Double>? = null
    private var currentLang: String? = null

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0 // Để 0s để debug nhanh, release nên để 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    /**
     * Nạp dữ liệu từ Firebase hoặc Cache Local.
     */
    fun prefetchData(context: Context, forceRefresh: Boolean = false, onComplete: (Boolean) -> Unit) {
        val lang = AppPreferences.getLanguageCode(context)
        Log.d(TAG, "--- Start PrefetchData (Lang: $lang, Force: $forceRefresh) ---")

        if (!forceRefresh && lang == currentLang && isDataReady) {
            Log.d(TAG, "Data already cached and ready. Skipping fetch.")
            onComplete(true)
            return
        }

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            Log.d(TAG, "Firebase Fetch Success: ${task.isSuccessful}")

            // Bất kể thành công hay không, đều thử refreshCache để lấy data local cũ
            currentLang = lang
            refreshCache(context)

            isDataReady = cachedPoses != null && cachedAngles != null
            Log.d(TAG, "Final Data Status: isDataReady = $isDataReady")

            onComplete(isDataReady)
        }
    }

    /**
     * Parse toàn bộ JSON từ Remote Config vào Cache trong RAM.
     */
    @Synchronized
    private fun refreshCache(context: Context) {
        val lang = currentLang ?: AppPreferences.getLanguageCode(context)
        Log.d(TAG, "Refreshing Cache for Language: $lang")

        // 1. Pose Data
        val poseKey = if (lang == "vi") "${KEY_POSE}_vi" else KEY_POSE
        cachedPoses = parseJson<List<YogaPoseModel>>(getSafeJson(poseKey, KEY_POSE))
        Log.d(TAG, "-> Loaded Poses: ${cachedPoses?.size ?: 0}")

        // 2. Detail Data
        val detailKey = if (lang == "vi") "${KEY_DETAIL}_vi" else KEY_DETAIL
        cachedDetails = parseJson<List<YogaPoseDetailModel>>(getSafeJson(detailKey, KEY_DETAIL))
        Log.d(TAG, "-> Loaded Details: ${cachedDetails?.size ?: 0}")

        // 3. Angles Data
        cachedAngles = parseJson<List<YogaPoseAngleModel>>(remoteConfig.getString(KEY_ANGLE))
        Log.d(TAG, "-> Loaded Angles: ${cachedAngles?.size ?: 0}")

        // 4. MET Data
        val metJson = remoteConfig.getString(KEY_MET)
        val metList = parseJson<List<YogaMetModel>>(metJson)
        cachedMetMap = metList?.associate { it.id to it.met }
        Log.d(TAG, "-> Loaded MET Map: ${cachedMetMap?.size ?: 0}")
    }

    private fun getSafeJson(preferredKey: String, defaultKey: String): String {
        val value = remoteConfig.getString(preferredKey)
        return if (value.isNotEmpty()) value else remoteConfig.getString(defaultKey)
    }

    private inline fun <reified T> parseJson(json: String): T? {
        if (json.isBlank()) {
            Log.w(TAG, "JSON String is blank/empty. Type: ${T::class.java.simpleName}")
            return null
        }
        return try {
            val type = object : TypeToken<T>() {}.type
            gson.fromJson<T>(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Parse Error for ${T::class.java.simpleName}: ${e.message}")
            null
        }
    }

    // --- Public Getters ---
    fun getAllPoses(): List<YogaPoseModel> = cachedPoses ?: emptyList()
    fun getAllAngles(): List<YogaPoseAngleModel> = cachedAngles ?: emptyList()
    fun getPoseDetail(id: Int): YogaPoseDetailModel? = cachedDetails?.find { it.id == id }
    fun getMetValue(id: Int): Double = cachedMetMap?.get(id) ?: 3.0
    fun getAllMetData(): Map<Int, Double> = cachedMetMap ?: emptyMap()

    fun onLanguageChanged(context: Context, onComplete: (Boolean) -> Unit) {
        prefetchData(context, true, onComplete)
    }

    fun getLocalizedCategory(context: Context, rawValue: String): String {
        return when (rawValue.trim()) {
            "All" -> context.getString(R.string.category_all)
            "Standing" -> context.getString(R.string.category_standing)
            "Seated" -> context.getString(R.string.category_seated)
            "Prone" -> context.getString(R.string.category_prone)
            "Supine" -> context.getString(R.string.category_supine)
            "Inversion" -> context.getString(R.string.category_inversion)
            "Arm Balance" -> context.getString(R.string.category_arm_balance)
            "Arm Leg Support" -> context.getString(R.string.category_arm_leg_support)
            else -> rawValue
        }
    }

    fun getLocalYogaCategories(): List<CategoryModel> = listOf(
        "All", "Standing", "Seated", "Prone", "Supine",
        "Inversion", "Arm Balance", "Arm Leg Support"
    ).map { CategoryModel(it, it) }
}