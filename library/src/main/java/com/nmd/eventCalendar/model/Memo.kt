package com.nmd.eventCalendar.model

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
data class Memo (
    var date: String? = null,
    var memo: String? = null,
) : Parcelable