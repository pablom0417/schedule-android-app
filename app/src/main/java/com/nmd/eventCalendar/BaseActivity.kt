package com.nmd.eventCalendar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nmd.eventCalendar.util.AppPrefs
import com.nmd.eventCalendarSample.R


open class BaseActivity : AppCompatActivity() {

    var mPrefs: AppPrefs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        mPrefs = AppPrefs.create(this);
    }

}