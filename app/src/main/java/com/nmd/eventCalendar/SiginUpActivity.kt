package com.nmd.eventCalendar

import android.annotation.SuppressLint
import android.app.ProgressDialog
//import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.nmd.eventCalendarSample.R
import com.nmd.eventCalendarSample.databinding.ActivitySignupBinding
import java.util.Objects

class SignUpActivity : AppCompatActivity() {
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        password1 = binding.password1
        name = binding.name
        emailAddress1 = binding.emailAddress1
        signupButton = binding.signupButton
        name = binding.name
        progressDialog = ProgressDialog(this)
        mAuth = FirebaseAuth.getInstance()
        login = findViewById(R.id.login)

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

    private fun performAuth() {
        val email = emailAddress1.text.toString()
        val password = password1.text.toString()

        if (!email.matches(emailPattern.toRegex())) {
            Log.d("TAG", "Enter a valid email!")
            emailAddress1.error = "Enter a valid email!"
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
                    progressDialog.dismiss()
                    Log.d("TAG", "createUserWithEmail:success")
                    openLoginActivity()
                    Toast.makeText(this@SignUpActivity, "Successfully registered!", Toast.LENGTH_SHORT).show()
                } else {
                    progressDialog.dismiss()
                    Log.w("TAG", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this@SignUpActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun openLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(15)
    }
}