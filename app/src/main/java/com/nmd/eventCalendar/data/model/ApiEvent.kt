package com.nmd.eventCalendar.data.model

import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.annotations.SerializedName
import com.nmd.eventCalendar.model.Memo
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
    @SerializedName("memos") val memos: ArrayList<Memo>?,
) : ApiResult {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun toCalendarEntity(yearMonth: YearMonth, index: Int, type: String): CalendarEntity? {
        val formatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")
        val startDateTime = LocalDateTime.parse("$startDate $startTime", formatter)
        val endDateTime = LocalDateTime.parse("$endDate $endTime", formatter)
        return try {
            Log.d("memos----", memos.toString() + startDate)
            CalendarEntity.Event(
                id = "100${yearMonth.year}00${yearMonth.monthValue}00$index".toLong(),
                title = name,
                location = if (memos != null && memos.any { it.date == startDate }) "${memos.filter { it.date == startDate }[0].memo}" else "",
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