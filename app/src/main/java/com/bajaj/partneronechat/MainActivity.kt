package com.bajaj.partneronechat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bajaj.partneronechat.data.DataStoreManager
import com.bajaj.partneronechat.data.SocketAddress
import com.bajaj.partneronechat.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

const val EXTRA_NAME = "name"
private const val TAG = "MainActivity"

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    @Inject
    lateinit var dataStore: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityBinding = ActivityMainBinding.inflate(layoutInflater)
        binding = activityBinding
        CoroutineScope(Dispatchers.Main).launch {
            loadIpAndPort()
            loadUserName()
        }
        setContentView(activityBinding.root)
        setUpListeners()
    }

    private fun setUpListeners() {
        binding.enterRoomButton.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                saveSocketAddress()
                saveName()
            }
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra(EXTRA_NAME, binding.nameTextField.editText?.text.toString())
            startActivity(intent)
        }
    }

    private suspend fun saveSocketAddress() {
        dataStore.setSocketAddress(
            SocketAddress(
                binding.ipTextField.editText?.text.toString(),
                binding.portTextField.editText?.text.toString()
            )
        )
    }

    private suspend fun saveName() {
        dataStore.setName(binding.nameTextField.editText?.text.toString())
    }

    private suspend fun loadIpAndPort() {
//        Log.d(TAG, "loadIpAndPort: ")
        dataStore.socketAddress.first().apply {
            binding.ipTextField.editText?.setText(this.ipAddress)
            binding.portTextField.editText?.setText(this.port)
        }
    }

    private suspend fun loadUserName() {
        dataStore.userName.first().apply {
            binding.nameTextField.editText?.setText(this)
        }
    }
}