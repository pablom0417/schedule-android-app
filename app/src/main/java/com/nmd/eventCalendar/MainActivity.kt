package com.nmd.eventCalendar

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
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
import androidx.appcompat.app.AlertDialog
import com.alamkanak.weekview.WeekViewEntity
import com.alamkanak.weekview.jsr310.WeekViewPagingAdapterJsr310
import com.alamkanak.weekview.jsr310.setDateFormatter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.jakewharton.threetenabp.AndroidThreeTen
import com.nmd.eventCalendar.data.model.CalendarEntity
import com.nmd.eventCalendar.data.model.toWeekViewEntity
import com.nmd.eventCalendar.`interface`.EventCalendarDayClickListener
import com.nmd.eventCalendar.`interface`.EventCalendarScrollListener
import com.nmd.eventCalendar.model.Day
import com.nmd.eventCalendar.model.Event
import com.nmd.eventCalendar.util.AppPrefs
import com.nmd.eventCalendar.util.GenericAction
import com.nmd.eventCalendar.util.defaultDateTimeFormatter
import com.nmd.eventCalendar.util.genericViewModel
import com.nmd.eventCalendar.util.showToast
import com.nmd.eventCalendar.util.subscribeToEvents
import com.nmd.eventCalendar.util.yearMonthsBetween
import com.nmd.eventCalendarSample.R
import com.nmd.eventCalendarSample.databinding.ActivityMainBinding
import com.nmd.eventCalendarSample.databinding.BottomSheetBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
open class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selected: Boolean = false
    private var selectedDate: String = ""
    private var prevSelectedDate: String = ""
    private var currentScheduleStartDate: String = ""
    private var currentScheduleEndDate: String = ""
    private var isWeekView = false
    private var isScheduleMode = false
    private var color = arrayOf(
        "#e18900", "#f44336", "#4badeb", "#AB274F", "#CA1F7B", "#0BDA51"
    )

    @SuppressLint("StaticFieldLeak")
    var logoutButton: ImageView? = null
    var fullName: TextView? = null
    var firebaseAuth: FirebaseAuth? = null
    public var user: FirebaseUser? = null
    private val authStateListener: AuthStateListener? = null

    var firebaseDatabase = Firebase.database.reference
    var memoDatabaseReference = firebaseDatabase.child("memos")
    var scheduleDatabaseReference = firebaseDatabase.child("schedules")
    var userDatabaseReference = firebaseDatabase.child("users")
    var memo: Event? = null
    var schedule: Event? = null
    var schedules: ArrayList<Event> = ArrayList()

    private val viewModel by genericViewModel()

    var dataModels: ArrayList<DataModel>? = null
    private var adapter: CustomAdapter? = null


    private val weekViewAdapter: BasicActivityWeekViewAdapter by lazy {
        BasicActivityWeekViewAdapter(
            eventClickHandler = this::showInputDialog,
            loadMoreHandler = viewModel::fetchEvents,
        )
    }

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
        AndroidThreeTen.init(this)

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
            val imageviewProfileImage = navHeader.findViewById(R.id.imageviewProfileImage) as ImageView

            val avatarImageUri = mPrefs?.avatarUrl
            if (avatarImageUri != "") {
                Picasso.get().load(avatarImageUri).into(imageviewProfileImage, object : Callback {
                    override fun onSuccess() {
                        // Convert the selected image into a ByteArray
                        val bitmap = (imageviewProfileImage.drawable as BitmapDrawable?)?.bitmap
                        val baos = ByteArrayOutputStream()
                        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                        val data: ByteArray? = baos.toByteArray()

                        // Use the 'data' to store the avatar image in Firebase along with other user details
                        // For example, you can call a separate function that handles the signup process and passes the 'data' along with other fields.
                        // signUpUser(fullName, email, password, data)
                    }

                    override fun onError(e: Exception?) {
                        // Handle error loading the image
                    }
                })
            }


            user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                username.text = user?.displayName
            }

            val year = Calendar.getInstance().get(Calendar.YEAR)

            loadList {
                eventCalendarView.events = it
                Log.d("event-list", eventCalendarView.events.toString())
                eventCalendarView.post {
                    progressBar.visibility = View.GONE
                    if (isWeekView) weekCalendarView.visibility = View.VISIBLE else eventCalendarView.visibility = View.VISIBLE
                }
            }

            loadSchedule {
                schedules = it
            }

            eventCalendarView.setMonthAndYear(
                startMonth = 1, startYear = year - 10, endMonth = 12, endYear = year + 10
            )

            weekCalendarView.adapter = weekViewAdapter

            weekCalendarView.setDateFormatter { date: LocalDate ->
                val weekdayLabel = weekdayFormatter.format(date)
                val dateLabel = dateFormatter.format(date)
                weekdayLabel + "\n" + dateLabel
            }

            viewModel.viewState.observe(this@MainActivity) { viewState ->
                Log.d("viewState", viewState.entities.toString())
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
                    addSwitch.visibility = View.GONE
                    floatingActionButton.visibility = View.GONE
                    viewSwitch.text = "Weekly View"
                } else {
                    eventCalendarView.visibility = View.VISIBLE
                    weekCalendarView.visibility = View.GONE
                    addSwitch.visibility = View.VISIBLE
                    floatingActionButton.visibility = View.VISIBLE
                    viewSwitch.text = "Monthly View"
                }
                isWeekView = isChecked
            }
            addSwitch.setOnCheckedChangeListener { _, isChk ->
                weekCalendarView.visibility = View.GONE
                eventCalendarView.visibility = View.GONE
                if (isChk) {
                    loadSchedule {
                        eventCalendarView.events = it
                        addSwitch.text = "Add schedule"
                        eventCalendarView.post {
                            progressBar.visibility = View.GONE
                            if (isWeekView) weekCalendarView.visibility = View.VISIBLE else eventCalendarView.visibility = View.VISIBLE
                        }
                    }
                } else {
                    loadList {
                        eventCalendarView.events = it
                        addSwitch.text = "Add memo"
                        eventCalendarView.post {
                            progressBar.visibility = View.GONE
                            if (isWeekView) weekCalendarView.visibility = View.VISIBLE else eventCalendarView.visibility = View.VISIBLE
                        }
                    }
                }
                isScheduleMode = isChk
            }

            floatingActionButton.setOnClickListener {
                showInputDialog(Event(user?.email))
            }

            eventCalendarView.addOnDayClickListener(object : EventCalendarDayClickListener {
                @SuppressLint("SimpleDateFormat")
                override fun onClick(day: Day) {
                    if (!isScheduleMode) {
                        val eventList = eventCalendarView.events.filter { it.startDate != null && SimpleDateFormat("MM/dd/yyyy").parse(it.startDate)!! <= SimpleDateFormat("MM/dd/yyyy").parse(day.date) &&
                                SimpleDateFormat("MM/dd/yyyy").parse(it.endDate)!! >= SimpleDateFormat("MM/dd/yyyy").parse(day.date) }
                        bottomSheet(day, eventList)
                    } else {
                        val eventList = eventCalendarView.events.filter { it.startDate != null && SimpleDateFormat("MM/dd/yyyy").parse(it.startDate)!! <= SimpleDateFormat("MM/dd/yyyy").parse(day.date) &&
                                SimpleDateFormat("MM/dd/yyyy").parse(it.endDate)!! >= SimpleDateFormat("MM/dd/yyyy").parse(day.date) }
                        bottomSheet(day, eventList)
                    }
                }
            })

            eventCalendarView.addOnCalendarScrollListener(object : EventCalendarScrollListener {
                override fun onScrolled(month: Int, year: Int) {
                    Log.i("ECV", "Scrolled to: $month $year")
                }
            })

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

//            Log.d("all-users-list", Firebase)

            dataModels = ArrayList()

//            userDatabaseReference.get().addOnSuccessListener {
//                for (snapshot in it.children) {
//                    dataModels
//                }
//            }

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

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables", "SimpleDateFormat")
    protected fun showInputDialog(event: Event?) {
        // get prompts.xml view
        val layoutInflater = LayoutInflater.from(this@MainActivity)
        val promptView: View = layoutInflater.inflate(R.layout.add_dialog_content, null)
        val alertDialogBuilder = AlertDialog.Builder(this)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel"
            ) { _, _ -> }
            .create()
        if (event?.id != null) {
            if (isScheduleMode) alertDialogBuilder.setTitle("Edit schedule") else alertDialogBuilder.setTitle("Edit memo")
        } else {
            if (isScheduleMode) alertDialogBuilder.setTitle("Add schedule") else alertDialogBuilder.setTitle("Add memo")
        }
        alertDialogBuilder.setView(promptView)
        val inputStartDate = promptView.findViewById<View>(R.id.input_start_date) as TextInputLayout
        val inputEndDate = promptView.findViewById<View>(R.id.input_end_date) as TextInputLayout
        val inputStartTime = promptView.findViewById<View>(R.id.input_start_time) as TextInputLayout
        val inputEndTime = promptView.findViewById<View>(R.id.input_end_time) as TextInputLayout
        val inputMemo = promptView.findViewById<View>(R.id.input_memo) as TextInputLayout
        if (event?.startDate != null) inputStartDate.editText?.setText(event.startDate)
        if (event?.endDate != null) inputEndDate.editText?.setText(event.endDate)
        if (event?.startTime != null) inputStartTime.editText?.setText(event.startTime)
        if (event?.endTime != null) inputEndTime.editText?.setText(event.endTime)
        if (event?.name != null) inputMemo.editText?.setText(event.name)

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
        inputStartTime.editText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pickTime(inputStartTime.editText!!)
            }
        }
        inputEndTime.editText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pickTime(inputEndTime.editText!!)
            }
        }
        // create an alert dialog
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.show()
        val positiveButton = alertDialogBuilder.getButton(AlertDialog.BUTTON_POSITIVE)
        // setup a dialog window
        positiveButton.setOnClickListener { _ ->
            if (inputStartDate.editText?.text.isNullOrEmpty() ||
                inputEndDate.editText?.text.isNullOrEmpty() ||
                inputStartTime.editText?.text.isNullOrEmpty() ||
                inputEndTime.editText?.text.isNullOrEmpty()
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "The date and time fields are required.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                memo = Event(
                    user?.email!!,
                    inputStartDate.editText?.text.toString(),
                    inputEndDate.editText?.text.toString(),
                    inputStartTime.editText?.text.toString(),
                    inputEndTime.editText?.text.toString(),
                    inputMemo.editText?.text.toString(),
                    if (event?.backgroundHexColor != null) event.backgroundHexColor else color.random()
                )
                val isExist = binding.eventCalendarView.events.any {
                    event?.id == null && it.startDate != null && it.endDate != null && it.startTime != null && it.endTime != null &&
                    it.startDate!! < memo!!.endDate!! && it.endDate!! > memo!!.startDate!! && it.startTime!! < memo!!.endTime!! && it.endTime!! > memo!!.startTime!!
                }
                Log.d("is-exist---", isExist.toString())
                if (isExist) {
                    Toast.makeText(this, "Datetime you input have been already booked.", Toast.LENGTH_LONG).show()
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.eventCalendarView.visibility = View.GONE
                    binding.weekCalendarView.visibility = View.GONE
                    binding.eventCalendarView.events = ArrayList()
                    if (event?.id == null) {
                        if (isScheduleMode) {
                            scheduleDatabaseReference.push().setValue(memo).addOnSuccessListener{
                                loadSchedule {
                                    binding.eventCalendarView.events = it
                                    binding.progressBar.visibility = View.GONE
                                    if (isWeekView) binding.weekCalendarView.visibility = View.VISIBLE else binding.eventCalendarView.visibility = View.VISIBLE
                                    Toast.makeText(
                                        this@MainActivity,
                                        "The schedule is successfully created.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    alertDialogBuilder.dismiss()
                                }
                            }.addOnFailureListener {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Creating the schedule failed.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            if (schedules.any {
                                    it.startDate!! <= memo!!.startDate!! && it.endDate!! >= memo!!.endDate!! && it.startTime!! <= memo!!.startTime!! && it.endTime!! >= memo!!.endTime!!
                                }) {
                                memoDatabaseReference.push().setValue(memo).addOnSuccessListener{
                                    loadList {
                                        binding.eventCalendarView.events = it
                                        binding.progressBar.visibility = View.GONE
                                        binding.eventCalendarView.visibility = View.VISIBLE
                                        Toast.makeText(
                                            this@MainActivity,
                                            "The memo is successfully created.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        alertDialogBuilder.dismiss()
                                    }
                                }.addOnFailureListener {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Creating the memo failed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(this, "The corresponding schedule does not exist.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        if (isScheduleMode) {
                            scheduleDatabaseReference.child(event.id!!).setValue(memo).addOnSuccessListener {
                                loadSchedule {
                                    binding.eventCalendarView.events = it
                                    binding.progressBar.visibility = View.GONE
                                    if (isWeekView) binding.weekCalendarView.visibility = View.VISIBLE else binding.eventCalendarView.visibility = View.VISIBLE
                                    Toast.makeText(
                                        this@MainActivity,
                                        "The schedule is successfully updated.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    alertDialogBuilder.dismiss()
                                }
                            }.addOnFailureListener {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Updating the schedule failed.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            if (schedules.any {
                                    it.startDate!! <= memo!!.startDate!! && it.endDate!! >= memo!!.endDate!! && it.startTime!! <= memo!!.startTime!! && it.endTime!! >= memo!!.endTime!!
                                }) {
                                memoDatabaseReference.child(event.id!!).setValue(memo).addOnSuccessListener {
                                    loadList {
                                        binding.eventCalendarView.events = it
                                        binding.progressBar.visibility = View.GONE
                                        if (isWeekView) binding.weekCalendarView.visibility = View.VISIBLE else binding.eventCalendarView.visibility = View.VISIBLE
                                        Toast.makeText(
                                            this@MainActivity,
                                            "The memo is successfully updated.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        alertDialogBuilder.dismiss()
                                    }
                                }.addOnFailureListener {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Updating the memo failed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
        }
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

    @SuppressLint("SetTextI18n")
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

    private fun showLogoutConfirmDialog() {
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

    private fun showDeleteConfirmButton(id: String) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(this)
        alertDialogBuilder.setTitle("Delete memo")
        alertDialogBuilder.setMessage("Are you sure you want to delete this memo?")
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("Delete"
            ) { _, _ ->
                binding.progressBar.visibility = View.VISIBLE
                binding.eventCalendarView.visibility = View.GONE
                binding.eventCalendarView.events = ArrayList()
                if (isScheduleMode) {
                    scheduleDatabaseReference.child(id).removeValue().addOnSuccessListener {
                        loadSchedule {
                            binding.eventCalendarView.events = it
                            binding.progressBar.visibility = View.GONE
                            binding.eventCalendarView.visibility = View.VISIBLE
                            Toast.makeText(
                                this,
                                "Memo has been deleted.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Deleting memo failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    memoDatabaseReference.child(id).removeValue().addOnSuccessListener {
                        loadList {
                            binding.eventCalendarView.events = it
                            binding.progressBar.visibility = View.GONE
                            binding.eventCalendarView.visibility = View.VISIBLE
                            Toast.makeText(
                                this,
                                "Memo has been deleted.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Deleting memo failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel"
            ) { _, _ ->  }
        alertDialogBuilder.setCancelable(false)
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
    private fun pickTime(component: EditText) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minutes ->
                val time = String.format("%02d:%02d", hourOfDay, minutes)
                Log.d("picked_time", time)
                component.setText(time)
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

    @SuppressLint("SimpleDateFormat")
    private fun getAllDates(mDate1: String, mDate2: String) {
        // Creating a date format
        val mDateFormat = SimpleDateFormat("MM/dd/yyyy")

        // Converting the dates
        // from string to date format
        val mDate11 = mDateFormat.parse(mDate1)
        val mDate22 = mDateFormat.parse(mDate2)
        val cal1 = Calendar.getInstance()
        cal1.time = mDate11!!

        val cal2 = Calendar.getInstance()
        cal2.time = mDate22!!

        while (!cal1.after(cal2)) {
            Log.d("all dates", String.format("%02d/%02d/%04d", cal1.get(Calendar.MONTH) + 1, cal1.get(Calendar.DAY_OF_MONTH), cal1.get(Calendar.YEAR)))
            cal1.add(Calendar.DATE, 1)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun bottomSheet(day: Day, eventList: List<Event>) {
        val binding = BottomSheetBinding.inflate(LayoutInflater.from(this))
        val bottomSheetDialog =
            BottomSheetDialog(this, R.style.BottomSheetDialog)
        val size = eventList.size
        var sheetHeaderText = ""
        sheetHeaderText = if (size == 0) {
            if (isScheduleMode) day.date + " (No schedules)" else day.date + " (No memos)"
        } else {
            if (size == 1) {
                if (isScheduleMode) day.date + " (" + eventList.size + " schedule)" else day.date + " (" + eventList.size + " memo)"
            } else {
                if (isScheduleMode) day.date + " (" + eventList.size + " schedules)" else day.date + " (" + eventList.size + " memos)"
            }
        }

        binding.bottomSheetMaterialTextView.text = sheetHeaderText
        binding.bottomSheetNoEventsMaterialTextView.visibility =
            if (eventList.isEmpty()) View.VISIBLE else View.GONE

        binding.bottomSheetNoEventsMaterialTextView.text = if (isScheduleMode) "No schedules were found for the selected day." else "No schedules were found for the selected day."
        val sheetEventsAdapter = SheetEventsAdapter(ArrayList(eventList))
        binding.bottomSheetRecyclerView.adapter = sheetEventsAdapter
        sheetEventsAdapter.setOnClickListener(object :
            SheetEventsAdapter.OnClickListener {
            override fun onClick(position: Int, model: Event) {
                showInputDialog(model)
                bottomSheetDialog.cancel()
            }
        })

        sheetEventsAdapter.setOnLongClickListener(object : SheetEventsAdapter.OnLongClickListener {
            override fun onLongClick(position: Int, model: Event) {
                if (model.id != null) {
                    showDeleteConfirmButton(model.id!!)
                    bottomSheetDialog.cancel()
                }
            }
        })

        bottomSheetDialog.setContentView(binding.root)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true
        bottomSheetDialog.setCancelable(true)
        binding.addNewMemoButton.text = if (isScheduleMode) "Add schedule" else "Add memo"
        binding.addNewMemoButton.setOnClickListener {
            showInputDialog(Event(user?.email, day.date, day.date))
            bottomSheetDialog.cancel()
        }

        bottomSheetDialog.show()
    }

    private fun loadList(callback: (ArrayList<Event>) -> Unit) {
        val eventList = arrayListOf<Event>()
        memoDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val children = dataSnapshot.children
                for (postSnapshot in children) {
                    val event = postSnapshot.getValue<Event>()!!
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

    inner class BasicActivityWeekViewAdapter(
        private val eventClickHandler: (Event?) -> Unit,
        private val loadMoreHandler: (Boolean, List<YearMonth>) -> Unit
    ) : WeekViewPagingAdapterJsr310<CalendarEntity>() {
        override fun onCreateEntity(item: CalendarEntity): WeekViewEntity = item.toWeekViewEntity()

        override fun onEventClick(data: CalendarEntity, bounds: RectF) {
//            if (data is CalendarEntity.Event) {
//                val schedule = scheduleDatabaseReference.child(data.location as String)
//                scheduleDatabaseReference.child(data.location.split("|")[1].split(" ")[0]).get().addOnSuccessListener {
//                    Log.d("all-event--", it.toString())
//                    if (it.value != null) {
//                        binding.addSwitch.isChecked = true
//                        isScheduleMode = true
//                        val event = it.getValue<Event>()!!
//                        event.id = it.key
//                        eventClickHandler(event)
//                    } else {
//                        binding.addSwitch.isChecked = false
//                        isScheduleMode = false
//                        memoDatabaseReference.child(data.location.split("|")[1].split(" ")[0]).get().addOnSuccessListener { snapshot ->
//                            Log.d("all-event--", snapshot.toString())
//                            if (snapshot.value != null) {
//                                val event = snapshot.getValue<Event>()!!
//                                event.id = snapshot.key
//                                eventClickHandler(event)
//                            }
//                        }.addOnFailureListener {
//                            Toast.makeText(context, "Editing memo failed.", Toast.LENGTH_LONG).show()
//                        }
//                    }
//                }.addOnFailureListener {
//                    Toast.makeText(context, "Editing schedule failed.", Toast.LENGTH_LONG).show()
//                }
//            }
        }

        override fun onEmptyViewClick(time: LocalDateTime) {
//            val mPrefs: AppPrefs = AppPrefs.create(context)
//            Log.d("mPrefs", mPrefs.toString())
//            eventClickHandler(Event(mPrefs.email))
        }

        override fun onDragAndDropFinished(data: CalendarEntity, newStartTime: LocalDateTime, newEndTime: LocalDateTime) {
            if (data is CalendarEntity.Event) {
//                dragHandler(data.id, newStartTime, newEndTime)
            }
        }

        override fun onEmptyViewLongClick(time: LocalDateTime) {
//            context.showToast("Empty view long-clicked at ${defaultDateTimeFormatter.format(time)}")
        }

        override fun onLoadMore(startDate: LocalDate, endDate: LocalDate) {
            loadMoreHandler(isScheduleMode, yearMonthsBetween(startDate, endDate))
        }

        override fun onVerticalScrollPositionChanged(currentOffset: Float, distance: Float) {
            Log.d("BasicActivity", "Scrolling vertically (distance: ${distance.toInt()}, current offset ${currentOffset.toInt()})")
        }

        override fun onVerticalScrollFinished(currentOffset: Float) {
            Log.d("BasicActivity", "Vertical scroll finished (current offset ${currentOffset.toInt()})")
        }
    }
}

