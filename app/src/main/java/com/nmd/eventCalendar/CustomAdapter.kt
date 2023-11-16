package com.nmd.eventCalendar

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.nmd.eventCalendar.model.User
import com.nmd.eventCalendarSample.R

class CustomAdapter(private val dataSet: ArrayList<User>, var mContext: Context) :
    ArrayAdapter<User?>(mContext, R.layout.request_item, dataSet as List<User?>), View.OnClickListener {
    // View lookup cache
    private class ViewHolder {
        var txtName: TextView? = null
        var txtEmail: TextView? = null
    }

    override fun onClick(v: View) {
        val position = v.tag as Int
        val `object`: Any? = getItem(position)
        val dataModel = `object` as DataModel?
        when (v.id) {
            R.id.item_info -> Snackbar.make(
                v,
                "Release date " + dataModel!!.status,
                Snackbar.LENGTH_LONG
            )
                .setAction("No action", null).show()
        }
    }

    private var lastPosition = -1
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the data item for this position
        var view = convertView
        val dataModel = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        val viewHolder: ViewHolder // view lookup cache stored in tag
        val result: View?
        if (view == null) {
            viewHolder = ViewHolder()
            val inflater: LayoutInflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.request_item, parent, false)
            viewHolder.txtName = view.findViewById<View>(R.id.name) as TextView
            viewHolder.txtEmail = view.findViewById<View>(R.id.email) as TextView
            result = view
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
            result = view
        }
        lastPosition = position
        viewHolder.txtName!!.text = dataModel!!.displayName
        viewHolder.txtEmail!!.text = dataModel.email
        return view!!
    }
}