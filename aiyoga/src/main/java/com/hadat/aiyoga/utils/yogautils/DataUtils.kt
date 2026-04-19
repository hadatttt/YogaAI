package com.hadat.aiyoga.utils.yogautils

import android.annotation.SuppressLint
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
import com.hadat.aiyoga.yogamain.YogaPoseModel

object YogaDataUtils {
    private const val TAG = "YogaDataUtils"
    private const val CONFIG_YOGA_MET_KEY = "data_yoga_met"
    private const val CONFIG_YOGA_KEY = "data_yoga_image"
    private const val CONFIG_CATEGORY_KEY = "data_yoga_categories"
    private const val CONFIG_DETAIL_KEY = "data_yoga_details"
    private const val CONFIG_YOGA_ANGLES_KEY = "data_yoga_angles"

    @SuppressLint("StaticFieldLeak")
    private val remoteConfig = Firebase.remoteConfig
    private val gson = Gson()

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }
    fun getRemoteYogaMet(id: Int, onResult: (Double) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val json = remoteConfig.getString(CONFIG_YOGA_MET_KEY)
                if (json.isNotEmpty()) {
                    val metList = parseMetJson(json)
                    val metValue = metList?.find { it.id == id }?.met ?: 3.0
                    onResult(metValue)
                } else {
                    Log.e(TAG, "Key '$CONFIG_YOGA_MET_KEY' trống")
                    onResult(3.0)
                }
            } else {
                onResult(3.0)
            }
        }
    }
    fun getRemoteYogaAngles(onResult: (List<YogaPoseAngleModel>?) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val json = remoteConfig.getString(CONFIG_YOGA_ANGLES_KEY)
                if (json.isNotEmpty()) {
                    onResult(parseAngleJson(json))
                } else {
                    Log.e(TAG, "Key '$CONFIG_YOGA_ANGLES_KEY' trống")
                    onResult(null)
                }
            } else {
                onResult(null)
            }
        }
    }
    fun getAllRemoteMet(onResult: (Map<Int, Double>) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            val metMap = mutableMapOf<Int, Double>()
            if (task.isSuccessful) {
                val json = remoteConfig.getString(CONFIG_YOGA_MET_KEY)
                if (json.isNotEmpty()) {
                    parseMetJson(json)?.forEach {
                        metMap[it.id] = it.met
                    }
                }
            }
            onResult(metMap)
        }
    }
    private fun parseAngleJson(json: String): List<YogaPoseAngleModel>? {
        return try {
            val listType = object : TypeToken<List<YogaPoseAngleModel>>() {}.type
            gson.fromJson<List<YogaPoseAngleModel>>(json, listType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi Parse Angle GSON: ${e.message}")
            null
        }
    }
    fun getRemoteYogaPoses(onResult: (List<YogaPoseModel>?) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Các Keys hiện có trên Firebase: ${remoteConfig.all.keys}")

                val json = remoteConfig.getString(CONFIG_YOGA_KEY)
                Log.d(TAG, "JSON Poses nhận được: '$json'")

                if (json.isNotEmpty()) {
                    onResult(parseJsonToModel(json))
                } else {
                    Log.e(TAG, "Nội dung Key '$CONFIG_YOGA_KEY' bị trống")
                    onResult(null)
                }
            } else {
                onResult(null)
            }
        }
    }
    private fun parseMetJson(json: String): List<YogaMetModel>? {
        return try {
            val listType = object : TypeToken<List<YogaMetModel>>() {}.type
            gson.fromJson<List<YogaMetModel>>(json, listType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi Parse MET GSON: ${e.message}")
            null
        }
    }
    fun getRemoteYogaCategories(onResult: (List<CategoryModel>?) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val json = remoteConfig.getString(CONFIG_CATEGORY_KEY)
                Log.d(TAG, "JSON Categories nhận được: '$json'")

                if (json.isNotEmpty()) {
                    onResult(parseCategoryJson(json))
                } else {
                    Log.e(TAG, "Nội dung Key '$CONFIG_CATEGORY_KEY' bị trống")
                    onResult(null)
                }
            } else {
                onResult(null)
            }
        }
    }

    fun getLocalizedCategory(context: android.content.Context, rawValue: String): String {
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

    private fun parseJsonToModel(json: String): List<YogaPoseModel>? {
        return try {
            val listType = object : TypeToken<List<YogaPoseModel>>() {}.type
            gson.fromJson<List<YogaPoseModel>>(json, listType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi Parse Poses GSON: ${e.message}")
            null
        }
    }

    private fun parseCategoryJson(json: String): List<CategoryModel>? {
        return try {
            val listType = object : TypeToken<List<CategoryModel>>() {}.type
            gson.fromJson<List<CategoryModel>>(json, listType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi Parse Category GSON: ${e.message}")
            null
        }
    }
    fun getRemoteYogaDetail(id: Int, onResult: (YogaPoseDetailModel?) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val json = remoteConfig.getString(CONFIG_DETAIL_KEY)
                if (json.isNotEmpty()) {
                    // Sửa chỗ này: Parse thành List thay vì Map
                    val detailList = parseDetailListJson(json)
                    // Tìm kiếm tư thế có ID trùng với ID truyền vào
                    val detail = detailList?.find { it.id == id }
                    onResult(detail)
                } else {
                    Log.e(TAG, "Nội dung Key '$CONFIG_DETAIL_KEY' bị trống")
                    onResult(null)
                }
            } else {
                onResult(null)
            }
        }
    }

    private fun parseDetailListJson(json: String): List<YogaPoseDetailModel>? {
        return try {
            val listType = object : TypeToken<List<YogaPoseDetailModel>>() {}.type
            gson.fromJson<List<YogaPoseDetailModel>>(json, listType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi Parse Detail List GSON: ${e.message}")
            null
        }
    }

    private fun parseDetailMapJson(json: String): Map<String, YogaPoseDetailModel>? {
        return try {
            val mapType = object : TypeToken<Map<String, YogaPoseDetailModel>>() {}.type
            gson.fromJson<Map<String, YogaPoseDetailModel>>(json, mapType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi Parse Detail Map GSON: ${e.message}")
            null
        }
    }
}