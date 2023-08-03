package com.example.tts.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.tts.databinding.ItemTestListBinding
import com.example.tts.model.bean.TestItem

class TestItemAdapter() : ListAdapter<TestItem, ViewHolder>(MyDiffCallback()) {

    var onItemClicked: ((TestItem) -> Unit)? = null


    class MyDiffCallback : DiffUtil.ItemCallback<TestItem>() {
        override fun areItemsTheSame(
            oldItem: TestItem, newItem: TestItem
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: TestItem, newItem: TestItem
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return MyViewHolder(
            ItemTestListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        if (holder is MyViewHolder) {
            holder.bind(data)
        }
    }

    inner class MyViewHolder(private val binding: ItemTestListBinding) : ViewHolder(binding.root) {
        fun bind(data: TestItem) {
            binding.tvTitle.text = data.language
            binding.tvDesc.text = data.content

            binding.root.setOnClickListener {
                onItemClicked?.invoke(data)
            }
        }
    }
}