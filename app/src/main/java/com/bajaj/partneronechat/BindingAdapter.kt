package com.bajaj.partneronechat

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

private const val TAG = "BindingAdapter"

@BindingAdapter("setBroadcastMessage")
fun TextView.setBroadCastMessage(itemViewModel: BroadcastViewModel) {
    itemViewModel.apply {
        if (isJoined || isLeft) {
            val firstString = if (isSent == true) "You" else name
            val secondString = if (isJoined) "joined" else "left"
            val textToDisplay = "$firstString $secondString"

            text = SpannableString(textToDisplay).apply {
                setSpan(
                    ForegroundColorSpan(getPrimaryColor(context)),
                    0,
                    firstString.length + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return
        }

        if (dayChangeMessage != null) {
            text = dayChangeMessage
//            Log.d(TAG, "setBroadCastMessage: text: $text")
            return
        }
    }
}

@BindingAdapter("multiImageTextAndVisibility")
fun TextView.multiImageTextAndVisibility(itemViewModel: MessageItemViewModel) {
    when (itemViewModel) {
        is MultipleImageSentViewModel -> {
            val size = itemViewModel.imagesList.size
            if (size >= 5) {
                visibility = View.VISIBLE
                val text = "+${size - 3}"
                setText(text)
            } else visibility = View.GONE
        }
        is MultipleImageReceivedViewModel -> {
            val size = itemViewModel.imagesList.size
            if (size >= 5) {
                visibility = View.VISIBLE
                val text = "+${size - 3}"
                setText(text)
            } else visibility = View.GONE
        }
    }
}

@BindingAdapter("setReceivedChatImage", "imagePosition")
fun ImageView.setReceivedChatImage(multipleImageReceivedViewModel: MultipleImageReceivedViewModel, position: Int) {
    clipToOutline = true
    setOnClickListener {
        multipleImageReceivedViewModel.onClick?.onClick(position, multipleImageReceivedViewModel, this)
    }
    setImageBitmap(getScaledBitmap(multipleImageReceivedViewModel.imagesList[position].originalBitmap, width, height))
}

@BindingAdapter("setSentChatImage", "imagePosition")
fun ImageView.setSentChatImage(multipleImageSentViewModel: MultipleImageSentViewModel, position: Int) {
    clipToOutline = true
//    android:onClick="@{(imageView) -> itemViewModel.onCLick.onClick(0, itemViewModel, imageView)}"
    setOnClickListener {
        multipleImageSentViewModel.onCLick?.onClick(position, multipleImageSentViewModel, this)
    }
    setImageBitmap(getScaledBitmap(multipleImageSentViewModel.imagesList[position].originalBitmap, width, height))
}

