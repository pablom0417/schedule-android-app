package com.nmd.eventCalendar.data

import android.R.attr.end
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nmd.eventCalendar.data.model.ApiBlockedTime
import com.nmd.eventCalendar.data.model.ApiEvent
import com.nmd.eventCalendar.data.model.ApiResult
import com.nmd.eventCalendar.data.model.CalendarEntity
import com.nmd.eventCalendar.model.Event
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar
import java.util.Date


class EventsRepository(private val context: Context) {

    private val eventResponseType = object : TypeToken<List<ApiEvent>>() {}.type
    private val blockedTimeResponseType = object : TypeToken<List<ApiBlockedTime>>() {}.type

    var firebaseDatabase = Firebase.database.reference
    private var scheduleDatabaseReference = firebaseDatabase.child("schedules")
    private var memoDatabaseReference = firebaseDatabase.child("memos")
    private val user = FirebaseAuth.getInstance().currentUser

    private val gson = Gson()

    fun fetch(
        yearMonths: List<YearMonth>,
        onSuccess: (List<CalendarEntity>) -> Unit
    ) {
        val handlerThread = HandlerThread("events-fetching")
        handlerThread.start()

        val backgroundHandler = Handler(handlerThread.looper)
        val mainHandler = Handler(Looper.getMainLooper())

        val eventList = arrayListOf<Event>()
        scheduleDatabaseReference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SimpleDateFormat")
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val children = dataSnapshot.children
                for (postSnapshot in children) {
                    var event = postSnapshot.getValue<Event>()!!
                    event.id = postSnapshot.key!!
                    if (event.email == user?.email) {
                        if (event.startDate != event.endDate) {
                            val formatter = SimpleDateFormat("MM/dd/yyyy")
                            var start: Date? = formatter.parse(event.startDate!!)
                            val end: Date? = formatter.parse(event.endDate!!)
                            if (start != null) {
                                while (start!!.before(end)) {
                                    val calendar: Calendar = Calendar.getInstance()
                                    calendar.time = start
                                    val startDate = String.format("%02d/%02d/%04d", calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.YEAR))
                                    val tempEvent = Event(event.email, startDate, startDate, event.startTime, event.endTime, event.name + "|schedule " + event.id, event.backgroundHexColor, event.id)
                                    eventList.add(tempEvent)
                                    calendar.add(Calendar.DATE, 1)
                                    start = calendar.time
                                }
                            }
                        } else {
                            val tempEvent = Event(event.email, event.startDate, event.endDate, event.startTime, event.endTime, event.name + "|schedule " + event.id, event.backgroundHexColor, event.id)
                            eventList.add(tempEvent)
                        }
                    }
                }
                Log.d("event-repository", eventList.toString())
                memoDatabaseReference.addValueEventListener(object : ValueEventListener {
                    @SuppressLint("SimpleDateFormat")
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val children = dataSnapshot.children
                        for (postSnapshot in children) {
                            var event = postSnapshot.getValue<Event>()!!
                            event.id = postSnapshot.key!!
                            if (event.email == user?.email) {
                                if (event.startDate != event.endDate) {
                                    val formatter = SimpleDateFormat("MM/dd/yyyy")
                                    var start: Date? = formatter.parse(event.startDate!!)
                                    val end: Date? = formatter.parse(event.endDate!!)
                                    if (start != null) {
                                        while (start!!.before(end)) {
                                            val calendar: Calendar = Calendar.getInstance()
                                            calendar.time = start
                                            val startDate = String.format("%02d/%02d/%04d", calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.YEAR))
                                            val tempEvent = Event(event.email, startDate, startDate, event.startTime, event.endTime, event.name + "|memo " + event.id, event.backgroundHexColor, event.id)
                                            eventList.add(tempEvent)
                                            calendar.add(Calendar.DATE, 1)
                                            start = calendar.time
                                        }
                                    }
                                } else {
                                    val tempEvent = Event(event.email, event.startDate, event.endDate, event.startTime, event.endTime, event.name + "|memo " + event.id, event.backgroundHexColor, event.id)
                                    eventList.add(tempEvent)
                                }
                            }
                        }
                        Log.d("event-repository", eventList.toString())
                        val apiEntities: List<ApiResult> = gson.fromJson(gson.toJson(eventList), eventResponseType)
                        val calendarEntities = apiEntities.mapIndexedNotNull { index, apiResult ->
                            Log.d("apiEntities", apiEntities.toString() + index)
                            apiResult.toCalendarEntity(yearMonths[0], index, "")
                        }
                        Log.d("calendarEntities", calendarEntities.toString())
                        onSuccess(calendarEntities)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.w("error", "loadPost:onCancelled", databaseError.toException())
                    }
                })
                val apiEntities: List<ApiResult> = gson.fromJson(gson.toJson(eventList), eventResponseType)
                val calendarEntities = apiEntities.mapIndexedNotNull { index, apiResult ->
                    Log.d("apiEntities", apiEntities.toString() + index)
                    apiResult.toCalendarEntity(yearMonths[0], index, "- S -")
                }
                Log.d("calendarEntities", calendarEntities.toString())
                onSuccess(calendarEntities)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("error", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    private fun fetchEvents(): List<ApiResult> {
        val events: ArrayList<ApiResult> = arrayListOf()
        loadSchedule {
            val str = gson.toJson(it);
            Log.d("str-str", str)
        }
        val inputStream = context.assets.open("events.json")
        val json = inputStream.reader().readText()
        return gson.fromJson(json, eventResponseType)
    }

    private fun fetchBlockedTimes(): List<ApiResult> {
        val inputStream = context.assets.open("blocked_times.json")
        val json = inputStream.reader().readText()
        return gson.fromJson(json, blockedTimeResponseType)
    }

    private fun loadSchedule(callback: (List<ApiResult>) -> Unit) {
        val eventList = arrayListOf<Event>()
        scheduleDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val children = dataSnapshot.children
                for (postSnapshot in children) {
                    var event = postSnapshot.getValue<Event>()!!
                    event.id = postSnapshot.key!!
                    if (event.email == user?.email) eventList.add(event)
                }
                Log.d("initial-memos", eventList.toString())
                val apiEntities: List<ApiResult> = gson.fromJson(gson.toJson(eventList), eventResponseType)
                callback(apiEntities)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("error", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }
}
