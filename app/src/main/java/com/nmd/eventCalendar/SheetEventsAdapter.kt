package com.nmd.eventCalendar

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nmd.eventCalendar.model.Day
import com.nmd.eventCalendar.model.Event
import com.nmd.eventCalendar.model.Memo
import com.nmd.eventCalendar.utils.Utils.Companion.isDarkColor
import com.nmd.eventCalendarSample.databinding.RecyclerViewSheetEventBinding

class SheetEventsAdapter(private var day: Day, private var list: ArrayList<Event>) :
    RecyclerView.Adapter<SheetEventsAdapter.AdapterViewHolder>() {

    private val db = Firebase.firestore
    private val scheduleDatabaseReference = db.collection("schedules")
    private var onClickListener: OnClickListener? = null
    private var onLongClickListener: OnLongClickListener? = null
    private var onButtonClickListener: OnButtonClickListener? = null
    var itemEventMaterialButton: MaterialButton? = null
    var user: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    class AdapterViewHolder(val binding: RecyclerViewSheetEventBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterViewHolder {
        val binding = RecyclerViewSheetEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdapterViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n", "ResourceType")
    override fun onBindViewHolder(holder: AdapterViewHolder, position: Int) {
        with(holder) {
            val item = list[position]
            itemEventMaterialButton = binding.itemEventMaterialButton
            binding.itemEventMaterialTextView.text =
                if (item.startDate == item.endDate) "${item.name}\n${item.startDate}\n${item.startTime}~${item.endTime}"
                else "${item.name}\n${item.startDate}~${item.endDate}\n${item.startTime}~${item.endTime}"
            Log.d("isNullOrEmpty", (!item.memos.isNullOrEmpty() && item.memos!!.any { it.date == day.date }).toString())
            binding.itemEventMaterialButton.setIconResource(
                if (item.email != user?.email) com.nmd.eventCalendarSample.R.drawable.ic_baseline_visibility_24
                else if (!item.memos.isNullOrEmpty() && item.memos!!.any { it.date == day.date }) R.drawable.icon_pencil
                else android.R.drawable.ic_menu_add
            )
            if (item.email != user?.email && (item.memos == null || !item.memos?.any { it.date == day.date }!!)) binding.itemEventMaterialButton.visibility = View.GONE
            val color = Color.parseColor(item.backgroundHexColor)
            binding.itemEventMaterialTextView.setTextColor(
                ContextCompat.getColor(
                    binding.itemEventMaterialTextView.context,
                    if (color.isDarkColor()) R.color.ecv_white else R.color.ecv_black
                )
            )
            binding.itemEventMaterialButton.setIconTintResource(
                if (color.isDarkColor()) R.color.ecv_white else R.color.ecv_black
            )
            binding.root.setCardBackgroundColor(color)
            binding.root.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick(position, item )
                }
            }
            binding.root.setOnLongClickListener {
                if (onLongClickListener != null) {
                    onLongClickListener!!.onLongClick(position, item)
                }
                return@setOnLongClickListener true
            }
            binding.itemEventMaterialButton.setOnClickListener {
                onButtonClickListener?.onButtonClickListener(position, item)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    // A function to bind the onclickListener.
    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    fun setOnLongClickListener(onLongClickListener: OnLongClickListener) {
        this.onLongClickListener = onLongClickListener
    }

    fun setOnButtonClickListener(onButtonClickListener: OnButtonClickListener) {
        this.onButtonClickListener = onButtonClickListener
    }

    // onClickListener Interface
    interface OnClickListener {
        fun onClick(position: Int, model: Event)
    }

    interface OnLongClickListener {
        fun onLongClick(position: Int, model: Event)
    }

    interface OnButtonClickListener {
        fun onButtonClickListener(position: Int, model: Event)
    }
}