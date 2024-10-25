package com.zune.customtv.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TvBean(
    var name: String,
    var urls: MutableList<UrlBean>
) : Parcelable

@Parcelize
data class UrlBean (
    var url: String,
    var timeout: Int
) : Parcelable
