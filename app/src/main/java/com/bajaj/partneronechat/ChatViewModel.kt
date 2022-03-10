package com.bajaj.partneronechat

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bajaj.partneronechat.observers.DateBroadcast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject

private const val TAG = "ChatViewModel"

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val interactor: MainInteractor
) : ViewModel() {

    private val _chatUiState = MutableLiveData<ChatUIState?>()
    val chatUIState: LiveData<ChatUIState?>
        get() = _chatUiState

    private val _messages = ArrayList<MessageItemViewModel>()

    private val _messagesLiveData: MutableLiveData<ArrayList<MessageItemViewModel>> =
        MutableLiveData()
    val messages: LiveData<ArrayList<MessageItemViewModel>>
        get() = _messagesLiveData

    private var _isConnectionOpen: Boolean = false

    var name = ""

    init {
        subscribeToSocketEvents()
    }

    fun subscribeToSocketEvents() {
        _chatUiState.postValue(ChatUIState.Loading)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                interactor.startSocket().consumeEach {
                    if (it.exception == null) {
                        it.text?.let { text ->
                            addReceivedMessageToList(text)
                        }
                    } else {
                        it.exception.printStackTrace()
                        if (it.exception is ConnectionOpened) {
                            _isConnectionOpen = true
                            _chatUiState.postValue(ChatUIState.Success("Connection Successful" ))
                            sendBroadcastMessage(EXTRA_JOINED)
                        } else if (it.exception is SocketFailedException || it.exception is SocketAbortedException) {
                            if (_isConnectionOpen) {
                                sendBroadcastMessage(EXTRA_LEFT)
                            }
                            _isConnectionOpen = false
                            _chatUiState.postValue(ChatUIState.Error("Connection Failed"))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isConnectionOpen = false
                _chatUiState.postValue(ChatUIState.Error("Connection Failed"))
            }
        }
    }

    fun successMessageHandled() {
        _chatUiState.postValue(ChatUIState.Success(null))
    }

    private fun sendPayload(text: String) {
        interactor.sendData(text)
    }

    private fun addReceivedMessageToList(text: String) {
        val jsonObject = JSONObject(text)

        when {
            jsonObject.has(EXTRA_MESSAGE) -> {
                updateMessages(
                    TextReceivedViewModel(
                        jsonObject.getString(EXTRA_NAME),
                        jsonObject.getString(EXTRA_MESSAGE),
                        jsonObject.getLong(EXTRA_SENT_TIME),
                        deliveredTime = System.currentTimeMillis()
                    )
                )
            }
            jsonObject.has(EXTRA_IMAGE) -> {
                updateMessages(
                    ImageReceivedViewModel(
                        jsonObject.getString(EXTRA_NAME),
                        getBitmapFromString(jsonObject.getString(EXTRA_IMAGE)),
                        sentTime = jsonObject.getLong(EXTRA_SENT_TIME)
                    )
                )
            }
            jsonObject.has(EXTRA_BROADCAST) -> {
                updateMessages(
                    BroadcastViewModel(
                        jsonObject.getString(EXTRA_NAME),
                        sentTime = jsonObject.getLong(EXTRA_SENT_TIME),
                        isSent = false,
                        isJoined = jsonObject.getString(EXTRA_BROADCAST) == EXTRA_JOINED,
                        isLeft = jsonObject.getString(EXTRA_BROADCAST) == EXTRA_LEFT
                    )
                )
            }
            else -> throw IllegalArgumentException("Invalid data received")
        }
    }

    fun sendText(message: String) {

        val messageObject = JSONObject()

        messageObject.put(EXTRA_NAME, name)
        messageObject.put(EXTRA_MESSAGE, message)
        val sentTime = System.currentTimeMillis()
        messageObject.put(EXTRA_SENT_TIME, sentTime)

        updateMessages(
            TextSentViewModel(
                name, message, sentTime
            )
        )

        sendPayload(messageObject.toString())
    }

    fun sendImage(image: Bitmap) {
        val outputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)

        val imageString = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        val imageObject = JSONObject()

        imageObject.put(EXTRA_NAME, name)
        imageObject.put(EXTRA_IMAGE, imageString)
        val sentTime = System.currentTimeMillis()
        imageObject.put(EXTRA_SENT_TIME, sentTime)

        updateMessages(
            ImageSentViewModel(
                name, image, sentTime = sentTime,
            )
        )

        sendPayload(imageObject.toString())
    }

    private fun sendBroadcastMessage(message: String) {

        val broadCastObject = JSONObject()
        val sentTime = System.currentTimeMillis()

        broadCastObject.apply {
            put(EXTRA_NAME, name)
            put(EXTRA_BROADCAST, message)
            put(EXTRA_SENT_TIME, sentTime)
        }

        updateMessages(
            BroadcastViewModel(
                name,
                sentTime = sentTime,
                isSent = true,
                isJoined = message == EXTRA_JOINED,
                isLeft = message == EXTRA_LEFT
            )
        )

        sendPayload(broadCastObject.toString())
    }

    private fun updateMessages(messageItem: MessageItemViewModel) {

        _messages.add(messageItem)
        _messagesLiveData.postValue(_messages)
    }

    override fun onCleared() {
        sendBroadcastMessage(EXTRA_LEFT)
        interactor.stopSocket()
        Log.d(TAG, "onCleared: $this")
        super.onCleared()
    }
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class MainInteractor @Inject constructor(
    private val repository: MainRepository
) {
    fun stopSocket() {
        repository.closeSocket()
    }

    fun sendData(text: String) {
        repository.sendData(text)
    }

    suspend fun startSocket(): Channel<SocketUpdate> = repository.startSocket()
}

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class MainRepository @Inject constructor(private val webservicesProvider: WebServicesProvider) {

    suspend fun startSocket(): Channel<SocketUpdate> = webservicesProvider.startSocket()

    fun closeSocket() {
        webservicesProvider.stopSocket()
    }

    fun sendData(text: String) {
        webservicesProvider.sendData(text)
    }
}