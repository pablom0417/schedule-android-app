package com.nmd.eventCalendar

import android.content.Context
import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.nmd.eventCalendarSample.R

class CustomAdapter(private val dataSet: ArrayList<DataModel>, var mContext: Context) :
    ArrayAdapter<DataModel?>(mContext, R.layout.request_item, dataSet as List<DataModel?>), View.OnClickListener {
    // View lookup cache
    private class ViewHolder {
        var txtName: TextView? = null
        var txtEmail: TextView? = null
        var txtDate: TextView? = null
        var txtStatus: MaterialTextView? = null
        var txtStatus2: MaterialTextView? = null
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
        var convertView = convertView
        val dataModel = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        val viewHolder: ViewHolder // view lookup cache stored in tag
        val result: View?
        if (convertView == null) {
            viewHolder = ViewHolder()
            val inflater: LayoutInflater = LayoutInflater.from(context)
            convertView = inflater.inflate(R.layout.request_item, parent, false)
            viewHolder.txtName = convertView.findViewById<View>(R.id.name) as TextView
            viewHolder.txtEmail = convertView.findViewById<View>(R.id.email) as TextView
            viewHolder.txtDate = convertView.findViewById<View>(R.id.date) as TextView
            viewHolder.txtStatus = convertView.findViewById<View>(R.id.status1) as MaterialTextView
            viewHolder.txtStatus2 = convertView.findViewById<View>(R.id.status2) as MaterialTextView
            result = convertView
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
            result = convertView
        }
        lastPosition = position
        if (dataModel!!.status == "invited") {
            viewHolder.txtStatus!!.visibility = View.VISIBLE
            viewHolder.txtStatus2!!.visibility = View.GONE
        } else {
            viewHolder.txtStatus!!.visibility = View.GONE
            viewHolder.txtStatus2!!.visibility = View.VISIBLE
        }
        viewHolder.txtName!!.text = dataModel!!.name
        viewHolder.txtEmail!!.text = dataModel.email
        viewHolder.txtDate!!.text = dataModel.date
        viewHolder.txtStatus!!.text = dataModel.status
        viewHolder.txtStatus2!!.text = dataModel.status
        viewHolder.txtStatus!!.setOnClickListener(this)
        viewHolder.txtStatus!!.tag = position
        viewHolder.txtStatus2!!.setOnClickListener(this)
        viewHolder.txtStatus2!!.tag = position
        // Return the completed view to render on screen
        return convertView!!
    }
}