package com.bajaj.partneronechat

import android.util.Log
import com.bajaj.partneronechat.WebServicesProvider.Companion.NORMAL_CLOSURE_STATUS
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

private const val TAG = "WebSocketListener"

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
class ChatWebSocketListener : WebSocketListener() {

    val socketEventChannel: Channel<SocketUpdate> = Channel(10)


    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "onOpen")
        GlobalScope.launch {
            socketEventChannel.send(SocketUpdate(exception = ConnectionOpened()))
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "onMessage: $text")
        GlobalScope.launch {
            socketEventChannel.send(SocketUpdate(text))
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        GlobalScope.launch(Dispatchers.Default) {
            socketEventChannel.send(SocketUpdate(exception = SocketAbortedException()))
            webSocket.close(NORMAL_CLOSURE_STATUS, "client closed the connection")
            socketEventChannel.close()
        }
        Log.d(TAG, "onClosing: $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.d(TAG, "onFailure : $t")
        GlobalScope.launch {
            socketEventChannel.send(SocketUpdate(exception = SocketFailedException(t)))
        }
    }
}

class SocketAbortedException : Exception()
class ConnectionOpened : Exception()
class SocketFailedException(t: Throwable) : Exception(t)

data class SocketUpdate(
    val text: String? = null,
    val byteString: ByteString? = null,
    val exception: Throwable? = null
)