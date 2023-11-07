package com.nmd.eventCalendar

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Vibrator
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.nmd.eventCalendarSample.R
import com.nmd.eventCalendarSample.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    var passwordVisible = false
    private var emailPattern = Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z.]+")
    private var progressDialog: ProgressDialog? = null
    var mAuth: FirebaseAuth? = null
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth!!.currentUser
        if (currentUser != null) {
            currentUser.reload()
            openHomeActivity()
        }
    }

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
        loginButton!!.setOnClickListener(
            View.OnClickListener {
                performAuth()
                val vibrator =
                    getSystemService(VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(15)
            })
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
    }

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
            progressDialog!!.setTitle("Logging in")
            progressDialog!!.setCanceledOnTouchOutside(false)
            progressDialog!!.show()
            mAuth!!.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task: Task<AuthResult?> ->
                    if (task.isSuccessful) {
                        progressDialog!!.dismiss()
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("TAG", "signInWithEmail:success")
                        // updateUI(user);
                        openHomeActivity()
                        Toast.makeText(
                            this@LoginActivity,
                            "Successfully logged in!",
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