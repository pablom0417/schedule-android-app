package com.nmd.eventCalendar.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Event(
    val email: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val name: String? = null,
    val backgroundHexColor: String? = null,
    var id: String? = null,
    var memos: ArrayList<Memo>? = null
) : Parcelable