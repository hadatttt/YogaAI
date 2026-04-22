package com.hadat.aiyoga.community

import androidx.annotation.StringRes
import com.hadat.aiyoga.R

enum class CommunityCategory(@StringRes val titleRes: Int) {
    LATEST(R.string.cat_latest),
    TOP_LIKED(R.string.cat_top_liked),
    TOP_VIEWED(R.string.cat_top_viewed),
    LIKED_BY_ME(R.string.cat_liked_by_me)
}