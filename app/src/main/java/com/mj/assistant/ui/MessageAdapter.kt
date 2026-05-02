package com.mj.assistant.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mj.assistant.databinding.ItemMessageBinding

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.binding.apply {
            if (msg.isUser) {
                llUser.visibility = View.VISIBLE
                llMj.visibility = View.GONE
                tvUserMessage.text = msg.text
            } else {
                llUser.visibility = View.GONE
                llMj.visibility = View.VISIBLE
                tvMjMessage.text = msg.text
            }
        }
    }

    override fun getItemCount() = messages.size
}
