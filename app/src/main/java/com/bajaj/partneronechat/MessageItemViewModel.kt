package com.bajaj.partneronechat

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.LayoutRes
import com.bajaj.partneronechat.ItemViewType.BROADCAST
import com.bajaj.partneronechat.ItemViewType.DOUBLE_IMAGE_RECEIVED
import com.bajaj.partneronechat.ItemViewType.DOUBLE_IMAGE_SENT
import com.bajaj.partneronechat.ItemViewType.IMAGE_RECEIVED
import com.bajaj.partneronechat.ItemViewType.IMAGE_SENT
import com.bajaj.partneronechat.ItemViewType.MESSAGE_RECEIVED
import com.bajaj.partneronechat.ItemViewType.MESSAGE_SENT
import com.bajaj.partneronechat.ItemViewType.MULTI_IMAGE_RECEIVED
import com.bajaj.partneronechat.ItemViewType.MULTI_IMAGE_SENT
import com.bajaj.partneronechat.ItemViewType.TRIPLE_IMAGE_RECEIVED
import com.bajaj.partneronechat.ItemViewType.TRIPLE_IMAGE_SENT
import com.bajaj.partneronechat.observers.DateBroadcast

interface MessageItemViewModel {
    @get:LayoutRes
    val layoutId: Int
    val viewType: Int
        get() = 0
    val name: String
    val sentTime: Long
    var deliveredTime: Long?
    var readTime: Long?
    val sentTimeAsFormatted: String
        get() = getTimeAsFormatted(sentTime)
}

data class TextSentViewModel(
    override val name: String,
    val message: String,
    override val sentTime: Long,
    override var deliveredTime: Long? = null,
    override var readTime: Long? = null
) : MessageItemViewModel {
    override val layoutId: Int = R.layout.item_message_sent
    override val viewType: Int = MESSAGE_SENT

    override val sentTimeAsFormatted: String
        get() = super.sentTimeAsFormatted

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        other as TextSentViewModel

        if (name != other.name) return false
        if (sentTime != other.sentTime) return false
        if (viewType != other.viewType) return false
        return true
    }
}

data class TextReceivedViewModel(
    override val name: String,
    val message: String,
    override val sentTime: Long,
    override var deliveredTime: Long? = null,
    override var readTime: Long? = null,
) :
    MessageItemViewModel {
    override val layoutId: Int = R.layout.item_message_received
    override val viewType: Int = MESSAGE_RECEIVED

    override val sentTimeAsFormatted: String
        get() = super.sentTimeAsFormatted

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false

        other as TextReceivedViewModel

        if (name != other.name) return false
        if (sentTime != other.sentTime) return false
        if (viewType != other.viewType) return false
        return true
    }
}

data class ImageSentViewModel(
    override val name: String,
    val originalBitmap: Bitmap,
    val bitmapDrawable: BitmapDrawable? = null,
    override val sentTime: Long,
    override var deliveredTime: Long? = null,
    override var readTime: Long? = null
) : MessageItemViewModel {
    override val layoutId: Int = R.layout.item_image_sent
    override val viewType: Int = IMAGE_SENT
    override val sentTimeAsFormatted: String
        get() = super.sentTimeAsFormatted

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        other as ImageSentViewModel

        if (name != other.name) return false
        if (sentTime != other.sentTime) return false
        if (viewType != other.viewType) return false
        return true
    }
}

data class MultipleImageSentViewModel(
    override val name: String,
    override var sentTime: Long,
    override var deliveredTime: Long? = null,
    override var readTime: Long? = null
) : MessageItemViewModel {
    override val layoutId: Int
        get() =
            when (imagesList.size) {
                1 -> R.layout.item_image_sent
                2 -> R.layout.item_double_image_sent
                3 -> R.layout.item_triple_image_sent
                else -> R.layout.item_multiple_image_sent

            }
    override val viewType: Int
        get() = when (imagesList.size) {
            1 -> IMAGE_SENT
            2 -> DOUBLE_IMAGE_SENT
            3 -> TRIPLE_IMAGE_SENT
            else -> MULTI_IMAGE_SENT
        }
    override val sentTimeAsFormatted: String
        get() = super.sentTimeAsFormatted

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        other as MultipleImageSentViewModel

        if (name != other.name) return false
        if (sentTime != other.sentTime) return false
        if (viewType != other.viewType) return false
        if (imagesList != other.imagesList) return false
        return true
    }

    var imagesList = ArrayList<ImageSentViewModel>()
    var onCLick: ImageItemListener? = null
}

data class ImageReceivedViewModel(
    override val name: String,
    val originalBitmap: Bitmap,
    val bitmapDrawable: BitmapDrawable? = null,
    override val sentTime: Long,
    override var deliveredTime: Long? = null,
    override var readTime: Long? = null,
) : MessageItemViewModel {
    override val layoutId: Int = R.layout.item_image_received
    override val viewType: Int = IMAGE_RECEIVED
    override val sentTimeAsFormatted: String
        get() = super.sentTimeAsFormatted

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        other as ImageReceivedViewModel

        if (name != other.name) return false
        if (sentTime != other.sentTime) return false
        if (viewType != other.viewType) return false
        return true
    }
}

data class MultipleImageReceivedViewModel(
    override val name: String,
    override var sentTime: Long,
    override var deliveredTime: Long? = null,
    override var readTime: Long? = null,
) : MessageItemViewModel {
    override val layoutId: Int
        get() = when (imagesList.size) {
            1 -> R.layout.item_image_received
            2 -> R.layout.item_double_image_received
            3 -> R.layout.item_triple_image_received
            else -> R.layout.item_multiple_image_received
        }
    override val viewType: Int
        get() = when (imagesList.size) {
            1 -> IMAGE_RECEIVED
            2 -> DOUBLE_IMAGE_RECEIVED
            3 -> TRIPLE_IMAGE_RECEIVED
            else -> MULTI_IMAGE_RECEIVED
        }

    override val sentTimeAsFormatted: String
        get() = super.sentTimeAsFormatted

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        other as MultipleImageReceivedViewModel

        if (name != other.name) return false
        if (sentTime != other.sentTime) return false
        if (viewType != other.viewType) return false
        if (imagesList != other.imagesList) return false
        return true
    }

    var imagesList = ArrayList<ImageReceivedViewModel>()
    var onClick: ImageItemListener? = null
}

data class BroadcastViewModel(
    override val name: String,
    override val sentTime: Long,
    override var deliveredTime: Long? = null,
    override var readTime: Long? = null,
    val isJoined: Boolean = false,
    val isLeft: Boolean = false,
    var message: String? = null,
    var dayChangeMessage: String? = null,
    val isSent: Boolean? = null,
    val onDateChangeListener: DateBroadcast.OnDateChangeListener? = null
) : MessageItemViewModel {

    init {
        onDateChangeListener?.let {
            DateBroadcast.setOnDateChangeListener(this)
        }
    }

    override val layoutId: Int = R.layout.item_broadcast
    override val viewType: Int = BROADCAST
    override val sentTimeAsFormatted: String
        get() = super.sentTimeAsFormatted

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        other as BroadcastViewModel

        if (name != other.name) return false
        if (sentTime != other.sentTime) return false
        if (viewType != other.viewType) return false
        if (message != other.message) return false
        if (dayChangeMessage != other.dayChangeMessage) return false
        return true
    }
}