package com.nmd.eventCalendar.data

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
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
import java.time.YearMonth

class EventsRepository(private val context: Context) {

    private val eventResponseType = object : TypeToken<List<ApiEvent>>() {}.type
    private val blockedTimeResponseType = object : TypeToken<List<ApiBlockedTime>>() {}.type

    var firebaseDatabase = Firebase.database.reference
    var scheduleDatabaseReference = firebaseDatabase.child("schedules")
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

        backgroundHandler.post {
            val apiEntities = fetchEvents() + fetchBlockedTimes()

            val calendarEntities = yearMonths.flatMap { yearMonth ->
                apiEntities.mapIndexedNotNull { index, apiResult ->
                    apiResult.toCalendarEntity(yearMonth, index)
                }
            }

            mainHandler.post {
                onSuccess(calendarEntities)
            }
        }
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

    private fun loadSchedule(callback: (ArrayList<Event>) -> Unit) {
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
                callback(eventList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("error", "loadPost:onCancelled", databaseError.toException())
            }
        })
    }
}
