package com.bajaj.partneronechat

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bajaj.partneronechat.databinding.ItemDoubleImageSentBinding
import com.bajaj.partneronechat.databinding.ItemImageSentBinding

private const val TAG = "MessagesAdapter"

class MessagesAdapter(private val clickListener: ImageItemListener) :
    ListAdapter<MessageItemViewModel, MessagesAdapter.CommonViewHolder>(MessageDiffCallback()) {

    private val viewTypeToLayoutId: MutableMap<Int, Int> = mutableMapOf()

    inner class CommonViewHolder(private val binding: ViewDataBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(messageItemViewModel: MessageItemViewModel) {
//            Log.d(TAG, "bind: $messageItemViewModel")
            when (messageItemViewModel) {
                is MultipleImageSentViewModel -> {
                    messageItemViewModel.onCLick = clickListener
                }
                is MultipleImageReceivedViewModel -> {
                    messageItemViewModel.onClick = clickListener
                }
            }
            binding.setVariable(BR.itemViewModel, messageItemViewModel)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonViewHolder {

        val binding: ViewDataBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            viewTypeToLayoutId[viewType] ?: 0,
            parent, false
        )
//        Log.d(TAG, "onCreateViewHolder: viewTypeLayoutId - ${viewTypeToLayoutId[viewType]}")
        return CommonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommonViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun getItemViewType(position: Int): Int {

        val item = getItem(position)
        if (!viewTypeToLayoutId.containsKey(item.viewType)) {
            viewTypeToLayoutId[item.viewType] = item.layoutId
        }
//        Log.d(TAG, "getItemViewType: ${item.viewType}")
        return item.viewType
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<MessageItemViewModel>() {
    override fun areItemsTheSame(
        oldItem: MessageItemViewModel,
        newItem: MessageItemViewModel
    ): Boolean {
        return (oldItem.sentTime == newItem.sentTime && oldItem.name == newItem.name)
    }

    override fun areContentsTheSame(
        oldItem: MessageItemViewModel,
        newItem: MessageItemViewModel
    ): Boolean {
        return oldItem.equals(newItem)
    }

}

class ImageItemListener(val clickListener: (position: Int, itemViewModel: MessageItemViewModel, imageView: View) -> Unit)  {
    fun onClick(position: Int, itemViewModel: MessageItemViewModel, imageView: View) = clickListener(position, itemViewModel, imageView)
}

object ItemViewType {
    const val MESSAGE_SENT = 0
    const val MESSAGE_RECEIVED = 1
    const val BROADCAST = 2
    const val IMAGE_SENT = 3
    const val IMAGE_RECEIVED = 4
    const val DOUBLE_IMAGE_SENT = 5
    const val DOUBLE_IMAGE_RECEIVED = 6
    const val TRIPLE_IMAGE_SENT = 7
    const val TRIPLE_IMAGE_RECEIVED = 8
    const val MULTI_IMAGE_SENT = 9
    const val MULTI_IMAGE_RECEIVED = 10
}