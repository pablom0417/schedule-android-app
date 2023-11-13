package com.nmd.eventCalendar.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Schedule(
    val startDate: String,
    val endDate: String,
    val startDateIndex: Int,
    val endDateIndex: Int,
) : Parcelable