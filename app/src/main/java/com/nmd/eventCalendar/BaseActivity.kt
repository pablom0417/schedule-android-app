package com.nmd.eventCalendar

import android.os.Bundle
import android.os.Handler.Callback
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.nmd.eventCalendar.util.AppPrefs
import com.nmd.eventCalendarSample.R


open class BaseActivity : AppCompatActivity() {

    var mPrefs: AppPrefs? = null
    val AUTH = FirebaseAuth.getInstance()
    val AUTH_USER =
        if (FirebaseAuth.getInstance().currentUser != null) FirebaseAuth.getInstance().currentUser else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        mPrefs = AppPrefs.create(this);
    }

    protected fun verifyEmail(firebaseUser: FirebaseUser) {
        firebaseUser.sendEmailVerification().addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(applicationContext,  "Email was successfully sent", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(applicationContext, "Sending email failed", Toast.LENGTH_LONG).show()
            }
        }.addOnCanceledListener {
            Toast.makeText(applicationContext, "An error was occurred", Toast.LENGTH_LONG).show();
        }
    }

    open fun checkIfEmailVerified(firebaseUser: FirebaseUser?): Boolean {
        return firebaseUser?.isEmailVerified ?: false
    }

    protected fun resetPassword(emailId: String, callback: Callback){
        AUTH.sendPasswordResetEmail(emailId)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(applicationContext, "Success", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(applicationContext, it.exception!!.message, Toast.LENGTH_LONG).show();
                }
            }
    }
}