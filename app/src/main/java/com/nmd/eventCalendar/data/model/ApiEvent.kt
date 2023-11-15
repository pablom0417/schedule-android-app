package com.nmd.eventCalendar.data.model

import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.annotations.SerializedName
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

interface ApiResult {
    fun toCalendarEntity(yearMonth: YearMonth, index: Int): CalendarEntity?
}

data class ApiEvent(
    @SerializedName("name") val name: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: Int,
    @SerializedName("backgroundHexColor") val backgroundHexColor: String,
) : ApiResult {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun toCalendarEntity(yearMonth: YearMonth, index: Int): CalendarEntity? {
        return try {
            val startDateTime = LocalDateTime.parse("$startDate $startTime")
            val endDateTime = LocalDateTime.parse("$endDate $endTime")
            CalendarEntity.Event(
                id = "100${yearMonth.year}00${yearMonth.monthValue}00$index".toLong(),
                title = name,
                location = "",
                startTime = startDateTime,
                endTime = endDateTime,
                color = Color.parseColor(backgroundHexColor),
                isAllDay = startDate != endDate,
                isCanceled = false
            )
        } catch (e: DateTimeException) {
            null
        }
    }
}

data class ApiBlockedTime(
    @SerializedName("day_of_month") val dayOfMonth: Int,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("duration") val duration: Int
) : ApiResult {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun toCalendarEntity(yearMonth: YearMonth, index: Int): CalendarEntity? {
        return try {
            val startTime = LocalTime.parse(startTime)
            val startDateTime = yearMonth.atDay(dayOfMonth).atTime(startTime)
            val endDateTime = startDateTime.plusMinutes(duration.toLong())
            CalendarEntity.BlockedTimeSlot(
                id = "200${yearMonth.year}00${yearMonth.monthValue}00$index".toLong(),
                startTime = startDateTime,
                endTime = endDateTime
            )
        } catch (e: DateTimeException) {
            null
        }
    }
}
