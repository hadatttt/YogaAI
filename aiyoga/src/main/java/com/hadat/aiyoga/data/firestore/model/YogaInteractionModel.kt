package com.hadat.aiyoga.data.firestore.model

import com.google.firebase.firestore.PropertyName

data class YogaInteractionModel(
    @get:PropertyName("poseId") @set:PropertyName("poseId")
    var poseId: String = "",

    @get:PropertyName("poseName") @set:PropertyName("poseName")
    var poseName: String = "",

    @get:PropertyName("clickCount") @set:PropertyName("clickCount")
    var clickCount: Int = 0,

    @get:PropertyName("dateKey") @set:PropertyName("dateKey")
    var dateKey: String = "",

    @get:PropertyName("lastUpdated") @set:PropertyName("lastUpdated")
    var lastUpdated: Long = System.currentTimeMillis()
)