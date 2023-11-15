package com.nmd.eventCalendar

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.nmd.eventCalendarSample.databinding.ActivityInvitationBinding


open class InvitationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInvitationBinding

    @SuppressLint("StaticFieldLeak")
    var logoutButton: ImageView? = null
    var fullName: TextView? = null
    var firebaseAuth: FirebaseAuth? = null
    private val authStateListener: AuthStateListener? = null

    var dataModels: ArrayList<DataModel>? = null
    private var adapter: CustomAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvitationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initialize()
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    private fun initialize() {
        with(binding) {
//            dataModels = ArrayList()
//
//            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "invited"))
//            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "pending"))
//            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "invited"))
//            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "invited"))
//            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "pending"))
//            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "invited"))
//            dataModels!!.add(DataModel("John Doe", "johndoe@example.com", "Oct 23, 2023", "invited"))
//
//            adapter = CustomAdapter(dataModels!!, applicationContext)
//
//            list.setAdapter(adapter)
//            list.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
//                val dataModel: DataModel = dataModels!![position]
//                showAcceptConfirmDialog(dataModel)
//            })
//            eventCalendarLogoutImageView.setOnClickListener(View.OnClickListener {
//                showLogoutConfirmDialog()
//            })
//            eventCalendarGotoCalendarImageView.setOnClickListener {
//                val intent = Intent(this@InvitationActivity, MainActivity::class.java)
//                startActivity(intent)
//            }
        }
    }

    private fun showAcceptConfirmDialog(dataModel: DataModel) {
        val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@InvitationActivity)
        alertDialogBuilder.setTitle("Invitation")
        alertDialogBuilder.setMessage("What do you want to do with ${dataModel.name}?")
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("Accept",
                DialogInterface.OnClickListener { dialog, id -> Toast.makeText(this@InvitationActivity, "You have invited ${dataModel.name}.", Toast.LENGTH_SHORT).show() })
            .setNegativeButton("Decline",
                DialogInterface.OnClickListener { dialog, id -> Toast.makeText(this@InvitationActivity, "You have declined ${dataModel.name}.", Toast.LENGTH_SHORT).show() })
            .setNeutralButton("Keep prev",
                DialogInterface.OnClickListener { dialog, id ->  })

        // create an alert dialog
        alertDialogBuilder.setCancelable(true)
        alertDialogBuilder.show()
    }

    protected fun showLogoutConfirmDialog() {
        val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@InvitationActivity)
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
                            this@InvitationActivity,
                            "Successfully logged out!",
                            Toast.LENGTH_SHORT
                        ).show()
                        Toast.makeText(
                            this@InvitationActivity,
                            "You have been logged out.",
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

    private fun openLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(15)
    }

}