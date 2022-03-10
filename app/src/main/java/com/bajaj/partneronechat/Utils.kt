package com.bajaj.partneronechat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.util.Base64
import android.util.TypedValue
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "Utils"

fun getBitmapFromString(string: String): Bitmap {
    val imageBytes = Base64.decode(string, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
fun getTimeAsFormatted(time: Long): String {
    val date = Date(time)
    return sdf.format(date)
}

fun getPrimaryColor(context: Context): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
    return typedValue.data
}

fun getScaledBitmap(originalBitmap: Bitmap, width: Int, height: Int): Bitmap {
//    Log.d(TAG, "getScaledBitmap: width: $width, height: $height")

    val currentWidth = originalBitmap.width
    val currentHeight = originalBitmap.height

    val requiredWidth = maxOf(1, width)
    val requiredHeight = maxOf(1, height)

    var newWidth = requiredWidth
    var newHeight = (currentHeight * (requiredWidth.toDouble() / currentWidth)).toInt()

    if (newHeight < requiredHeight) {
        newHeight = requiredHeight
        newWidth = (currentWidth * (requiredHeight.toDouble() / currentHeight)).toInt()
    }

    newHeight = maxOf(newHeight, 1)
    newWidth = maxOf(newWidth, 1)
//    Log.d(TAG, "getScaledBitmap: newWidth: $newWidth, newHeight: $newHeight")
    return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
}


val sameDayFormat = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
fun isSameDay(currentMessageTime: Long, previousMessageTime: Long): Boolean {
    return sameDayFormat.format(Date(currentMessageTime))
        .equals(sameDayFormat.format(Date(previousMessageTime)))
}

fun isYesterday(timeInMillis: Long) : Boolean {
    return DateUtils.isToday(timeInMillis + DateUtils.DAY_IN_MILLIS)
}

val fullDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH)
fun getFullDateFormat(timeInMillis: Long) : String {
    return fullDateFormat.format(timeInMillis)
}

fun getDateTextToDisplay(sentTime: Long): String {
    return when {
        DateUtils.isToday(sentTime) -> "Today"
        isYesterday(sentTime) -> "Yesterday"
        else -> getFullDateFormat(sentTime)
    }
}
