package com.nmd.eventCalendar

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nmd.eventCalendar.model.Event
import com.nmd.eventCalendar.utils.Utils.Companion.isDarkColor
import com.nmd.eventCalendarSample.databinding.RecyclerViewSheetEventBinding

class SheetEventsAdapter(private var list: ArrayList<Event>) :
    RecyclerView.Adapter<SheetEventsAdapter.AdapterViewHolder>() {

    private var onClickListener: OnClickListener? = null
    private var onLongClickListener: OnLongClickListener? = null

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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AdapterViewHolder, position: Int) {
        with(holder) {
            val item = list[position]
            binding.itemEventMaterialTextView.text =
                if (item.startDate == item.endDate) "${item.name} - ${item.startDate} ${item.startTime}~${item.endTime}"
                else "${item.name} - ${item.startDate} ~ ${item.endDate} ${item.startTime}~${item.endTime}"

            val color = Color.parseColor(item.backgroundHexColor)
            binding.itemEventMaterialTextView.setTextColor(
                ContextCompat.getColor(
                    binding.itemEventMaterialTextView.context,
                    if (color.isDarkColor()) R.color.ecv_white else R.color.ecv_black
                )
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

    // onClickListener Interface
    interface OnClickListener {
        fun onClick(position: Int, model: Event)
    }

    interface OnLongClickListener {
        fun onLongClick(position: Int, model: Event)
    }
}