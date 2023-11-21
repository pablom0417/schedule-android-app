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
import android.os.StrictMode
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import com.alamkanak.weekview.WeekViewEntity
import com.alamkanak.weekview.jsr310.WeekViewPagingAdapterJsr310
import com.alamkanak.weekview.jsr310.setDateFormatter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.nmd.eventCalendar.data.model.CalendarEntity
import com.nmd.eventCalendar.data.model.toWeekViewEntity
import com.nmd.eventCalendar.`interface`.EventCalendarDayClickListener
import com.nmd.eventCalendar.`interface`.EventCalendarScrollListener
import com.nmd.eventCalendar.model.Day
import com.nmd.eventCalendar.model.Event
import com.nmd.eventCalendar.model.Memo
import com.nmd.eventCalendar.model.User
import com.nmd.eventCalendar.util.GenericAction
import com.nmd.eventCalendar.util.genericViewModel
import com.nmd.eventCalendar.util.subscribeToEvents
import com.nmd.eventCalendar.util.yearMonthsBetween
import com.nmd.eventCalendar.utils.Utils.Companion.isDarkColor
import com.nmd.eventCalendarSample.R
import com.nmd.eventCalendarSample.databinding.ActivityMainBinding
import com.nmd.eventCalendarSample.databinding.BottomSheetBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
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
    private var isWeekView = false
    private var color = arrayOf(
        "#e18900", "#f44336", "#4badeb", "#AB274F", "#CA1F7B", "#0BDA51"
    )

    var user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    private val db = Firebase.firestore
    val userConnectionRef = db.collection("users")
    val memoCollectionRef = db.collection("memos")
    val scheduleCollectionRef = db.collection("schedules")

    var memo: Event? = null
    var email: String? = user?.email
    var displayName: String? = user?.displayName
    var schedules: ArrayList<Event> = ArrayList()

    private val viewModel by genericViewModel()

    var dataModels: ArrayList<User>? = null
    private var adapter: CustomAdapter? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private val weekdayFormatter = DateTimeFormatter.ofPattern("E", Locale.getDefault())
    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        if (user == null || !checkIfEmailVerified(user)) {
            Log.d("logged in user", checkIfEmailVerified(user).toString())
            FirebaseAuth.getInstance().signOut()
            openLoginActivity()
            Toast.makeText(this, "User doesn't exists or email wasn't verified.", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
//        AndroidThreeTen.init(this)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        initialize()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun initialize() {
        with(binding) {
            val weekViewAdapter: BasicActivityWeekViewAdapter by lazy {
                BasicActivityWeekViewAdapter(
                    eventClickHandler = this@MainActivity::showInputDialog,
                    loadMoreHandler = viewModel::fetchEvents,
                )
            }
            progressBar.visibility = View.VISIBLE
            eventCalendarView.visibility = View.GONE
            weekCalendarView.visibility = View.GONE
            switchLayout.visibility = View.GONE
            backButton.visibility = View.INVISIBLE
            floatingActionButton.visibility = View.GONE
            viewSwitch.isChecked = false
            calendarStatus.text = "My Calendar"
            drawerLayout.closeDrawer(navView)
            if (email != user?.email) {
                calendarStatus.text = "${displayName}'s Calendar"
                backButton.visibility = View.VISIBLE
                floatingActionButton.visibility = View.GONE
            }

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
            val useremail = navHeader.findViewById(R.id.textViewUserEmail) as TextView
            val imageviewProfileImage = navHeader.findViewById(R.id.imageviewProfileImage) as ImageView

            val avatarImageUri = user?.photoUrl.toString()
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


            if (user != null) {
                username.text = user?.displayName
                useremail.text = user?.email
            }

            val year = Calendar.getInstance().get(Calendar.YEAR)

            getSchedules(email!!) {
                eventCalendarView.events = it
                Log.d("event-list", eventCalendarView.events.toString())
                eventCalendarView.post {
                    progressBar.visibility = View.GONE
                    switchLayout.visibility = View.VISIBLE
                    if (isWeekView) {
                        weekCalendarView.visibility = View.VISIBLE
                        floatingActionButton.visibility = View.GONE
                    } else {
                        if (email == user?.email) floatingActionButton.visibility = View.VISIBLE
                        eventCalendarView.visibility = View.VISIBLE
                    }
                }
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

                    else -> {}
                }
            }

            eventCalendarViewCalendarImageView.setOnClickListener {
                eventCalendarView.scrollToCurrentMonth(false)
            }

            backButton.setOnClickListener {
                email = user?.email
                displayName = user?.displayName
                initialize()
            }

            viewSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    eventCalendarView.visibility = View.GONE
                    weekCalendarView.visibility = View.VISIBLE
                    floatingActionButton.visibility = View.GONE
                    eventCalendarViewCalendarImageView.visibility = View.GONE
                    viewSwitch.text = "Weekly View"
                } else {
                    eventCalendarView.visibility = View.VISIBLE
                    weekCalendarView.visibility = View.GONE
                    if (email == user?.email) floatingActionButton.visibility = View.VISIBLE
                    eventCalendarViewCalendarImageView.visibility = View.VISIBLE
                    viewSwitch.text = "Monthly View"
                }
                isWeekView = isChecked
            }

            floatingActionButton.setOnClickListener {
                showInputDialog(Event(user?.email))
            }

            eventCalendarView.addOnDayClickListener(object : EventCalendarDayClickListener {
                @SuppressLint("SimpleDateFormat")
                override fun onClick(day: Day) {
                    val eventList = eventCalendarView.events.filter { it.startDate != null && SimpleDateFormat("MM/dd/yyyy").parse(it.startDate)!! <= SimpleDateFormat("MM/dd/yyyy").parse(day.date) &&
                            SimpleDateFormat("MM/dd/yyyy").parse(it.endDate)!! >= SimpleDateFormat("MM/dd/yyyy").parse(day.date) }
                    bottomSheet(day, eventList)
                }
            })

            eventCalendarView.addOnCalendarScrollListener(object : EventCalendarScrollListener {
                override fun onScrolled(month: Int, year: Int) {
                    Log.i("ECV", "Scrolled to: $month $year")
                }
            })

            sideLogoutButton.setOnClickListener {
                showLogoutConfirmDialog()
            }

            eventCalendarLogoutImageView.setOnClickListener(View.OnClickListener {
                showLogoutConfirmDialog()
            })

            dataModels = arrayListOf()
            userConnectionRef.get().addOnSuccessListener {
                for (document in it) {
                    val item = document.toObject<User>()
                    if (item.email != user?.email) dataModels!!.add(item)
                }
                adapter = CustomAdapter(dataModels!!, applicationContext)
                list.adapter = adapter
            }
            list.setOnItemClickListener { parent, view, position, id ->
                val dataModel: User = dataModels!![position]
                showAcceptConfirmDialog(dataModel)
            }

            sideInvitationListButton.setOnClickListener {
                drawerLayout.closeDrawer(navView)
            }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
            alertDialogBuilder.setTitle("Finish")
            alertDialogBuilder.setMessage("Are you sure you want to exit this app?")
            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    run {
                        finish()
                        Toast.makeText(
                            this@MainActivity,
                            "The app is closed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("No") { dialog, id -> }

            // create an alert dialog
            alertDialogBuilder.setCancelable(true)
            alertDialogBuilder.show()
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
            alertDialogBuilder.setTitle("Edit Schedule")
        } else {
            alertDialogBuilder.setTitle("Add Schedule")
        }
        alertDialogBuilder.setView(promptView)
        // create an alert dialog
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.show()
        val positiveButton = alertDialogBuilder.getButton(AlertDialog.BUTTON_POSITIVE)
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
        inputStartDate.editText?.setOnClickListener {
            pickDate(inputStartDate.editText!!)
        }
        inputStartDate.editText?.addTextChangedListener {
            if (inputStartDate.editText?.text.isNullOrEmpty()) {
                inputStartDate.error = "Fill start date."
            } else {
                inputStartDate.error = null
            }
        }
        inputEndDate.editText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pickDate(inputEndDate.editText!!)
            }
        }
        inputEndDate.editText?.setOnClickListener {
            pickDate(inputEndDate.editText!!)
        }
        inputEndDate.editText?.addTextChangedListener {
            if (inputEndDate.editText?.text.isNullOrEmpty()) {
                inputEndDate.error = "Fill end date."
            } else {
                inputEndDate.error = null
            }
        }
        inputStartTime.editText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pickTime(inputStartTime.editText!!)
            }
        }
        inputStartTime.editText?.setOnClickListener {
            pickTime(inputStartTime.editText!!)
        }
        inputStartTime.editText?.addTextChangedListener {
            if (inputStartTime.editText?.text.isNullOrEmpty()) {
                inputStartTime.error = "Fill start time."
            } else {
                inputStartTime.error = null
            }
        }
        inputEndTime.editText?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                pickTime(inputEndTime.editText!!)
            }
        }
        inputEndTime.editText?.setOnClickListener {
            pickTime(inputEndTime.editText!!)
        }
        inputEndTime.editText?.addTextChangedListener {
            if (inputEndTime.editText?.text.isNullOrEmpty()) {
                inputEndTime.error = "Fill end time."
            } else {
                inputEndTime.error = null
            }
        }
        // setup a dialog window
        positiveButton.setOnClickListener { _ ->
            positiveButton.isEnabled = false
            if (inputStartDate.editText?.text.isNullOrEmpty() ||
                inputEndDate.editText?.text.isNullOrEmpty() ||
                inputStartTime.editText?.text.isNullOrEmpty() ||
                inputEndTime.editText?.text.isNullOrEmpty()
            ) {
                if (inputStartDate.editText?.text.isNullOrEmpty()) inputStartDate.error = "Fill start date."
                if (inputEndDate.editText?.text.isNullOrEmpty()) inputEndDate.error = "Fill end date."
                if (inputStartTime.editText?.text.isNullOrEmpty()) inputStartTime.error = "Fill start time."
                if (inputEndTime.editText?.text.isNullOrEmpty()) inputEndTime.error = "Fill end time."
                Toast.makeText(
                    this@MainActivity,
                    "The date and time fields are required.",
                    Toast.LENGTH_SHORT
                ).show()
                positiveButton.isEnabled = true
            } else if (inputStartDate.editText?.text.toString() > inputEndDate.editText?.text.toString() || inputStartTime.editText?.text.toString() >= inputEndTime.editText?.text.toString()) {
                if (inputStartDate.editText?.text.toString() > inputEndDate.editText?.text.toString()) inputEndDate.error = "Invalid end date"
                if (inputStartTime.editText?.text.toString() >= inputEndTime.editText?.text.toString()) inputEndTime.error = "Invalid end time"
                Toast.makeText(
                    this@MainActivity,
                    "End date and time must be bigger than start.",
                    Toast.LENGTH_SHORT
                ).show()
                positiveButton.isEnabled = true
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
                    positiveButton.isEnabled = true
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.eventCalendarView.visibility = View.GONE
                    binding.weekCalendarView.visibility = View.GONE
                    binding.eventCalendarView.events = ArrayList()
                    if (event?.id == null) {
                        val item = hashMapOf(
                            "email" to memo!!.email,
                            "startDate" to memo!!.startDate,
                            "endDate" to memo!!.endDate,
                            "startTime" to memo!!.startTime,
                            "endDate" to memo!!.endTime,
                            "name" to memo!!.name,
                            "backgroundHexColor" to memo!!.backgroundHexColor,
                        )
                        scheduleCollectionRef.add(memo!!).addOnSuccessListener {
                            initialize()
                            Toast.makeText(
                                this@MainActivity,
                                "The schedule is successfully created.",
                                Toast.LENGTH_SHORT
                            ).show()
                            alertDialogBuilder.dismiss()
                            positiveButton.isEnabled = true
                            // Get bearer token
                            val serviceAccountKeyPath = "firebase.json"
                            // Load the service account key file
                            val credentials = GoogleCredentials.fromStream(assets.open(serviceAccountKeyPath))
                                .createScoped("https://www.googleapis.com/auth/cloud-platform")

                            val gfgPolicy =
                                StrictMode.ThreadPolicy.Builder().permitAll().build()
                            StrictMode.setThreadPolicy(gfgPolicy)
                            // Obtain an access token
                            val accessToken = credentials.refreshAccessToken().tokenValue

                            // Use the access token in your FCM API requests
                            val authorizationHeader = "Bearer $accessToken"

                            fun sendMessageToUser(token: String){
                                val username = user!!.displayName
                                Log.d("token", token)
                                val messageBody = """
                                        {
                                            "message": {
                                                "token": "$token",
                                                "notification": {
                                                    "body": "${username} has created a new schedule.",
                                                    "title": "A new schedule"
                                                }
                                            }
                                        }
                                    """.trimIndent()

                                // Set the request headers
                                val url = URL("https://fcm.googleapis.com/v1/projects/eventcalendar-566a9/messages:send")
                                val connection = url.openConnection() as HttpURLConnection
                                connection.requestMethod = "POST"
                                connection.setRequestProperty("Content-Type", "application/json")
                                connection.setRequestProperty("Authorization", authorizationHeader)

                                // Send the message body as the request payload
                                connection.doOutput = true
                                val payload = messageBody.toByteArray(StandardCharsets.UTF_8)
                                connection.setRequestProperty("Content-Length", payload.size.toString())
                                connection.outputStream.write(payload)

                                // Read the response from the server
                                val responseCode = connection.responseCode
                                val responseMessage = connection.responseMessage
                                val inputStream = if (responseCode < 400) connection.inputStream else connection.errorStream
                                val response = inputStream.bufferedReader().use { it.readText() }
                                inputStream.close()

                                // Handle the response
                                if (responseCode < 400) {
                                    Log.d("message-result", "Message sent successfully: $response")
                                    println("Message sent successfully: $response")
                                } else {
                                    Log.d("message-result", "Failed to send message: $responseMessage - $response")
                                    println("Failed to send message: $responseMessage - $response")
                                }
                            }
                            userConnectionRef.get().addOnSuccessListener {
                                for (document in it) {
                                    val userItem = document.toObject<User>()
                                    if (userItem.email != user?.email) sendMessageToUser(userItem.token!!)
                                }
                            }
                        }.addOnFailureListener {
                            initialize()
                            Toast.makeText(
                                this@MainActivity,
                                "Creating the schedule failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                            alertDialogBuilder.dismiss()
                            positiveButton.isEnabled = true
                        }
                    } else {
                        scheduleCollectionRef.document(event.id!!).update(
                            mapOf(
                                "startDate" to memo!!.startDate,
                                "endDate" to memo!!.endDate,
                                "startTime" to memo!!.startTime,
                                "endTime" to memo!!.endTime,
                                "name" to memo!!.name,
                            )
                        ).addOnSuccessListener {
                            binding.progressBar.visibility = View.GONE
                            binding.eventCalendarView.visibility = View.VISIBLE
                            initialize()
                            Toast.makeText(
                                this@MainActivity,
                                "The schedule is successfully updated.",
                                Toast.LENGTH_SHORT
                            ).show()
                            alertDialogBuilder.dismiss()
                            positiveButton.isEnabled = true
                        }.addOnFailureListener {
                            binding.progressBar.visibility = View.GONE
                            binding.eventCalendarView.visibility = View.VISIBLE
                            initialize()
                            Toast.makeText(
                                this@MainActivity,
                                "Updating the schedule failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                            alertDialogBuilder.dismiss()
                            positiveButton.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    protected fun showInputMemoDialog(day: Day, item: Event) {
        val alertDialogBuilder = AlertDialog.Builder(binding.root.context)
            .setPositiveButton("Save", null)
            .setNegativeButton("Delete", null)
            .setNeutralButton("Close"
            ) { _, _ -> }
            .create()
        if (!item.memos.isNullOrEmpty() && item.memos!!.any { it.date == day.date }) {
            alertDialogBuilder.setTitle("Edit Memo")
        } else {
            alertDialogBuilder.setTitle("Add Memo")
        }
        val promptView: View = LayoutInflater.from(binding.root.context).inflate(com.nmd.eventCalendar.R.layout.add_memo_content, null)
        alertDialogBuilder.setView(promptView)
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.show()
        val positiveButton = alertDialogBuilder.getButton(AlertDialog.BUTTON_POSITIVE)
        val negativeButton = alertDialogBuilder.getButton(AlertDialog.BUTTON_NEGATIVE)
        positiveButton.isEnabled = false
        val inputMemo = promptView.findViewById<View>(com.nmd.eventCalendar.R.id.input_memo) as TextInputLayout
        var originalMap: HashMap<String, String> = hashMapOf()
        if (!item.memos.isNullOrEmpty() && item.memos!!.any { it.date == day.date }) {
            originalMap = hashMapOf(
                "date" to item.memos!!.filter { it.date == day.date }[0].date!!,
                "memo" to item.memos!!.filter { it.date == day.date }[0].memo!!
            )
            inputMemo.editText?.setText(
                item.memos!!.filter { it.date == day.date }[0].memo
            )
        }
        inputMemo.editText?.addTextChangedListener {
            Log.d("key-history", inputMemo.editText?.text.toString())
            if (inputMemo.editText?.text.isNullOrEmpty()) {
                inputMemo.error = "The memo fields are required."
                positiveButton.isEnabled = false
            } else {
                inputMemo.error = null
                positiveButton.isEnabled = true
            }
        }
        if (item.memos.isNullOrEmpty() || !item.memos!!.any { it.date == day.date }) {
            negativeButton.visibility = View.GONE
        }

        positiveButton.setOnClickListener { _ ->
            positiveButton.isEnabled = false
            if (inputMemo.editText?.text.isNullOrEmpty()) {
                Toast.makeText(
                    binding.root.context,
                    "The memo fields are required.",
                    Toast.LENGTH_SHORT
                ).show()
                inputMemo.error = "The memo fields are required."
                positiveButton.isEnabled = true
            } else {
                val map = hashMapOf(
                    "date" to day.date,
                    "memo" to inputMemo.editText?.text.toString()
                )
                if (originalMap.isNotEmpty()) {
                    scheduleCollectionRef.document(item.id!!).update("memos", FieldValue.arrayRemove(originalMap))
                        .addOnSuccessListener {
                            scheduleCollectionRef.document(item.id!!).update("memos", FieldValue.arrayUnion(map)).addOnSuccessListener {
                                initialize()
                                Toast.makeText(binding.root.context, "New memo was updated.", Toast.LENGTH_SHORT).show()
                                alertDialogBuilder.dismiss()
                                positiveButton.isEnabled = true
                            }.addOnFailureListener {
                                Toast.makeText(binding.root.context, "Updating memo failed", Toast.LENGTH_SHORT).show()
                                alertDialogBuilder.dismiss()
                                positiveButton.isEnabled = true
                            }
                        }.addOnFailureListener {
                            Toast.makeText(binding.root.context, "Updating memo failed", Toast.LENGTH_SHORT).show()
                            alertDialogBuilder.dismiss()
                            positiveButton.isEnabled = true
                        }
                } else {
                    scheduleCollectionRef.document(item.id!!).update("memos", FieldValue.arrayUnion(map)).addOnSuccessListener {
                        initialize()
                        Toast.makeText(binding.root.context, "New memo was added.", Toast.LENGTH_SHORT).show()
                        alertDialogBuilder.dismiss()
                        positiveButton.isEnabled = true
                    }.addOnFailureListener {
                        Toast.makeText(binding.root.context, "Adding memo failed", Toast.LENGTH_SHORT).show()
                        alertDialogBuilder.dismiss()
                        positiveButton.isEnabled = true
                    }
                }
            }
        }
        negativeButton.setOnClickListener {
            negativeButton.isEnabled = false
            scheduleCollectionRef.document(item.id!!).update("memos", FieldValue.arrayRemove(originalMap)).addOnSuccessListener {
                initialize()
                Toast.makeText(binding.root.context, "Memo was deleted.", Toast.LENGTH_SHORT).show()
                alertDialogBuilder.dismiss()
                negativeButton.isEnabled = true
            }.addOnFailureListener {
                Toast.makeText(binding.root.context, "Deleting memo failed", Toast.LENGTH_SHORT).show()
                alertDialogBuilder.dismiss()
                negativeButton.isEnabled = true
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showAcceptConfirmDialog(dataModel: User) {
        val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
        alertDialogBuilder.setTitle("View others' calendar")
        alertDialogBuilder.setMessage("Do you really want to see ${dataModel.displayName}'s calendar?")
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("Yes"
            ) { _, _ ->
                email = dataModel.email
                displayName = dataModel.displayName
                initialize()
                Toast.makeText(
                    this@MainActivity,
                    "You are seeing ${dataModel.displayName}'s calendar now.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("No"
            ) { _, _ -> }

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
                        finish()
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
        alertDialogBuilder.setTitle("Delete Schedule")
        alertDialogBuilder.setMessage("Are you sure you want to delete this schedule?")
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("Delete"
            ) { _, _ ->
                scheduleCollectionRef.document(id).delete().addOnSuccessListener {
                    initialize()
                    Toast.makeText(
                        this@MainActivity,
                        "The schedule is deleted.",
                        Toast.LENGTH_SHORT
                    ).show()
                }.addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Deleting schedule failed.",
                        Toast.LENGTH_SHORT
                    ).show()
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
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private fun bottomSheet(day: Day, eventList: List<Event>) {
        var events = ArrayList(eventList).sortedBy { it.startTime }
        val binding = BottomSheetBinding.inflate(LayoutInflater.from(this))
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val size = eventList.size
        var sheetHeaderText = ""
        sheetHeaderText = if (size == 0) {
            day.date + " (No schedules)"
        } else {
            if (size == 1) {
                day.date + " (" + eventList.size + " schedule)"
            } else {
                day.date + " (" + eventList.size + " schedules)"
            }
        }

        if (email != user?.email) binding.addNewMemoButton.visibility = View.GONE
        binding.bottomSheetMaterialTextView.text = sheetHeaderText
        binding.bottomSheetNoEventsMaterialTextView.visibility =
            if (eventList.isEmpty()) View.VISIBLE else View.GONE

        binding.bottomSheetNoEventsMaterialTextView.text = "No schedules were found for the selected day."
        val sheetEventsAdapter = SheetEventsAdapter(day, ArrayList(events))
        binding.bottomSheetRecyclerView.adapter = sheetEventsAdapter
        sheetEventsAdapter.setOnClickListener(object :
            SheetEventsAdapter.OnClickListener {
            override fun onClick(position: Int, model: Event) {
                if (email == user?.email) {
                    showInputDialog(model)
                    bottomSheetDialog.cancel()
                }
            }
        })

        sheetEventsAdapter.setOnLongClickListener(object : SheetEventsAdapter.OnLongClickListener {
            override fun onLongClick(position: Int, model: Event) {
                if (model.id != null && email == user?.email) {
                    showDeleteConfirmButton(model.id!!)
                    bottomSheetDialog.cancel()
                }
            }
        })

        sheetEventsAdapter.setOnButtonClickListener(object : SheetEventsAdapter.OnButtonClickListener {
            override fun onButtonClickListener(position: Int, model: Event) {
                if (model.email != user?.email) showOthersMemo(day, model)
                else showInputMemoDialog(day, model)
                bottomSheetDialog.cancel()
            }

        })

        bottomSheetDialog.setContentView(binding.root)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true
        bottomSheetDialog.setCancelable(true)
        binding.addNewMemoButton.text = "Add Schedule"
        binding.addNewMemoButton.setOnClickListener {
            showInputDialog(Event(user?.email, day.date, day.date))
            bottomSheetDialog.cancel()
        }

        bottomSheetDialog.show()
    }

    fun showOthersMemo(day: Day, event: Event) {
        val alertDialogBuilder = AlertDialog.Builder(this@MainActivity)
            .setPositiveButton("Close"
            ) { _, _ ->  }
            .create()
        alertDialogBuilder.setTitle("View Memo")

        val promptView: View = LayoutInflater.from(binding.root.context).inflate(R.layout.weekly_memo_view, null)
        alertDialogBuilder.setView(promptView)
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.show()
        val memoTextView = promptView.findViewById<MaterialTextView>(R.id.weeklyMemoTextView)
        memoTextView.text = event.memos?.filter { it.date == day.date }!![0].memo
        memoTextView.setBackgroundColor(Color.parseColor(event.backgroundHexColor))
        memoTextView.setTextColor(if (Color.parseColor(event.backgroundHexColor).isDarkColor()) Color.WHITE else Color.BLACK)
    }

    private fun getMemos(email: String, callback: (ArrayList<Event>) -> Unit) {
        val eventList = arrayListOf<Event>()
        memoCollectionRef.get().addOnSuccessListener {
            for (document in it) {
                val event = Event(document["email"].toString(), document["startDate"].toString(), document["endDate"].toString(), document["startTime"].toString(), document["endTime"].toString(), document["name"].toString(), document["backgroundHexColor"].toString(), document["id"].toString())
                if (event.email == email) eventList.add(event)
            }
            callback(eventList)
        }.addOnFailureListener {
            callback(eventList)
        }
    }

    private fun getSchedules(email: String, callback: (ArrayList<Event>) -> Unit) {
        val eventList = arrayListOf<Event>()
        scheduleCollectionRef.get().addOnSuccessListener {
            for (document in it) {
//                scheduleCollectionRef.document(document.id).collection("memos").get().addOnSuccessListener {  }
                val event = document.toObject<Event>()
                event.id = document.id
                if (event.email == email) eventList.add(event)
            }
            callback(eventList)
        }.addOnFailureListener {
            callback(eventList)
        }
    }

    inner class BasicActivityWeekViewAdapter(
        private val eventClickHandler: (Event?) -> Unit,
        private val loadMoreHandler: (String, List<YearMonth>) -> Unit
    ) : WeekViewPagingAdapterJsr310<CalendarEntity>() {
        override fun onCreateEntity(item: CalendarEntity): WeekViewEntity = item.toWeekViewEntity()

        override fun onEventClick(data: CalendarEntity, bounds: RectF) {
            if (data is CalendarEntity.Event) {
                if (data.location.isNotEmpty()) {
                    val alertDialogBuilder = AlertDialog.Builder(this@MainActivity)
                        .setPositiveButton("Close"
                        ) { _, _ ->  }
                        .create()
                    alertDialogBuilder.setTitle("Memo of ${
                        String.format("%02d/%02d/%04d %02d:%02d", 
                            data.startTime.monthValue, data.startTime.dayOfMonth, data.startTime.year, data.startTime.hour, data.startTime.minute)
                    }")

                    val promptView: View = LayoutInflater.from(binding.root.context).inflate(R.layout.weekly_memo_view, null)
                    alertDialogBuilder.setView(promptView)
                    alertDialogBuilder.setCancelable(true)
                    alertDialogBuilder.show()
                    val memoTextView = promptView.findViewById<MaterialTextView>(R.id.weeklyMemoTextView)
                    memoTextView.text = data.location
                    memoTextView.setBackgroundColor(data.color)
                    memoTextView.setTextColor(if (data.color.isDarkColor()) Color.WHITE else Color.BLACK)
                }
            }
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
            loadMoreHandler(email!!, yearMonthsBetween(startDate, endDate))
        }

        override fun onVerticalScrollPositionChanged(currentOffset: Float, distance: Float) {
            Log.d("BasicActivity", "Scrolling vertically (distance: ${distance.toInt()}, current offset ${currentOffset.toInt()})")
        }

        override fun onVerticalScrollFinished(currentOffset: Float) {
            Log.d("BasicActivity", "Vertical scroll finished (current offset ${currentOffset.toInt()})")
        }
    }
}