package com.nmd.eventCalendar.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nmd.eventCalendar.data.model.ApiEvent
import com.nmd.eventCalendar.data.model.ApiResult
import com.nmd.eventCalendar.data.model.CalendarEntity
import com.nmd.eventCalendar.model.Event
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Calendar
import java.util.Date


class EventsRepository(private val context: Context) {

    private val eventResponseType = object : TypeToken<List<ApiEvent>>() {}.type

    var firebaseDatabase = Firebase.firestore
    private var scheduleDatabaseReference = firebaseDatabase.collection("schedules")
    private var memoDatabaseReference = firebaseDatabase.collection("memos")
    private val user = FirebaseAuth.getInstance().currentUser

    private val gson = Gson()

    @SuppressLint("SimpleDateFormat")
    fun fetch(
        email: String,
        yearMonths: List<YearMonth>,
        onSuccess: (List<CalendarEntity>) -> Unit
    ) {
        val handlerThread = HandlerThread("events-fetching")
        handlerThread.start()

        val backgroundHandler = Handler(handlerThread.looper)
        val mainHandler = Handler(Looper.getMainLooper())

        val eventList = arrayListOf<Event>()
        scheduleDatabaseReference.get().addOnSuccessListener {
            for (document in it) {
                val event = document.toObject<Event>()
                if (event.email == email) {
                    if (event.startDate != event.endDate) {
                        val formatter = SimpleDateFormat("MM/dd/yyyy")
                        var start: Date? = formatter.parse(event.startDate!!)
                        val end: Date? = formatter.parse(event.endDate!!)
                        if (start != null) {
                            while (start!! <= end) {
                                val calendar: Calendar = Calendar.getInstance()
                                calendar.time = start
                                val startDate = String.format("%02d/%02d/%04d", calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.YEAR))
                                val tempEvent = Event(
                                    event.email,
                                    startDate,
                                    startDate,
                                    event.startTime,
                                    event.endTime,
                                    event.name,
                                    event.backgroundHexColor,
                                    event.id,
                                    event.memos
                                )
                                eventList.add(tempEvent)
                                calendar.add(Calendar.DATE, 1)
                                start = calendar.time
                            }
                        }
                    } else {
                        val tempEvent = Event(
                            event.email,
                            event.startDate,
                            event.endDate,
                            event.startTime,
                            event.endTime,
                            event.name,
                            event.backgroundHexColor,
                            event.id,
                            event.memos
                        )
                        eventList.add(tempEvent)
                    }
                }
            }
            val apiEntities: List<ApiResult> = gson.fromJson(gson.toJson(eventList), eventResponseType)
            val calendarEntities = apiEntities.mapIndexedNotNull { index, apiResult ->
                Log.d("apiEntities", apiEntities.toString() + index)
                apiResult.toCalendarEntity(yearMonths[0], index, "- S -")
            }
            Log.d("calendarEntities", calendarEntities.toString())
            onSuccess(calendarEntities)
        }
    }

    private fun fetchEvents(): List<ApiResult> {
        val inputStream = context.assets.open("events.json")
        val json = inputStream.reader().readText()
        return gson.fromJson(json, eventResponseType)
    }
}
