package com.nmd.eventCalendar

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.nmd.eventCalendarSample.R
import com.nmd.eventCalendarSample.databinding.ActivityLoginBinding


class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding
    var passwordVisible = false
    private var emailPattern = Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z.]+")
    private var progressDialog: ProgressDialog? = null
    var mAuth: FirebaseAuth? = null
    @RequiresApi(Build.VERSION_CODES.O)
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth!!.currentUser
        if (currentUser != null && checkIfEmailVerified(currentUser)) {
            currentUser.reload()
            openHomeActivity()
        } else {
//            Toast.makeText(this, "Please verify your email.", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        emailAddress = binding.emailAddress
        password = binding.password
        loginButton = binding.loginButton
        forgotPassword = binding.forgotPassword
        signUp = binding.signUp
        progressDialog = ProgressDialog(this)
        mAuth = FirebaseAuth.getInstance()
        loginButton!!.setOnClickListener {
            performAuth()
            val vibrator =
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(15)
        }
        signUp!!.setOnClickListener(View.OnClickListener { openSignUpActivity() })
        password!!.setOnTouchListener(
            OnTouchListener { _: View?, motionEvent: MotionEvent ->
                val Right = 2
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    if (motionEvent.rawX >= password!!.right - password!!.compoundDrawables[Right].bounds.width()
                    ) {
                        val selection =
                            password!!.selectionEnd
                        passwordVisible = if (passwordVisible) {
                            password!!.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.ic_baseline_lock_24,
                                0,
                                R.drawable.ic_baseline_visibility_off_24,
                                0
                            )
                            password!!.transformationMethod = PasswordTransformationMethod.getInstance()
                            false
                        } else {
                            password!!.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.ic_baseline_lock_24,
                                0,
                                R.drawable.ic_baseline_visibility_24,
                                0
                            )
                            password!!.transformationMethod = HideReturnsTransformationMethod.getInstance()
                            true
                        }
                        password!!.setSelection(
                            selection
                        )
                        return@OnTouchListener true
                    }
                }
                false
            })
        forgotPassword!!.setOnClickListener {
            showRecoverPasswordDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun openHomeActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    fun openSignUpActivity() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(15)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun performAuth() {
        val email = emailAddress!!.text.toString()
        val pass = password!!.text.toString()
        if (!email.matches(emailPattern)) {
            Log.d("TAG", "Enter a valid email!")
            emailAddress!!.error = "Enter a valid email!"
        } else if (pass.isEmpty() || pass.length < 8) {
            Log.d("TAG", "Enter a valid email!")
            password!!.error = "Password should contain at least 8 characters!"
        } else {
            progressDialog!!.setMessage("Please wait...")
            progressDialog!!.setTitle("Signing in")
            progressDialog!!.setCanceledOnTouchOutside(false)
            progressDialog!!.show()
            mAuth!!.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task: Task<AuthResult?> ->
                    if (task.isSuccessful) {
                        progressDialog!!.dismiss()

                        val currentUser: FirebaseUser? = mAuth!!.currentUser
                        if (currentUser != null) {
                            val path = currentUser.photoUrl?.path
                            if (path == null){
                                mPrefs?.avatarUrl = ""
                            }else{
                                mPrefs?.avatarUrl = currentUser?.photoUrl.toString()
                            }

                            mPrefs?.username = currentUser.displayName
                            mPrefs?.email = email
                            mPrefs?.password = pass
                        }

                        // Sign in success, update UI with the signed-in user's information
                        Log.d("TAG", "signInWithEmail:success")
                        // updateUI(user);
                        openHomeActivity()
                        Toast.makeText(
                            this@LoginActivity,
                            "Successfully signed in!",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        val vibrator =
                            getSystemService(VIBRATOR_SERVICE) as Vibrator
                        vibrator.vibrate(15)
                    } else {
                        progressDialog!!.dismiss()
                        // If sign in fails, display a message to the user.
                        Log.w("TAG", "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            this@LoginActivity,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        //updateUI(null);
                    }
                }
        }
    }

    var loadingBar: ProgressDialog? = null

    @SuppressLint("SetTextI18n")
    private fun showRecoverPasswordDialog() {
        val layoutInflater = LayoutInflater.from(this@LoginActivity)
        val promptView: View = layoutInflater.inflate(R.layout.reset_dialog_content, null)
        val builder: AlertDialog = AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(promptView)
            .setCancelable(false)
            .setPositiveButton("Send", null)
            .setNegativeButton("Cancel"
            ) { dialog, _ -> dialog.dismiss() }
            .create()

        val resetRequestEmail = promptView.findViewById<View>(R.id.resetRequestEmail) as TextInputLayout
        resetRequestEmail.editText?.textSize = 14F

        builder.show()
        // Click on Recover and a email will be sent to your registered email id
        val positiveButton = builder.getButton(AlertDialog.BUTTON_POSITIVE)
        // setup a dialog window
        positiveButton.setOnClickListener { _ ->
            val email = resetRequestEmail.editText?.text.toString().trim { it <= ' ' }
            if (email != "") {
                beginRecovery(email)
                builder.dismiss()
            }
        }
        builder.show()
    }

    private fun beginRecovery(email: String) {
        loadingBar = ProgressDialog(this)
        loadingBar!!.setMessage("Sending Email....")
        loadingBar!!.setCanceledOnTouchOutside(false)
        loadingBar!!.show()

        mAuth!!.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            loadingBar!!.dismiss()
            if (task.isSuccessful) {
                Toast.makeText(
                    this@LoginActivity,
                    "Reset email was successfully sent",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@LoginActivity,
                    "An error was occurred.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.addOnFailureListener {
            loadingBar!!.dismiss()
            Toast.makeText(
                this@LoginActivity,
                "Error Failed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var emailAddress: EditText? = null

        @SuppressLint("StaticFieldLeak")
        var password: EditText? = null

        @SuppressLint("StaticFieldLeak")
        var loginButton: Button? = null

        @SuppressLint("StaticFieldLeak")
        var forgotPassword: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var option1: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var signUp: TextView? = null
    }
}