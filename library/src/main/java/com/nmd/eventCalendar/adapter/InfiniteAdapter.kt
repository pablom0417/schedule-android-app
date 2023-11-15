package com.nmd.eventCalendar.adapter

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.nmd.eventCalendar.EventCalendarView
import com.nmd.eventCalendar.R
import com.nmd.eventCalendar.databinding.EcvEventCalendarViewBinding
import com.nmd.eventCalendar.databinding.EcvIncludeRowsBinding
import com.nmd.eventCalendar.databinding.EcvTextviewCircleBinding
import com.nmd.eventCalendar.model.Day
import com.nmd.eventCalendar.model.Event
import com.nmd.eventCalendar.model.Memo
import com.nmd.eventCalendar.model.Schedule
import com.nmd.eventCalendar.utils.Utils.Companion.dayEvents
import com.nmd.eventCalendar.utils.Utils.Companion.getDaysOfMonthAndGivenYear
import com.nmd.eventCalendar.utils.Utils.Companion.getMonthName
import com.nmd.eventCalendar.utils.Utils.Companion.getRealContext
import com.nmd.eventCalendar.utils.Utils.Companion.orEmptyArrayList
import com.nmd.eventCalendar.utils.Utils.Companion.orTrue
import com.nmd.eventCalendar.utils.Utils.Companion.smoothScrollTo
import kotlinx.coroutines.DelicateCoroutinesApi
import java.util.Calendar


class InfiniteAdapter(private val eventCalendarView: EventCalendarView) :
    RecyclerView.Adapter<InfiniteAdapter.AdapterViewHolder>() {

    private var selected: Boolean = false
    private var selectedDate: String = ""
    private var prevIndex: Int = -1
    private lateinit var eventCalendarViewBinding: EcvEventCalendarViewBinding
    private var isScheduleMode = false

    var firebaseDatabase = Firebase.database.reference
    var memoDatabaseReference = firebaseDatabase.child("memos")
    var memo: Memo? = Memo()
    var memos: ArrayList<Memo> = ArrayList()

    private var calendarEvents: ArrayList<Event> = ArrayList()
    private var schedule: ArrayList<Schedule> = ArrayList()

    inner class AdapterViewHolder(val binding: EcvEventCalendarViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val yearAdapterViewHolder = Calendar.getInstance().get(Calendar.YEAR)

        fun ecvTextviewCircleBindings(): ArrayList<EcvTextviewCircleBinding> {
            val listOfRows: List<EcvIncludeRowsBinding> = listOf(
                binding.eventCalendarViewRow1,
                binding.eventCalendarViewRow2,
                binding.eventCalendarViewRow3,
                binding.eventCalendarViewRow4,
                binding.eventCalendarViewRow5,
                binding.eventCalendarViewRow6
            )

            val bindingArrayList = ArrayList<EcvTextviewCircleBinding>()
            for (row in listOfRows) {
                bindingArrayList.add(row.eventCalendarViewDay1)
                bindingArrayList.add(row.eventCalendarViewDay2)
                bindingArrayList.add(row.eventCalendarViewDay3)
                bindingArrayList.add(row.eventCalendarViewDay4)
                bindingArrayList.add(row.eventCalendarViewDay5)
                bindingArrayList.add(row.eventCalendarViewDay6)
                bindingArrayList.add(row.eventCalendarViewDay7)
            }
            return bindingArrayList
        }

        init {
            binding.eventCalendarViewMonthYearHeader.visibility =
                if (eventCalendarView.headerVisible) View.VISIBLE else View.GONE

            binding.eventCalendarViewMonthYearImageViewLeft.setOnClickListener {
                eventCalendarView.binding.eventCalendarRecyclerView.smoothScrollTo(
                    currentItem - 1
                )
            }

            binding.eventCalendarViewMonthYearImageViewRight.setOnClickListener {
                eventCalendarView.binding.eventCalendarRecyclerView.smoothScrollTo(
                    currentItem + 1
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterViewHolder {
        return AdapterViewHolder(
            EcvEventCalendarViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AdapterViewHolder, position: Int) {
        with(holder.binding) {
            eventCalendarViewBinding = holder.binding
            val item = holder.bindingAdapterPosition.calculate()
            val month = item.get(0)
            val year = item.get(1)

            val monthName = month.getMonthName(root.context)
            val monthYearText = "$monthName $year"
            eventCalendarViewMonthYearTextView1?.text = monthYearText
            eventCalendarViewMonthYearTextView2?.text = monthYearText

            Log.d("entered-to-this", "+++++++++++")

            styleTextViews(
                month.getDaysOfMonthAndGivenYear(year),
                holder.ecvTextviewCircleBindings()
            )
        }
    }

    private fun pickDate(component: EditText) {
        val c = Calendar.getInstance()
        // our day, month and year.
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        // variable for date picker dialog.
        val datePickerDialog = DatePickerDialog(
            eventCalendarViewBinding.root.context,
            { _, year, monthOfYear, dayOfMonth ->
                // date to our edit text.
                val dat = String.format("%02d/%02d/%04d", monthOfYear + 1, dayOfMonth, year)
                component.setText(dat)
                pickTime(component, dat)
            },
            // and day for the selected date in our date picker.
            year,
            month,
            day
        )
        // to display our date picker dialog.
        datePickerDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun pickTime(component: EditText, prevText: String) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(
            eventCalendarViewBinding.root.context,
            { _, hourOfDay, minutes ->
                val time = String.format("%02d:%02d", hourOfDay, minutes)
                component.setText("$prevText $time")
            },
            hour, minute, true
        )
        timePickerDialog.show()
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun showInputDialog(type: String, currentDate: String, days: List<Day>, list: List<EcvTextviewCircleBinding>) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(eventCalendarViewBinding.root.context)
        val inputType = if (type != "") "Edit" else "Add"
        alertDialogBuilder.setTitle("$inputType Memo")
        val promptView = LayoutInflater.from(eventCalendarViewBinding.root.context).inflate(R.layout.add_memo_content, null)
        alertDialogBuilder.setView(promptView)
        val date = promptView.findViewById<View>(R.id.input_date) as TextInputLayout
        val inputMemo = promptView.findViewById<View>(R.id.input_memo) as TextInputLayout
//        val uploadFile = promptView.findViewById<View>(R.id.upload_file) as MaterialButton
        if (currentDate != "") date.editText?.setText("$currentDate 12:00") else date.editText?.setText("")
        date.editText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pickDate(date.editText!!)
            }
        }
        inputMemo.editText?.setText(type)
//        uploadFile.setOnClickListener {
//            openFolder()
//        }
        // setup a dialog window
        alertDialogBuilder.setCancelable(true)
            .setPositiveButton("Save"
            ) { _, _ ->
                if (TextUtils.isEmpty(date.editText?.text) || TextUtils.isEmpty(inputMemo.editText?.text)) {
                    Toast.makeText(
                        eventCalendarViewBinding.root.context,
                        "Please fill all input fields.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    memo = Memo(date.editText?.text.toString(), inputMemo.editText?.text.toString(), "#f44336")
                    memoDatabaseReference.push().setValue(memo)
                    memoDatabaseReference.addChildEventListener(object : ChildEventListener {
                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            memos.add(snapshot.getValue<Memo>()!!)
                            Log.d("memos", memos.toString())
//                            calendarEvents.add(Event(date.editText?.text.toString(), inputMemo.editText?.text.toString(), "#f44336"))
//                            styleTextViews(days, list)
                            Toast.makeText(
                                eventCalendarViewBinding.root.context,
                                "New memo has been added.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            TODO("Not yet implemented")
                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {
                            TODO("Not yet implemented")
                        }

                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            TODO("Not yet implemented")
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }

                    })
                }
            }
            .setNegativeButton("Cancel"
            ) { _, _ ->
                Toast.makeText(
                    eventCalendarViewBinding.root.context,
                    "Saving memo has been cancelled.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // create an alert dialog
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.show()
    }

    private fun openFolder() {

        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("*/*")
        startActivity(eventCalendarViewBinding.root.context,
            Intent.createChooser(intent, "Choose a file"), null)
    }

    override fun getItemCount(): Int = getMonthCount()

    private val currentItem: Int
        get() = eventCalendarView.currentRecyclerViewPosition

    private fun getMonthCount(): Int {
        val diffYear = eventCalendarView.eYear - eventCalendarView.sYear
        val diffMonth = eventCalendarView.eMonth - eventCalendarView.sMonth

        val diffTotal = diffYear * 12 + diffMonth + 1
        return maxOf(0, diffTotal)
    }

    private fun Int.calculate(): SparseIntArray {
        val year = eventCalendarView.sYear + this / 12
        val month = (this % 12 + eventCalendarView.sMonth) % 12
        return SparseIntArray().apply {
            put(0, month)
            put(1, year)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("ResourceAsColor", "MissingInflatedId")
    private fun styleTextViews(days: List<Day>, list: List<EcvTextviewCircleBinding>) {
        for ((index, day) in days.withIndex()) {
            val dayItemLayout = list[index]
            val eventList = day.dayEvents(eventCalendarView.eventArrayList.orEmptyArrayList())
            val memoCell = dayItemLayout.memoCell

            dayItemLayout.eventCalendarViewDayFrameLayout.setOnClickListener {
                eventCalendarView.clickListener?.onClick(day)
            }

            val textView: MaterialTextView = dayItemLayout.eventCalendarViewDayTextView

            val recyclerView: RecyclerView = dayItemLayout.eventCalendarViewDayRecyclerView
            with(recyclerView) {
                suppressLayout(true)
                addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                        return true
                    }
                })
                setItemViewCacheSize(100)
                setHasFixedSize(true)
                isSaveEnabled = false
                itemAnimator = null

                if (eventList.isNotEmpty()) {
                    if (eventCalendarView.countVisible) {
                        addItemDecoration(
                            LastPossibleVisibleItemForUserDecoration(
                                eventList
                            )
                        )
                    }

                    adapter = EventsAdapter(
                        list = eventList,
                        eventItemAutomaticTextColor = eventCalendarView.eventItemAutomaticTextColor.orTrue(),
                        eventItemTextColor = eventCalendarView.eventItemTextColor
                    )
                }
            }

            with(textView) {
                text = day.value

                if (day.isCurrentDay) {
                    setTextColor(eventCalendarView.currentDayTextColor)

                    context.getRealContext()?.let {
                        background =
                            ContextCompat.getDrawable(it, R.drawable.ecv_circle)
                    }

                    ViewCompat.setBackgroundTintList(
                        this,
                        ColorStateList.valueOf(eventCalendarView.currentDayBackgroundTintColor)
                    )
                }

                if (day.isCurrentMonth || day.isCurrentDay) {
                    setTypeface(typeface, Typeface.BOLD)
                } else {
                    setTypeface(typeface, Typeface.ITALIC)
                    setTextColor(R.color.ecv_item_day_name_color)
                }
            }
        }
    }

    inner class LastPossibleVisibleItemForUserDecoration(private val eventList: ArrayList<Event>) :
        RecyclerView.ItemDecoration() {
        @SuppressLint("SetTextI18n")
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            super.onDraw(c, parent, state)
            if (eventList.isEmpty()) return

            val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return
            val lastCompleteVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()

            if (lastCompleteVisiblePosition == RecyclerView.NO_POSITION || lastCompleteVisiblePosition == eventList.lastIndex) return

            val count = eventList.size - lastCompleteVisiblePosition - 1
            if (count > 0) {
                val materialTextView =
                    parent.findViewHolderForAdapterPosition(lastCompleteVisiblePosition)?.itemView as? MaterialTextView

                materialTextView?.let { textView ->
                    textView.text = "+${count.plus(1)}"
                    textView.setTextColor(eventCalendarView.countBackgroundTextColor)
                    textView.setTypeface(textView.typeface, Typeface.BOLD)

                    ViewCompat.setBackgroundTintList(
                        textView,
                        ColorStateList.valueOf(eventCalendarView.countBackgroundTintColor)
                    )
                }
            }

            for (i in lastCompleteVisiblePosition + 1..eventList.lastIndex) {
                val view = parent.findViewHolderForAdapterPosition(i)?.itemView ?: continue
                view.visibility = View.GONE
            }
        }
    }

}