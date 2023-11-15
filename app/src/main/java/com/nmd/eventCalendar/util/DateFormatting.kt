package com.nmd.eventCalendar.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT

@RequiresApi(Build.VERSION_CODES.O)
val defaultDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(MEDIUM, SHORT)
