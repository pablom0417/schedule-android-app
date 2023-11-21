package com.nmd.eventCalendar.data.model

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.TypefaceSpan
import com.alamkanak.weekview.WeekViewEntity
import com.alamkanak.weekview.jsr310.setEndTime
import com.alamkanak.weekview.jsr310.setStartTime
import com.nmd.eventCalendar.utils.Utils.Companion.isDarkColor
import com.nmd.eventCalendarSample.R
import java.time.LocalDateTime

sealed class CalendarEntity {

    data class Event(
        val id: Long,
        val title: CharSequence,
        val startTime: LocalDateTime,
        val endTime: LocalDateTime,
        val location: CharSequence,
        val color: Int,
        val isAllDay: Boolean,
        val isCanceled: Boolean
    ) : CalendarEntity()
}

fun CalendarEntity.toWeekViewEntity(): WeekViewEntity {
    return when (this) {
        is CalendarEntity.Event -> toWeekViewEntity()
    }
}

fun CalendarEntity.Event.toWeekViewEntity(): WeekViewEntity {
    val backgroundColor = color
    val textColor = if (color.isDarkColor()) Color.WHITE else Color.BLACK
    val borderWidthResId = R.dimen.no_border_width

    val style = WeekViewEntity.Style.Builder()
        .setTextColor(textColor)
        .setBackgroundColor(backgroundColor)
        .setBorderWidthResource(borderWidthResId)
        .setBorderColor(color)
        .build()

    val title = SpannableStringBuilder(title).apply {
        val titleSpan = TypefaceSpan("sans-serif-medium")
        setSpan(titleSpan, 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (isCanceled) {
            setSpan(StrikethroughSpan(), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    val subtitle = SpannableStringBuilder(location).apply {
        if (isCanceled) {
            setSpan(StrikethroughSpan(), 0, location.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    var type = ""
    type = if (location != "") "✔ $location" else ""

    return WeekViewEntity.Event.Builder(this)
        .setId(id)
        .setTitle(title)
        .setStartTime(startTime)
        .setEndTime(endTime)
        .setSubtitle(type)
        .setAllDay(isAllDay)
        .setStyle(style)
        .build()
}
