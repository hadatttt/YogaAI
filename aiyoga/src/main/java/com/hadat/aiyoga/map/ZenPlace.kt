package com.hadat.aiyoga.map

data class ZenPlace(
    val id: String = "",
    val name: String = "",            // Tên của địa điểm Zen
    val creatorName: String = "",     // Tên người đăng
    val creatorAvatar: String = "",   // Link ảnh đại diện người đăng
    val backgroundImage: String = "", // Link ảnh background do người dùng chụp
    val description: String = "",     // Mô tả về địa điểm
    val time: String = "",            // Ngày tháng năm cụ thể (vd: "12/04/2026")
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radius: Double = 0.0
)