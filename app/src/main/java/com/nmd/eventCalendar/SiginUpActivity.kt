package com.nmd.eventCalendar

//import android.app.ProgressDialog
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.provider.MediaStore
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
//import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.nmd.eventCalendar.model.User
import com.nmd.eventCalendarSample.R
import com.nmd.eventCalendarSample.databinding.ActivitySignupBinding
import java.util.UUID

class SignUpActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var name: EditText
        @SuppressLint("StaticFieldLeak")
        lateinit var emailAddress1: EditText
        @SuppressLint("StaticFieldLeak")
        lateinit var password1: EditText
        @SuppressLint("StaticFieldLeak")
        lateinit var signupButton: Button
        @SuppressLint("StaticFieldLeak")
        lateinit var login: TextView
    }

    private lateinit var binding: ActivitySignupBinding
    private lateinit var progressDialog: ProgressDialog
    private var passwordVisible: Boolean = false
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z.]+"
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStorage: FirebaseStorage
    private val PICK_IMAGE_REQUEST = 1
    private var avatarImageUri: Uri? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        password1 = binding.password1
        name = binding.name
        emailAddress1 = binding.emailAddress1
        signupButton = binding.signupButton
        name = binding.name
        progressDialog = ProgressDialog(this)
        mAuth = FirebaseAuth.getInstance()
        mStorage = FirebaseStorage.getInstance()
        login = findViewById(R.id.login)
        val avatarImageView = findViewById<ImageView>(R.id.avatarImageView)

        avatarImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        password1.setOnTouchListener { view, motionEvent ->
            val Right = 2
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                if (motionEvent.rawX >= password1.right - password1.compoundDrawables[Right].bounds.width()) {
                    val selection = password1.selectionEnd
                    if (passwordVisible) {
                        password1.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_lock_24, 0, R.drawable.ic_baseline_visibility_off_24, 0)
                        password1.transformationMethod = PasswordTransformationMethod.getInstance()
                        passwordVisible = false
                    } else {
                        password1.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_lock_24, 0, R.drawable.ic_baseline_visibility_24, 0)
                        password1.transformationMethod = HideReturnsTransformationMethod.getInstance()
                        passwordVisible = true
                    }
                    password1.setSelection(selection)
                    return@setOnTouchListener true
                }
            }
            false
        }

        signupButton.setOnClickListener { performAuth() }
        login.setOnClickListener { openLoginActivity() }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val alertDialogBuilder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(this@SignUpActivity)
            alertDialogBuilder.setTitle("Finish")
            alertDialogBuilder.setMessage("Are you sure you want to exit this app?")
            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Yes",
                    DialogInterface.OnClickListener { dialog, id ->
                        run {
                            finish()
                            Toast.makeText(
                                this@SignUpActivity,
                                "The app is closed.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                .setNegativeButton("No",
                    DialogInterface.OnClickListener { dialog, id ->  })

            // create an alert dialog
            alertDialogBuilder.setCancelable(true)
            alertDialogBuilder.show()
        }
    }

    private fun performAuth() {
        var displayName = name.text.toString()
        val email = emailAddress1.text.toString()
        val password = password1.text.toString()
        val avatarImage = avatarImageUri.toString()

        if (!email.matches(emailPattern.toRegex())) {
            Log.d("TAG", "Enter a valid email!")
            emailAddress1.error = "Enter a valid email!"
        } else if (avatarImage.isNullOrEmpty()) {
            Log.d("TAG", "Select a user avatar!")
//            avatarImageUri.error = "Password should contain at least 8 characters!"
        } else if (password.isEmpty() || password.length < 8) {
            Log.d("TAG", "Enter a valid email!")
            password1.error = "Password should contain at least 8 characters!"
        } else {
            progressDialog.setMessage("Please wait...")
            progressDialog.setTitle("Registration")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
//                    val database = FirebaseDatabase.getInstance()
//                    val usersRef = database.getReference("users")
                    val db = FirebaseFirestore.getInstance()
                    val usersRef = db.collection("users")
                    var fcmToken: String? = null

                    // Retrieve the FCM token
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Get new FCM registration token
                            val token = task.result
                            fcmToken = token
                            Log.d("TAG", "FCM registration token: $fcmToken")

                            val newUser = hashMapOf(
                                "displayName" to displayName,
                                "email" to email,
                                "token" to token
                            )
                            usersRef.add(newUser).addOnSuccessListener {
                                Log.d("add-success", "Success")
                            }.addOnFailureListener {
                                Log.d("add-failure", "Failed")
                            }
//                            newUserRef.child("email").setValue(email)
//                            newUserRef.child("displayName").setValue(displayName)
//                            newUserRef.child("token").setValue(fcmToken)

                            Log.d("TAG" ,"$fcmToken")

                            val user = mAuth.currentUser

                            user?.updateProfile(userProfileChangeRequest {
                                setDisplayName(displayName)
                            })
                            uploadAvatarImage(user)

                            progressDialog.dismiss()
                            Log.d("TAG", "createUserWithEmail:success")
                            Toast.makeText(this@SignUpActivity, "Successfully registered!", Toast.LENGTH_SHORT).show()
                            if (user != null) {
                                verifyEmail(user)
                            }
                            openLoginActivity()
                        } else {
                            Log.d("TAG", "Fetching FCM registration token failed", task.exception)
                        }
                    }
                } else {
                    progressDialog.dismiss()
                    Log.w("TAG", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this@SignUpActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadAvatarImage(user: FirebaseUser?) {
        if (avatarImageUri != null && user != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("avatars/${UUID.randomUUID()}")
            storageRef.putFile(avatarImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val profileUpdates = userProfileChangeRequest {
                            photoUri = uri
                        }
                        user.updateProfile(profileUpdates)
                    }
                }
                .addOnFailureListener {

                }
        }
    }

    fun openLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(15)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val avatarImageView = findViewById<ImageView>(R.id.avatarImageView)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            avatarImageUri = data.data

            Log.d("dd", avatarImageUri.toString());
            // Set the selected image as preview
            avatarImageView.setImageURI(avatarImageUri)
        }
    }
}