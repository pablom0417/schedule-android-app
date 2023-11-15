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
import java.time.format.DateTimeFormatter

interface ApiResult {
    fun toCalendarEntity(yearMonth: YearMonth, index: Int, type: String): CalendarEntity?
}

data class ApiEvent(
    @SerializedName("name") val name: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("backgroundHexColor") val backgroundHexColor: String,
) : ApiResult {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun toCalendarEntity(yearMonth: YearMonth, index: Int, type: String): CalendarEntity? {
        val formatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")
        val startDateTime = LocalDateTime.parse("$startDate $startTime", formatter)
        val endDateTime = LocalDateTime.parse("$endDate $endTime", formatter)
        return try {
            Log.d("start-end", LocalDateTime.parse("$startDate $startTime", formatter).toString())
            CalendarEntity.Event(
                id = "100${yearMonth.year}00${yearMonth.monthValue}00$index".toLong(),
                title = name.split("|")[0],
                location = name.split("|")[1],
                startTime = startDateTime,
                endTime = endDateTime,
                color = Color.parseColor(backgroundHexColor),
                isAllDay = false,
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
    override fun toCalendarEntity(yearMonth: YearMonth, index: Int, type: String): CalendarEntity? {
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
