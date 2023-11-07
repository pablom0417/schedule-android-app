package com.nmd.eventCalendar

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.nmd.eventCalendar.MainActivity.RandomEventList.Companion.createRandomEventList
import com.nmd.eventCalendar.`interface`.EventCalendarDayClickListener
import com.nmd.eventCalendar.`interface`.EventCalendarScrollListener
import com.nmd.eventCalendar.model.Day
import com.nmd.eventCalendar.model.Event
import com.nmd.eventCalendarSample.R
import com.nmd.eventCalendarSample.databinding.ActivityMainBinding
import com.nmd.eventCalendarSample.databinding.BottomSheetBinding
import com.nmd.eventCalendarSample.databinding.BottomSheetSingleWeekBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


open class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("StaticFieldLeak")
    var logoutButton: ImageView? = null
    var fullName: TextView? = null
    var firebaseAuth: FirebaseAuth? = null
    private val authStateListener: AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initialize()
    }

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
            toggle.syncState()

            val navHeader: View = navView.getHeaderView(0)
            val username = navHeader.findViewById(R.id.textViewUserName) as TextView

            val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Log.d("user-----", user.email!!)
                Log.d("user++++", username.text as String)
                username.text = user.email
            }

            val year = Calendar.getInstance().get(Calendar.YEAR)

            eventCalendarView.setMonthAndYear(
                startMonth = 1, startYear = year, endMonth = 12, endYear = year
            )
            eventCalendarViewCalendarImageView.setOnClickListener {
                eventCalendarView.scrollToCurrentMonth(false)
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
                override fun onClick(day: Day) {
                    val eventList = eventCalendarView.events.filter { it.date == day.date }
                    bottomSheet(day, eventList)
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

            floatingActionButton.setOnClickListener {
                showInputDialog()
            }

            eventCalendarNotificationImageView.setOnClickListener {
                drawerLayout.openDrawer(navView)
            }

            sideLogoutButton.setOnClickListener {
                showLogoutConfirmDialog()
            }

            eventCalendarLogoutImageView.setOnClickListener(View.OnClickListener {
                showLogoutConfirmDialog()
            })

            navHeader.findViewById<MaterialTextView>(R.id.notificationMaterialTextView).setOnClickListener {
                showAcceptConfirmDialog()
                drawerLayout.closeDrawer(navView)
            }

            sideInvitationListButton.setOnClickListener {
                val intent = Intent(this@MainActivity, InvitationActivity::class.java)
                startActivity(intent)
            }
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    protected fun showInputDialog() {
        // get prompts.xml view
        val layoutInflater = LayoutInflater.from(this@MainActivity)
        val promptView: View = layoutInflater.inflate(R.layout.add_dialog_content, null)
        val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
        alertDialogBuilder.setTitle(R.string.dialog_title)
        alertDialogBuilder.setView(promptView)
        val inputStartDate = promptView.findViewById<View>(R.id.input_start_date) as TextInputLayout
        val inputEndDate = promptView.findViewById<View>(R.id.input_end_date) as TextInputLayout
        val inputMemo = promptView.findViewById<View>(R.id.input_memo) as TextInputLayout
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

    private fun showAcceptConfirmDialog() {
        val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
        alertDialogBuilder.setTitle("Accept Invitation")
        alertDialogBuilder.setMessage("Are you sure you want to invite John Doe?")
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("Accept",
                DialogInterface.OnClickListener { dialog, id -> Toast.makeText(this@MainActivity, "You have invited John Doe.", Toast.LENGTH_SHORT).show() })
            .setNegativeButton("Decline",
                DialogInterface.OnClickListener { dialog, id -> Toast.makeText(this@MainActivity, "You have declined John Doe.", Toast.LENGTH_SHORT).show() })

        // create an alert dialog
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.show()
    }

    protected fun showLogoutConfirmDialog() {
        val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
        alertDialogBuilder.setTitle("Logout")
        alertDialogBuilder.setMessage("Are you sure you want to log out?")
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("Logout",
                DialogInterface.OnClickListener { dialog, id ->
                    run {

                        FirebaseAuth.getInstance().signOut()
                        openLoginActivity()
                        Toast.makeText(
                            this@MainActivity,
                            "Successfully logged out!",
                            Toast.LENGTH_SHORT
                        ).show()
                        Toast.makeText(
                            this@MainActivity,
                            "You have been logged out.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            .setNegativeButton("Keep login",
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
                val dat = ((monthOfYear + 1).toString() + "/" + dayOfMonth + "/" + year)
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

    private fun pickTime(component: EditText, date: String) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this,
            OnTimeSetListener { view, hourOfDay, minute ->
                val formatTime = "$hourOfDay:$minute"
                val dateTime: String = "$date $formatTime"
                component.setText(dateTime)
            }, hour + 1, minute, false
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
        binding.addNewMemoButton.setOnClickListener {
            showInputDialog()
        }

        bottomSheetDialog.show()
    }

    private fun bottomSheet2() {
        val binding = BottomSheetSingleWeekBinding.inflate(LayoutInflater.from(this))
        val bottomSheetDialog =
            BottomSheetDialog(this, R.style.BottomSheetDialog)

        binding.bottomSheetEventCalendarSingleWeekView.events =
            this@MainActivity.binding.eventCalendarView.events
        binding.bottomSheetEventCalendarSingleWeekView.addOnDayClickListener(object :
            EventCalendarDayClickListener {
            override fun onClick(day: Day) {
                bottomSheetDialog.dismiss()

                val eventList =
                    this@MainActivity.binding.eventCalendarView.events.filter { it.date == day.date }
                bottomSheet(day, eventList)
            }
        })

        bottomSheetDialog.setContentView(binding.root)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true
        bottomSheetDialog.setCancelable(true)

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
                add(RandomEventList("Doctor's Appointment", "#29b6f6"))
                add(RandomEventList("Gym Session", "#ef5350"))
                add(RandomEventList("Networking Event", "#ab47bc"))
                add(RandomEventList("Movie Night", "#ffee58"))
                add(RandomEventList("Dinner Date", "#26a69a"))
                add(RandomEventList("Business Trip", "#8d6e63"))
                add(RandomEventList("Charity Event", "#ff9800"))
                add(RandomEventList("Book Club Meeting", "#b71c1c"))
                add(RandomEventList("Coffee with Friends", "#9ccc65"))
                add(RandomEventList("Music Festival", "#7e57c2"))
                add(RandomEventList("Volunteering", "#78909c"))
                add(RandomEventList("Sports Game", "#f44336"))
                add(RandomEventList("Art Exhibition", "#9c27b0"))
                add(RandomEventList("Language Exchange", "#4caf50"))
                add(RandomEventList("Hiking Trip", "#cddc39"))
                add(RandomEventList("Yoga Class", "#26c6da"))
                add(RandomEventList("Baking Workshop", "#ffab00"))
                add(RandomEventList("Science Fair", "#6a1b9a"))
                add(RandomEventList("Board Game Night", "#607d8b"))
                add(RandomEventList("Fashion Show", "#f57c00"))
                add(RandomEventList("Political Rally", "#009688"))
                add(RandomEventList("Writing Workshop", "#ff4081"))
                add(RandomEventList("Tech Conference", "#1565c0"))
                add(RandomEventList("Wine Tasting", "#8bc34a"))
                add(RandomEventList("Cooking Class", "#f4511e"))
                add(RandomEventList("Open Mic Night", "#673ab7"))
                add(RandomEventList("Karaoke Night", "#ff5252"))
                add(RandomEventList("Outdoor Concert", "#64dd17"))
                add(RandomEventList("Flea Market", "#9e9e9e"))
                add(RandomEventList("Art Museum Tour", "#ff1744"))
                add(RandomEventList("Escape Room", "#00bcd4"))
                add(RandomEventList("Photography Workshop", "#ffd600"))
                add(RandomEventList("Ballet Performance", "#9fa8da"))
                add(RandomEventList("Fashion Design Course", "#4caf4f"))
                add(RandomEventList("Community Service", "#c2185b"))
                add(RandomEventList("Trivia Night", "#2196f3"))
                add(RandomEventList("Chess Tournament", "#afb42b"))
                add(RandomEventList("Stand-up Comedy Show", "#795548"))
                add(RandomEventList("Book Signing", "#e91e63"))
                add(RandomEventList("Potluck Party", "#689f38"))
                add(RandomEventList("Art Auction", "#ba68c8"))
                add(RandomEventList("Game Night", "#00897b"))
                add(RandomEventList("Beer Tasting", "#ffd54f"))
                add(RandomEventList("Stand-up Paddleboarding", "#0277bd"))
                add(RandomEventList("Charity Run", "#f57f17"))
                add(RandomEventList("Poetry Slam", "#f44336"))
                add(RandomEventList("Salsa Dancing", "#4caf50"))
                add(RandomEventList("Board Game Cafe", "#8bc34a"))
                add(RandomEventList("Movie Marathon", "#b71c1c"))
                add(RandomEventList("Bike Tour", "#4db6ac"))
                add(RandomEventList("Outdoor Yoga", "#7cb342"))
                add(RandomEventList("Art Walk", "#00bcd4"))
                add(RandomEventList("Wine and Paint Night", "#9c27b0"))
                add(RandomEventList("Plant Swap", "#388e3c"))
                add(RandomEventList("Beach Clean-up", "#009688"))
                add(RandomEventList("Indoor Skydiving", "#ff6f00"))
                add(RandomEventList("Ice Skating", "#0277bd"))
                add(RandomEventList("Farmers Market", "#cddc39"))
                add(RandomEventList("Game of Thrones Marathon", "#6d4c41"))
                add(RandomEventList("Soap Making Workshop", "#26a69a"))
                add(RandomEventList("Beer and Cheese Pairing", "#ffab00"))
                add(RandomEventList("Group Painting Session", "#ffa000"))
                add(RandomEventList("Food Truck Festival", "#f06292"))
                add(RandomEventList("Ghost Tour", "#7e57c2"))
                add(RandomEventList("Sushi Making Class", "#0091ea"))
                add(RandomEventList("Aquarium Visit", "#b2ff59"))
                add(RandomEventList("Murder Mystery Dinner", "#d500f9"))
                add(RandomEventList("Vintage Clothing Market", "#f57c00"))
                add(RandomEventList("Rock Climbing", "#6a1b9a"))
                add(RandomEventList("DIY Woodworking Class", "#795548"))
                add(RandomEventList("Meditation Retreat", "#0097a7"))
                add(RandomEventList("Group Bike Ride", "#d32f2f"))
                add(RandomEventList("Cooking Competition", "#ff5252"))
                add(RandomEventList("Ice Cream Social", "#00bcd4"))
                add(RandomEventList("Haunted House Visit", "#ba68c8"))
                add(RandomEventList("Photography Walk", "#4caf50"))
                add(RandomEventList("Beach Volleyball", "#ffa000"))
                add(RandomEventList("Gardening Workshop", "#4db6ac"))
                add(RandomEventList("Laser Tag", "#673ab7"))
                add(RandomEventList("Bird Watching Tour", "#ff4081"))
                add(RandomEventList("Movie in the Park", "#43a047"))
                add(RandomEventList("Cider Tasting", "#ef5350"))
                add(RandomEventList("Escape Game", "#29b6f6"))
                add(RandomEventList("Cheese Making Class", "#ffc107"))
                add(RandomEventList("Farm Visit", "#7cb342"))
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
                                String.format("%02d.%02d.%04d", randomDay, month, currentYear)
                            val newEvent = Event(dateStr, randomEvent.name, randomEvent.color)
                            eventsForMonth.add(newEvent)
                        }

                        eventList.addAll(eventsForMonth.shuffled())
                    }

                    withContext(Dispatchers.Main) {
                        callback.invoke(eventList)
                    }
                }
            }
        }
    }

}