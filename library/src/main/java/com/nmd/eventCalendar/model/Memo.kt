package com.nmd.eventCalendar.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Memo (
    var date: String? = null,
    var memo: String? = null,
    var color: String? = null,
    var file: String? = null
)