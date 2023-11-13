@file:JvmName("ToolbarUtils")
package com.alamkanak.weekview.sample.util

import android.app.Activity
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.jsr310.scrollToDateTime
import com.nmd.eventCalendarSample.R
import java.time.LocalDateTime

private enum class WeekViewType(val value: Int) {
    DayView(1),
    ThreeDayView(3),
    WeekView(7);

    companion object {
        fun of(days: Int): WeekViewType = values().first { it.value == days }
    }
}

private val Activity.label: String
    get() = getString(packageManager.getActivityInfo(componentName, 0).labelRes)

