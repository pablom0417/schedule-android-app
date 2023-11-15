package com.nmd.eventCalendar.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class User(
    val displayName: String? = null,
    val email: String? = null,
    val token: String? = null,
) : Parcelable