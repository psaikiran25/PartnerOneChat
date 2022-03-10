package com.bajaj.partneronechat.observers

import android.util.Log
import com.bajaj.partneronechat.BroadcastViewModel
import com.bajaj.partneronechat.getDateTextToDisplay

private const val TAG = "DateBroadcast"

data class DateChangeListener(
    val broadcastViewModel: BroadcastViewModel,
    val listener: DateBroadcast.OnDateChangeListener
)

class DateBroadcast {

    interface OnDateChangeListener {
        fun onDateChange(broadcastViewModel: BroadcastViewModel) {
            val sentTime = broadcastViewModel.sentTime
            broadcastViewModel.dayChangeMessage?.let {
                broadcastViewModel.dayChangeMessage = getDateTextToDisplay(sentTime)
            }
//            Log.d(TAG, "onDateChange: ${broadcastViewModel.dayChangeMessage}")
        }
    }

    companion object {
        private val listeners = ArrayList<DateChangeListener>()
        fun setOnDateChangeListener(broadcastViewModel: BroadcastViewModel) {
            listeners.add(
                DateChangeListener(
                    broadcastViewModel, broadcastViewModel.onDateChangeListener ?: return
                )
            )
        }

        fun updateDates() {
            notifyAllListeners()
        }

        private fun notifyAllListeners() {
            for (listener in listeners) {
                listener.listener.onDateChange(listener.broadcastViewModel)
            }
        }
    }
}
