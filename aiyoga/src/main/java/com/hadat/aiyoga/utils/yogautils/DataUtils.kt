package com.hadat.aiyoga.utils.yogautils

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hadat.aiyoga.detailyoga.YogaPoseDetailModel
import com.hadat.aiyoga.home.CategoryModel
import com.hadat.aiyoga.yogamain.YogaPoseModel

object YogaDataUtils {
    private const val TAG = "YogaDataUtils"
    private const val CONFIG_YOGA_KEY = "data_yoga_image"
    private const val CONFIG_CATEGORY_KEY = "data_yoga_categories"
    private const val CONFIG_DETAIL_KEY = "data_yoga_details"

    @SuppressLint("StaticFieldLeak")
    private val remoteConfig = Firebase.remoteConfig
    private val gson = Gson()

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    /**
     * Giữ nguyên hàm lấy Poses của bạn
     */
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

    /**
     * Hàm mới thêm: Lấy danh sách Category
     */
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
                    // Parse toàn bộ Map từ Firebase: Key là String (ID), Value là DetailModel
                    val detailMap = parseDetailMapJson(json)
                    // Lấy đúng cái Detail theo ID truyền vào
                    val detail = detailMap?.get(id.toString())
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

    private fun parseDetailMapJson(json: String): Map<String, YogaPoseDetailModel>? {
        return try {
            // Sử dụng Map<String, YogaPoseDetailModel> để tối ưu việc tìm kiếm theo ID
            val mapType = object : TypeToken<Map<String, YogaPoseDetailModel>>() {}.type
            gson.fromJson<Map<String, YogaPoseDetailModel>>(json, mapType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi Parse Detail Map GSON: ${e.message}")
            null
        }
    }
}