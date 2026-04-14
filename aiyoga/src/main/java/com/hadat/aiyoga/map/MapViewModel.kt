package com.hadat.aiyoga.map

import androidx.lifecycle.MutableLiveData
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class MapViewModel : BaseViewModel() {

    val zenPlaces = MutableLiveData<List<ZenPlace>>()

    fun fetchAllZenPlaces() {
        val imageUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT3s7xQrKz46dWK_UZ0J5UWVbnjxtWVp3nEYQ&s"

        val dummyList = listOf(
            ZenPlace(
                id = "1",
                name = "Công viên 29/3 - Góc thiền",
                creatorName = "Hà Văn Khánh Đạt",
                creatorAvatar = imageUrl,
                backgroundImage = imageUrl,
                description = "Góc này buổi sáng rất vắng, nhiều cây xanh, cực hợp để tập Vinyasa.",
                time = "12/04/2026", // Ngày tháng cụ thể
                lat = 16.0667,
                lng = 108.2117,
                radius = 60.0
            ),
            ZenPlace(
                id = "2",
                name = "Bờ hồ Hàm Nghi",
                creatorName = "Minh Anh",
                creatorAvatar = imageUrl,
                backgroundImage = imageUrl,
                description = "View hồ cực chill, gió mát rượi vào sáng sớm. Mọi người nên thử nhé!",
                time = "10/04/2026",
                lat = 16.0595,
                lng = 108.2100,
                radius = 45.0
            ),
            ZenPlace(
                id = "3",
                name = "Bãi biển Mỹ Khê",
                creatorName = "Yoga Master",
                creatorAvatar = imageUrl,
                backgroundImage = imageUrl,
                description = "Tập yoga đón bình minh trên biển là trải nghiệm tuyệt vời nhất.",
                time = "01/01/2026",
                lat = 16.0600,
                lng = 108.2450,
                radius = 100.0
            )
        )

        zenPlaces.value = dummyList
    }
}