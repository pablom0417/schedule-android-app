package com.nmd.eventCalendar

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.alamkanak.weekview.WeekViewEntity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.jakewharton.threetenabp.AndroidThreeTen
import com.nmd.eventCalendar.MainActivity.RandomEventList.Companion.createRandomEventList
import com.nmd.eventCalendar.`interface`.EventCalendarDayClickListener
import com.nmd.eventCalendar.`interface`.EventCalendarScrollListener
import com.nmd.eventCalendar.model.Day
import com.nmd.eventCalendar.model.Event
import com.nmd.eventCalendarSample.R
import com.nmd.eventCalendarSample.databinding.ActivityMainBinding
import com.nmd.eventCalendarSample.databinding.BottomSheetBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Calendar

import com.alamkanak.weekview.jsr310.WeekViewPagingAdapterJsr310
import com.alamkanak.weekview.jsr310.setDateFormatter
import com.alamkanak.weekview.sample.data.model.CalendarEntity
import com.alamkanak.weekview.sample.data.model.toWeekViewEntity
import com.alamkanak.weekview.sample.util.GenericAction
import com.alamkanak.weekview.sample.util.defaultDateTimeFormatter
import com.alamkanak.weekview.sample.util.genericViewModel
import com.alamkanak.weekview.sample.util.showToast
import com.alamkanak.weekview.sample.util.subscribeToEvents
import com.alamkanak.weekview.sample.util.yearMonthsBetween
import com.nmd.eventCalendar.databinding.EcvTextviewCircleBinding
import com.nmd.eventCalendar.model.Schedule
import java.time.format.DateTimeFormatter
import java.util.Locale

open class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selected: Boolean = false
    private var selectedDate: String = ""
    private var prevSelectedDate: String = ""
    private var currentScheduleStartDate: String = ""
    private var currentScheduleEndDate: String = ""
    private var isWeekView = false
    private var isScheduleMode = false

    @SuppressLint("StaticFieldLeak")
    var logoutButton: ImageView? = null
    var fullName: TextView? = null
    var firebaseAuth: FirebaseAuth? = null
    private val authStateListener: AuthStateListener? = null

    var dataModels: ArrayList<DataModel>? = null
    private var adapter: CustomAdapter? = null

    private val viewModel by genericViewModel()

    @RequiresApi(Build.VERSION_CODES.O)
    private val weekdayFormatter = DateTimeFormatter.ofPattern("E", Locale.getDefault())
    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        AndroidThreeTen.init(this);

        initialize()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun initialize() {
        with(binding) {
            progressBar.visibility = View.VISIBLE
            eventCalendarView.visibility = View.GONE

            val toggle = ActionBarDrawerToggle(
                this@MainActivity,
                drawerLayout,
                binding.toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )
            drawerLayout.addDrawerListener(toggle)
            toggle.drawerArrowDrawable.color = resources.getColor(R.color.white)
            toggle.syncState()

            val navHeader: View = navView.getHeaderView(0)
            val username = navHeader.findViewById(R.id.textViewUserName) as TextView

            val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                username.text = user.email
            }

            val year = Calendar.getInstance().get(Calendar.YEAR)

            eventCalendarView.setMonthAndYear(
                startMonth = 1, startYear = year - 10, endMonth = 12, endYear = year + 10
            )
            eventCalendarViewCalendarImageView.setOnClickListener {
                eventCalendarView.scrollToCurrentMonth(false)
            }
            backButton.visibility = View.INVISIBLE

            backButton.setOnClickListener {
                calendarStatus.text = "My Calendar"
                backButton.visibility = View.INVISIBLE
            }

            viewSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    eventCalendarView.visibility = View.GONE
                    weekCalendarView.visibility = View.VISIBLE
                    addSwitch.visibility = View.VISIBLE
                    viewSwitch.text = "Weekly View"
                    addSwitch.setOnCheckedChangeListener { _, isChk ->
                        if (isChk) {
                            addSwitch.text = "Add schedule"
                        } else {
                            addSwitch.text = "Add memo"
                        }
                        isScheduleMode = isChk
                    }
                } else {
                    eventCalendarView.visibility = View.VISIBLE
                    weekCalendarView.visibility = View.GONE
                    addSwitch.visibility = View.GONE
                    viewSwitch.text = "Monthly View"
                }
                isWeekView = isChecked
            }

            val weekViewAdapter = BasicActivityWeekViewAdapter(
                dragHandler = viewModel::handleDrag,
                loadMoreHandler = viewModel::fetchEvents,
            )

            weekCalendarView.adapter = weekViewAdapter

            weekCalendarView.setDateFormatter { date: LocalDate ->
                val weekdayLabel = weekdayFormatter.format(date)
                val dateLabel = dateFormatter.format(date)
                weekdayLabel + "\n" + dateLabel
            }

            viewModel.viewState.observe(this@MainActivity) { viewState ->
                weekViewAdapter.submitList(viewState.entities)
            }

            viewModel.actions.subscribeToEvents(this@MainActivity) { action ->
                when (action) {
                    is GenericAction.ShowSnackbar -> {
                        Snackbar
                            .make(weekCalendarView, action.message, Snackbar.LENGTH_SHORT)
                            .setAction("Undo") { action.undoAction() }
                            .show()
                    }

                }
            }

            eventCalendarLogoutImageView.setOnClickListener {
                progressBar.visibility = View.VISIBLE
                eventCalendarView.visibility = View.GONE

                createRandomEventList(256) {
                    eventCalendarView.events = it
                    eventCalendarView.post {
                        progressBar.visibility = View.GONE
                        eventCalendarView.visibility = View.VISIBLE
                    }
                }
            }

            eventCalendarView.addOnDayClickListener(object : EventCalendarDayClickListener {
                @SuppressLint("SimpleDateFormat")
                override fun onClick(day: Day) {
//                    if (selectedDate == day.date && selected) {
//                        val eventList = eventCalendarView.events.filter { it.date == day.date }
//                        bottomSheet(day, eventList)
//                        selected = false
//                    } else {
//                        selectedDate = day.date
//                        selected = true
//                    }
//                    if (!isScheduleMode) {
//                        showInputDialog(day.date, day.date)
//                    }
//                    else {
//                        Log.d("day.date", day.date)
//                        Log.d("currentScheduleStartDate", currentScheduleStartDate)
//                        Log.d("currentScheduleEndDate", currentScheduleEndDate)
//                        if (selected && prevSelectedDate != "") {
//                            Log.d("datedate", "--------")
//                            val formatter = SimpleDateFormat("MM/dd/yyyy")
//                            var sDate = formatter.parse(prevSelectedDate)
//                            var eDate = formatter.parse(selectedDate)
//                            if (prevSelectedDate <= selectedDate) {
//                                currentScheduleStartDate = prevSelectedDate
//                                currentScheduleEndDate = selectedDate
//                            } else {
//                                currentScheduleStartDate = selectedDate
//                                currentScheduleEndDate = prevSelectedDate
//                            }
//                            if (day.date in currentScheduleStartDate..currentScheduleEndDate) {
//                                Log.d("datedate", "++++++++")
//                                showInputDialog(currentScheduleStartDate, currentScheduleEndDate)
//                            }
//                            selected = false
//                            prevSelectedDate = ""
//                            selectedDate = ""
//                        } else {
//                            Log.d("datedate", "oooooooo")
//                            selected = true
//                            prevSelectedDate = selectedDate
//                            selectedDate = day.date
//                        }
//                    }
                }
            })

            eventCalendarView.addOnCalendarScrollListener(object : EventCalendarScrollListener {
                override fun onScrolled(month: Int, year: Int) {
                    Log.i("ECV", "Scrolled to: $month $year")
                }
            })

            createRandomEventList(256) {
                eventCalendarView.events = it
                eventCalendarView.post {
                    progressBar.visibility = View.GONE
                    eventCalendarView.visibility = View.VISIBLE
                }
            }

//            floatingActionButton.setOnClickListener {
//                showInputDialog("", "")
//            }

            eventCalendarNotificationImageView.setOnClickListener {it ->
                Snackbar.make(
                    it,
                    "John Doe have added a new schedule",
                    Snackbar.LENGTH_LONG
                ).setBackgroundTint(Color.parseColor("#e18900"))
                    .setAction("Visit") { showAcceptConfirmDialog(dataModels!![0]) }.show()
            }

            sideLogoutButton.setOnClickListener {
                showLogoutConfirmDialog()
            }

            eventCalendarLogoutImageView.setOnClickListener(View.OnClickListener {
                showLogoutConfirmDialog()
            })

            dataModels = ArrayList()

            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "invited"))
            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "pending"))
            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "invited"))
            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "invited"))
            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "pending"))
            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "invited"))
            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "invited"))

            adapter = CustomAdapter(dataModels!!, applicationContext)

            list.adapter = adapter
            list.setOnItemClickListener { parent, view, position, id ->
                val dataModel: DataModel = dataModels!![position]
                showAcceptConfirmDialog(dataModel)
            }

            sideInvitationListButton.setOnClickListener {
                drawerLayout.closeDrawer(navView)
            }
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    protected fun showInputDialog(prevMemo: String, startDate: String, endDate: String) {
        // get prompts.xml view
        val layoutInflater = LayoutInflater.from(this@MainActivity)
        val promptView: View = layoutInflater.inflate(R.layout.add_dialog_content, null)
        val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
        if (prevMemo != "") alertDialogBuilder.setTitle("Edit memo") else alertDialogBuilder.setTitle("Add memo")
        alertDialogBuilder.setView(promptView)
        val inputStartDate = promptView.findViewById<View>(R.id.input_start_date) as TextInputLayout
        val inputEndDate = promptView.findViewById<View>(R.id.input_end_date) as TextInputLayout
        val inputMemo = promptView.findViewById<View>(R.id.input_memo) as TextInputLayout
        inputStartDate.editText?.setText(startDate)
        inputEndDate.editText?.setText(endDate)
        inputMemo.editText?.setText(prevMemo)
        inputStartDate.editText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pickDate(inputStartDate.editText!!)
            }
        }
        inputEndDate.editText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pickDate(inputEndDate.editText!!)
            }
        }
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, id -> Toast.makeText(this@MainActivity, "New memo successfully added.", Toast.LENGTH_SHORT).show() })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, id ->  })

        // create an alert dialog
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.background = getDrawable(R.drawable.background_rounded)
        alertDialogBuilder.show()
    }

    private fun showScheduleModal(prevSchedule: String, start: String, end: String) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(this)
        alertDialogBuilder.setTitle("Add Schedule Content")
        val promptView = LayoutInflater.from(this).inflate(com.nmd.eventCalendar.R.layout.add_schedule_content, null)
        alertDialogBuilder.setView(promptView)
        val startDate = promptView.findViewById<View>(com.nmd.eventCalendar.R.id.schedule_start_date) as TextInputLayout
        val endDate = promptView.findViewById<View>(com.nmd.eventCalendar.R.id.schedule_end_date) as TextInputLayout
        val scheduleContent = promptView.findViewById<View>(com.nmd.eventCalendar.R.id.schedule_content) as TextInputLayout
        if (start.split("/")[0].toInt() <= end.split("/")[0].toInt() && start.split("/")[1].toInt() <= end.split("/")[1].toInt()) {
            startDate.editText?.setText(start)
            endDate.editText?.setText(end)
        } else {
            startDate.editText?.setText(end)
            endDate.editText?.setText(start)
        }
        scheduleContent.editText?.setText(prevSchedule)
        startDate.editText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pickDate(startDate.editText!!)
            }
        }
        endDate.editText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pickDate(endDate.editText!!)
            }
        }
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("Save"
            ) { _, _ ->
                Toast.makeText(
                    this,
                    "New schedule has been added.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel"
            ) { _, _ ->

            }

        // create an alert dialog
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.show()
    }

    private fun showAcceptConfirmDialog(dataModel: DataModel) {
        val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
        alertDialogBuilder.setTitle("View others' calendar")
        alertDialogBuilder.setMessage("Do you really want to see ${dataModel.name}'s calendar?")
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("Yes",
                DialogInterface.OnClickListener { dialog, id ->
                    binding.drawerLayout.closeDrawer(binding.navView)
                    binding.calendarStatus.text = "${dataModel.name}'s calendar"
                    binding.backButton.visibility = View.VISIBLE
                    binding.weekCalendarView.visibility = View.GONE
                    binding.eventCalendarView.visibility = View.VISIBLE
                    isScheduleMode = false
                    binding.viewSwitch.isChecked = false
                    Toast.makeText(this@MainActivity, "You are seeing ${dataModel.name}'s calendar now.", Toast.LENGTH_SHORT).show()
                })
            .setNegativeButton("No",
                DialogInterface.OnClickListener { dialog, id ->  })

        // create an alert dialog
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.show()
    }

    protected fun showLogoutConfirmDialog() {
        val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
        alertDialogBuilder.setTitle("Sign out")
        alertDialogBuilder.setMessage("Are you sure you want to sign out?")
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("Sign out",
                DialogInterface.OnClickListener { dialog, id ->
                    run {

                        FirebaseAuth.getInstance().signOut()
                        openLoginActivity()
                        Toast.makeText(
                            this@MainActivity,
                            "Successfully signed out!",
                            Toast.LENGTH_SHORT
                        ).show()
                        Toast.makeText(
                            this@MainActivity,
                            "You have been signed out.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            .setNegativeButton("Keep signing in",
                DialogInterface.OnClickListener { dialog, id ->  })

        // create an alert dialog
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.show()
    }

    private fun pickDate(component: EditText) {
        val c = Calendar.getInstance()
        // our day, month and year.
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        // variable for date picker dialog.
        val datePickerDialog = DatePickerDialog(
            this,
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
            this,
            { _, hourOfDay, minutes ->
                val time = String.format("%02d:%02d", hourOfDay, minutes)
                component.setText("$prevText $time")
            },
            hour, minute, true
        )
        timePickerDialog.show()
    }

    private fun openLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(15)
    }

    @SuppressLint("SetTextI18n")
    private fun bottomSheet(day: Day, eventList: List<Event>) {
        val binding = BottomSheetBinding.inflate(LayoutInflater.from(this))
        val bottomSheetDialog =
            BottomSheetDialog(this, R.style.BottomSheetDialog)

        binding.bottomSheetMaterialTextView.text = day.date + " (" + eventList.size + " memos)"
        binding.bottomSheetNoEventsMaterialTextView.visibility =
            if (eventList.isEmpty()) View.VISIBLE else View.GONE

        binding.bottomSheetRecyclerView.adapter = SheetEventsAdapter(ArrayList(eventList))

        bottomSheetDialog.setContentView(binding.root)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true
        bottomSheetDialog.setCancelable(true)
//        binding.addNewMemoButton.setOnClickListener {
//            showInputDialog("", "")
//        }

        bottomSheetDialog.show()
    }

    data class RandomEventList(
        var name: String,
        val color: String,
    ) {
        companion object {
            private val list = ArrayList<RandomEventList>().apply {
                add(RandomEventList("Meeting", "#e07912"))
                add(RandomEventList("Vacation", "#4badeb"))
                add(RandomEventList("Birthday Party", "#ff6f00"))
                add(RandomEventList("Concert", "#d500f9"))
                add(RandomEventList("Job Interview", "#7cb342"))
            }

            fun createRandomEventList(numRandomEvents: Int, callback: (ArrayList<Event>) -> Unit) {
                CoroutineScope(Dispatchers.IO).launch {
                    val currentDate = Calendar.getInstance()
                    val currentYear = currentDate.get(Calendar.YEAR)

                    val eventList = arrayListOf<Event>()
                    val eventsPerMonth = numRandomEvents / 12

                    for (month in 1..12) {
                        val calendar = Calendar.getInstance().apply {
                            set(currentYear, month - 1, 1)
                        }
                        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

                        val randomEvents = arrayListOf<RandomEventList>()
                        repeat(list.size) {
                            val randomEvent = list.random()
                            randomEvents.add(randomEvent)
                        }

                        val eventsForMonth = arrayListOf<Event>()
                        for (i in 1..eventsPerMonth) {
                            val randomEvent = randomEvents.random()
                            val randomDay = (1..daysInMonth).random()
                            val dateStr =
                                String.format("%02d/%02d/%04d", month, randomDay, currentYear)
                            val newEvent = Event(dateStr, randomEvent.name, randomEvent.color)
                            eventsForMonth.add(newEvent)
                        }

//                        eventList.addAll(eventsForMonth.shuffled())
                    }
                    eventList.apply {
                        add(Event("11/16/2023", "Meeting", "#e07912"))
                        add(Event("11/17/2023", "Meeting", "#e07912"))
                        add(Event("11/18/2023", "Meeting", "#e07912"))
                        add(Event("11/13/2023", "Vacation", "#4badeb"))
                        add(Event("11/14/2023", "Vacation", "#4badeb"))
                        add(Event("11/15/2023", "Vacation", "#4badeb"))
                        add(Event("11/14/2023", "Birthday Party", "#ff6f00"))
                        add(Event("11/09/2023", "Meeting", "#e07912"))
                        add(Event("11/23/2023", "Meeting", "#e07912"))
                        add(Event("11/21/2023", "Concert", "#d500f9"))
                        add(Event("11/15/2023", "Meeting", "#e07912"))
                        add(Event("11/12/2023", "Meeting", "#e07912"))
                        add(Event("11/19/2023", "Concert", "#d500f9"))
                        add(Event("11/20/2023", "Concert", "#d500f9"))
                        add(Event("11/27/2023", "Job Interview", "#7cb342"))
                    }

                    withContext(Dispatchers.Main) {
                        callback.invoke(eventList)
                    }
                }
            }
        }
    }

    private inner class BasicActivityWeekViewAdapter(
        private val dragHandler: (Long, LocalDateTime, LocalDateTime) -> Unit,
        private val loadMoreHandler: (List<YearMonth>) -> Unit
    ) : WeekViewPagingAdapterJsr310<CalendarEntity>() {

        override fun onCreateEntity(item: CalendarEntity): WeekViewEntity = item.toWeekViewEntity()

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onEventClick(data: CalendarEntity, bounds: RectF) {
            if (data is CalendarEntity.Event) {
                val startDate = String.format("%02d/%02d/%04d %02d:%02d", data.startTime.monthValue, data.startTime.dayOfMonth, data.startTime.year, data.startTime.hour, data.startTime.minute)
                val endDate = String.format("%02d/%02d/%04d %02d:%02d", data.endTime.monthValue, data.endTime.dayOfMonth, data.endTime.year, data.endTime.hour, data.endTime.minute)
                if (isScheduleMode) {
                    showScheduleModal(data.title as String, startDate, endDate)
                } else {
                    showInputDialog(data.location as String, startDate, endDate)
                }
//                context.showToast("Clicked ${data.title}")
            }
        }

        override fun onEventLongClick(data: CalendarEntity, bounds: RectF): Boolean {
            val alertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
            alertDialogBuilder.setTitle("Delete schedule")
            alertDialogBuilder.setMessage("Are you sure you want to delete this schedule?")
            alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Delete"
                ) { _, _ ->
                    Toast.makeText(
                        this@MainActivity,
                        "Schedule has been deleted.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Cancel"
                ) { _, _ ->  }
            alertDialogBuilder.setCancelable(true)
            alertDialogBuilder.show()
            return false
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onEmptyViewClick(time: LocalDateTime) {
            val startDate = String.format("%02d/%02d/%04d %02d:%02d", time.monthValue, time.dayOfMonth, time.year, time.hour, 0)
            val endDate = String.format("%02d/%02d/%04d %02d:%02d", time.monthValue, time.dayOfMonth, time.year, time.hour + 1, 0)
            if (isScheduleMode) {
                if (selected) {
                    showScheduleModal("", selectedDate, startDate)
                    selected = false
                } else {
                    selectedDate = startDate
                    selected = true
                }
            } else {
                showInputDialog("", startDate, endDate)
            }
//            context.showToast("Empty view clicked at ${defaultDateTimeFormatter.format(time)}")
        }

        override fun onDragAndDropFinished(data: CalendarEntity, newStartTime: LocalDateTime, newEndTime: LocalDateTime) {
//            if (data is CalendarEntity.Event) {
//                dragHandler(data.id, newStartTime, newEndTime)
//            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onEmptyViewLongClick(time: LocalDateTime) {
//            context.showToast("Empty view long-clicked at ${defaultDateTimeFormatter.format(time)}")
        }

        override fun onLoadMore(startDate: LocalDate, endDate: LocalDate) {
            loadMoreHandler(yearMonthsBetween(startDate, endDate))
        }

        override fun onVerticalScrollPositionChanged(currentOffset: Float, distance: Float) {
            Log.d("BasicActivity", "Scrolling vertically (distance: ${distance.toInt()}, current offset ${currentOffset.toInt()})")
        }

        override fun onVerticalScrollFinished(currentOffset: Float) {
            Log.d("BasicActivity", "Vertical scroll finished (current offset ${currentOffset.toInt()})")
        }
    }
}

