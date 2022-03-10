package com.bajaj.partneronechat

import android.util.Log
import com.bajaj.partneronechat.data.DataStoreManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "WebServicesProvider"

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class WebServicesProvider @Inject constructor(val dataStoreManager: DataStoreManager) {

    private var _webSocket: WebSocket? = null

    private val socketOkHttpClient = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .hostnameVerifier { _, _ -> true }
        .build()

    private var _webSocketListener: ChatWebSocketListener? = null

    suspend fun startSocket(): Channel<SocketUpdate> =
        with(ChatWebSocketListener()) {
            startSocket(this)
            this@with.socketEventChannel
        }

    private suspend fun startSocket(webSocketListener: ChatWebSocketListener) {
        _webSocketListener = webSocketListener
        val ipAddress = dataStoreManager.socketAddress.first().ipAddress
        val port = dataStoreManager.socketAddress.first().port
        _webSocket = socketOkHttpClient.newWebSocket(
            Request.Builder().url(
//                "ws://192.168.1.104:3000"
            "ws://$ipAddress:$port"
            ).build(),
            webSocketListener
        )

//        socketOkHttpClient.dispatcher.executorService.shutdown()
    }

    fun stopSocket() {
        try {
            _webSocket?.close(NORMAL_CLOSURE_STATUS, null)
            _webSocket = null
//            _webSocketListener?.socketEventChannel?.close()
            _webSocketListener = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendData(text: String) {
        _webSocket?.send(text)
        Log.d(TAG, "sendData: $text")
    }

    companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
    }
}