package com.nmd.eventCalendar.util

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.YearMonth

fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

@RequiresApi(Build.VERSION_CODES.O)
fun yearMonthsBetween(startDate: LocalDate, endDate: LocalDate): List<YearMonth> {
    val yearMonths = mutableListOf<YearMonth>()
    val maxYearMonth = endDate.yearMonth
    var currentYearMonth = startDate.yearMonth

    while (currentYearMonth <= maxYearMonth) {
        yearMonths += currentYearMonth
        currentYearMonth = currentYearMonth.plusMonths(1)
    }

    return yearMonths
}

private val LocalDate.yearMonth: YearMonth
    @RequiresApi(Build.VERSION_CODES.O)
    get() = YearMonth.of(year, month)
