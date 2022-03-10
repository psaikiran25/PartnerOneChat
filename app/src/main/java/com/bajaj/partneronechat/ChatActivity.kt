package com.bajaj.partneronechat

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bajaj.partneronechat.databinding.ActivityChatBinding
import com.bajaj.partneronechat.observers.DateBroadcast
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.snackbar.Snackbar
import com.stfalcon.imageviewer.StfalconImageViewer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

const val EXTRA_MESSAGE = "message"
const val EXTRA_IMAGE = "image"
const val EXTRA_BROADCAST = "broadcast"
const val EXTRA_SENT_TIME = "time"
const val EXTRA_JOINED = "joined"
const val EXTRA_LEFT = "left"

private const val TAG = "ChatActivity"

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    private lateinit var name: String
    private var snackbar: Snackbar? = null
    private lateinit var binding: ActivityChatBinding
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var chatLayoutManager: LinearLayoutManager
    private val getImage =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            uris?.let {
                lifecycleScope.launch(Dispatchers.Default) {
                    for (uri in it) {
                        val inputStream = contentResolver.openInputStream(uri)
                        val image: Bitmap = BitmapFactory.decodeStream(inputStream)

                        viewModel.sendImage(image)
                    }
                }
            }
        }

    private val dateChangedReceiver = object : DateChangedReceiver() {
        override fun onDayChanged() {
            Log.d(TAG, "onDayChanged in ChatActivity")
            DateBroadcast.updateDates()
            messagesAdapter.submitList(messagesAdapter.currentList)
        }
    }

    private val viewModel: ChatViewModel by viewModels()

    private var isLastItemVisible: Boolean? = null
        set(value) {
            field = value
//            Log.d(TAG, "isLastItemVisible: $value")
            value?.let {
                if (it) {
                    binding.moveToBottomFab.hide()
                } else {
                    binding.moveToBottomFab.show()
                }
            }
        }
    private var messageCountBadgeDrawable: BadgeDrawable? = null
    private var noOfUnReadMessages: Int = 0
        @SuppressLint("UnsafeOptInUsageError")
        set(value) {
            val was = noOfUnReadMessages
            if (was == value) return
            Log.d(TAG, "noOfUnReadMessages: $value")
            messageCountBadgeDrawable?.apply {
                if (value > 0) {
                    this.number = value
                    this.isVisible = true
                    BadgeUtils.attachBadgeDrawable(this, binding.moveToBottomFab)
                } else {
                    this.clearNumber()
                    this.isVisible = false
                    BadgeUtils.detachBadgeDrawable(this, binding.moveToBottomFab)
                }
            }
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityBinding = ActivityChatBinding.inflate(layoutInflater)
        binding = activityBinding
        name = intent.getStringExtra(EXTRA_NAME).toString()

        viewModel.name = name

        messagesAdapter = MessagesAdapter(ImageItemListener { position, itemViewModel, view ->
//            Toast.makeText(this, itemViewModel.name, Toast.LENGTH_SHORT).show()
            onImageClicked(position, itemViewModel, view)
        })

        binding.chatRecyclerView.apply {
            adapter = messagesAdapter

            addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
//                Log.d(TAG, "onCreate: oldBottom - bottom = ${oldBottom - bottom}")
                if (bottom < oldBottom) {
                    smoothScrollBy(0, (oldBottom - bottom))
//                    isLastItemVisible?.let {
//                        if (it) Log.d(TAG, "onCreate: layoutChangeListener: Last Item Visible")
//                        else Log.d(TAG, "onCreate: layoutChangeListener: Last Item NOT Visible")
//                    }
                }
            }

            layoutManager = LinearLayoutManager(this@ChatActivity).also { lm ->
                chatLayoutManager = lm
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                        Log.d(TAG, "onScrolled: dx = $dx, dy = $dy")
                        super.onScrolled(recyclerView, dx, dy)
                        val visibleItemCount = lm.childCount
                        val totalItemCount = lm.itemCount
                        val firstVisibleItemPosition = lm.findFirstVisibleItemPosition()
                        val lastVisibleItemPosition = lm.findLastVisibleItemPosition()
//                        Log.d(
//                            TAG, "onScrolled: \n" +
//                                    "visibleItemCount: $visibleItemCount\n" +
//                                    "totalItemCount: $totalItemCount\n" +
//                                    "firstVisibleItemPosition: $firstVisibleItemPosition\n" +
//                                    "lastVisibleItemPosition: $lastVisibleItemPosition"
//                        )
                        if(totalItemCount <= 0) return
                        if (lastVisibleItemPosition == totalItemCount - 1) {
                            if (isLastItemVisible != true) {
                                isLastItemVisible = true
                            }
                        } else {
                            if (isLastItemVisible != false) {
                                isLastItemVisible = false
                            }
                        }

                        if (dy > 0 && noOfUnReadMessages > 0) {
                            run addReadTime@{
                                messagesAdapter.currentList.subList(0, lastVisibleItemPosition + 1)
                                    .asReversed().forEach {
                                        it?.also { messageItem ->
                                            if (messageItem is TextReceivedViewModel || messageItem is MultipleImageReceivedViewModel) {
                                                if (messageItem.readTime != null) return@addReadTime
                                                messageItem.readTime =
                                                    System.currentTimeMillis()
                                                noOfUnReadMessages--
                                            }
                                        }
                                    }
                            }
                        }
                    }
                })
            }
        }
        binding.moveToBottomFab.hide()
        setContentView(activityBinding.root)

        setUpObservers()
    }

    private fun setUpObservers() {

        viewModel.chatUIState.observe(this) {
            it?.let {
                when (it) {
                    is ChatUIState.Error -> {
                        snackbar?.dismiss()
                        it.errorMessage?.let { errorMessage ->
                            snackbar =
                                Snackbar.make(
                                    binding.root,
                                    errorMessage,
                                    Snackbar.LENGTH_INDEFINITE
                                )
                                    .setAction("RETRY") {
                                        viewModel.subscribeToSocketEvents()
                                    }.apply { show() }
                        }
                        setLoadingBar(false)
                    }

                    is ChatUIState.Success -> {
//                        Log.d(TAG, "setUpObservers: successMessage: ${it.successMessage}")
                        it.successMessage?.let {
                            snackbar = Snackbar.make(
                                binding.root,
                                "Connection Successful",
                                Snackbar.LENGTH_SHORT
                            ).setAction("OK") {
                                snackbar?.dismiss()
                            }.apply { show() }
                            viewModel.successMessageHandled()
                        }
                        setLoadingBar(false)
                        initializeViews()
                    }

                    is ChatUIState.Loading -> {
                        snackbar?.dismiss()
                        setLoadingBar(true)
                    }
                }
            }

        }
    }

    private fun setLoadingBar(show: Boolean) {
        binding.loadingBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun initializeViews() {

        binding.messageEditText.doOnTextChanged { text, start, before, count ->
            if (text.toString().trim().isNotEmpty()) {
                binding.sendImageButton.visibility = View.INVISIBLE
                binding.sendMessageButton.visibility = View.VISIBLE
            } else {
                binding.sendImageButton.visibility = View.VISIBLE
                binding.sendMessageButton.visibility = View.INVISIBLE
            }
        }

        binding.sendMessageButton.setOnClickListener {
            val message = binding.messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendText(message)
                resetMessageEditText()
            } else {
                Toast.makeText(this, "Message can't be empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.sendImageButton.setOnClickListener {
            getImage.launch("image/*")
        }

        binding.moveToBottomFab.setOnClickListener {
            if (messagesAdapter.itemCount > 0) {
                binding.chatRecyclerView.smoothScrollToPosition(messagesAdapter.itemCount - 1)
            }
        }

        messageCountBadgeDrawable = BadgeDrawable.create(this).apply {
            horizontalOffset = 10.dpToPx()
            verticalOffset = 10.dpToPx()
            badgeGravity = BadgeDrawable.TOP_START
        }.apply {
            BadgeUtils.attachBadgeDrawable(this, binding.moveToBottomFab)
        }

        viewModel.messages.observe(this) {
            it?.let {
                updateMessagesToRecyclerView(it)
            }
        }
    }

    private fun updateMessagesToRecyclerView(messages: ArrayList<MessageItemViewModel>) {
//        DateBroadcast.updateDates()
//        Log.d(TAG, "updateMessagesToRecyclerView: messages - ${messages.size}")
//        for (message in messages) {
//            if (message is BroadcastViewModel)
//                Log.d(TAG, "setUpObservers: messages: ${message.dayChangeMessage}")
//        }

        // adding date stamps
        val messageItems = ArrayList<MessageItemViewModel>()
        if (messages.isNotEmpty()) {
            messageItems.add(getDateItem(messages[0].sentTime))
            for (message in messages) {
                if (isSameDay(message.sentTime, messageItems.last().sentTime)) {
                    messageItems.add(message)
                } else {
                    messageItems.add(getDateItem(message.sentTime))
                    messageItems.add(message)
                }
            }
        }

//        Log.d(TAG, "updateMessagesToRecyclerView: messageItems: ${messageItems.size}")

        //combining multiple images
        val consolidatedMessageItems = ArrayList<MessageItemViewModel>()
        if (messageItems.isNotEmpty()) {
//            consolidatedMessageItems.add(messageItems[0])
            messageItems.first().also { firstMessage ->
                when (firstMessage) {
                    is ImageSentViewModel -> {
                        consolidatedMessageItems.add(
                            MultipleImageSentViewModel(
                                firstMessage.name, firstMessage.sentTime
                            ).apply { imagesList.add(firstMessage) }
                        )
                    }
                    is ImageReceivedViewModel -> {
                        consolidatedMessageItems.add(
                            MultipleImageReceivedViewModel(
                                firstMessage.name, firstMessage.sentTime
                            ).apply { imagesList.add(firstMessage) }
                        )
                    }
                    else -> consolidatedMessageItems.add(firstMessage)
                }
            }

            if (messageItems.size >= 2) {
                for (message in messageItems.subList(1, messageItems.size)) {
                    consolidatedMessageItems.last().also { lastMessage ->
                        when (message) {
                            is ImageSentViewModel -> {
                                when (lastMessage) {
                                    is MultipleImageSentViewModel -> {
                                        lastMessage.imagesList.add(message)
                                        lastMessage.sentTime = message.sentTime
                                    }
                                    else -> {
                                        consolidatedMessageItems.add(
                                            MultipleImageSentViewModel(
                                                message.name,
                                                message.sentTime
                                            ).apply {
                                                imagesList.add(message)
                                            })
                                    }
                                }
                            }

                            is ImageReceivedViewModel -> {
                                when (lastMessage) {
                                    is MultipleImageReceivedViewModel -> {
                                        if (lastMessage.name == message.name) {
                                            lastMessage.imagesList.add(message)
                                            lastMessage.sentTime = message.sentTime
                                        } else {
                                            consolidatedMessageItems.add(
                                                MultipleImageReceivedViewModel(
                                                    message.name,
                                                    message.sentTime
                                                ).apply {
                                                    imagesList.add(message)
                                                }
                                            )
                                        }
                                    }
                                    else -> {
                                        consolidatedMessageItems.add(
                                            MultipleImageReceivedViewModel(
                                                message.name,
                                                message.sentTime
                                            ).apply {
                                                imagesList.add(message)
                                            }
                                        )
                                    }
                                }
                            }
                            else -> consolidatedMessageItems.add(message)
                        }
                    }
                }
            }
        }

        messagesAdapter.submitList(consolidatedMessageItems.toMutableList()) {
            if (messagesAdapter.itemCount <= 0) return@submitList
//            Log.d(TAG, "updateMessagesToRecyclerView: currentListSize${messagesAdapter.currentList.size}")
            isLastItemVisible?.let {
                if (it) {
                    chatLayoutManager.smoothScrollToPosition(
                        binding.chatRecyclerView,
                        null,
                        messagesAdapter.itemCount - 1
                    )
//                    binding.chatRecyclerView.smoothScrollToPosition(messagesAdapter.itemCount - 1)
                    Log.d(
                        TAG,
                        "updateMessagesToRecyclerView: afterSmoothScrollToPosition(${messagesAdapter.itemCount - 1}) - ${System.currentTimeMillis()}"
                    )
                } else {
//                    Log.d(TAG, "updateMessagesToRecyclerView: last item NOT visible")
                }
            }

            chatLayoutManager.postOnAnimation {

                val lastVisibleItemPosition = chatLayoutManager.findLastVisibleItemPosition()

                Log.d(
                    TAG,
                    "updateMessagesToRecyclerView: lastVisibleItemPosition: $lastVisibleItemPosition - ${System.currentTimeMillis()}"
                )

                run addReadTime@{
                    messagesAdapter.currentList.subList(0, lastVisibleItemPosition + 1).asReversed()
                        .forEach { messageItem ->
                            if (messageItem is TextReceivedViewModel || messageItem is MultipleImageReceivedViewModel) {
                                if (messageItem.readTime != null) return@addReadTime
                                messageItem.readTime = System.currentTimeMillis()
                            }
                        }
                }

// traverses through the list from reverse till a non null read time and add a badge of new received message count

                var _noOfUnReadMessages = 0
                run unReadMessages@{
                    messagesAdapter.currentList.asReversed().forEach { messageItem ->
                        messageItem?.let {
                            if (messageItem is TextReceivedViewModel || messageItem is MultipleImageReceivedViewModel) {
                                if (messageItem.readTime != null) return@unReadMessages
                                _noOfUnReadMessages++
                            }
                        }
                    }
                }
                noOfUnReadMessages = _noOfUnReadMessages
                Log.d(
                    TAG,
                    "updateMessagesToRecyclerView: _noOfUnReadMessages: $_noOfUnReadMessages"
                )
            }
        }
    }

    private fun onImageClicked(
        position: Int,
        itemViewModel: MessageItemViewModel,
        imageView: View
    ) {
        Log.d(TAG, "onImageClicked: $itemViewModel")
        when (itemViewModel) {
            is MultipleImageSentViewModel -> {
                StfalconImageViewer.Builder(
                    this,
//                    arrayListOf(itemViewModel.imagesList[position].originalBitmap)
                    itemViewModel.imagesList.map { it.originalBitmap }
                ) { view, image ->
                    view.setImageBitmap(image)
                }
                    .withStartPosition(position)
                    .withTransitionFrom(
                        when (imageView) {
                            is ImageView -> imageView
                            else -> null
                        }
                    )
                    .show()
            }
            is MultipleImageReceivedViewModel -> {
                StfalconImageViewer.Builder(
                    this,
                    itemViewModel.imagesList.map { it.originalBitmap }
                ) { view, image ->
                    view.setImageBitmap(image)
                }
                    .withStartPosition(position)
                    .withTransitionFrom(
                        when (imageView) {
                            is ImageView -> imageView
                            else -> null
                        }
                    ).show()
            }
        }
    }

    private fun getDateItem(sentTime: Long): MessageItemViewModel {
        return BroadcastViewModel(
            name = "",
            sentTime = sentTime,
            dayChangeMessage = getDateTextToDisplay(sentTime)
        )
    }

    private fun resetMessageEditText() {
        binding.messageEditText.text.clear()
    }

    override fun onResume() {
        super.onResume()
        this.registerReceiver(dateChangedReceiver, DateChangedReceiver.getIntentFilter())
    }

    override fun onPause() {
        super.onPause()
        this.unregisterReceiver(dateChangedReceiver)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        snackbar?.dismiss()
        super.onDestroy()
    }

    companion object {
        fun Int.dpToPx() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics
        ).toInt()
    }
}