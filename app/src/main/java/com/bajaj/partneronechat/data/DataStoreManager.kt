package com.bajaj.partneronechat.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val TAG = "DataStoreManager"
const val SOCKET_ADDRESS = "socket_address"
const val USER_NAME = "user_name"

private val Context.socketDataStore by preferencesDataStore(
    name = SOCKET_ADDRESS
)

private val Context.userNameDataStore by preferencesDataStore(
    name = USER_NAME
)


private val IP_ADDRESS = stringPreferencesKey("ip_address")
private val PORT = stringPreferencesKey("port")
private val USER_NAME_PREF_KEY = stringPreferencesKey("user_name")

class DataStoreManager @Inject constructor(@ApplicationContext context: Context) {

    init {
        Log.d(TAG, "Initialisation")
    }

    private val socketDataStore = context.socketDataStore
    private val userNameDataStore = context.userNameDataStore

    suspend fun setSocketAddress(socketAddress: SocketAddress) {
//        Log.d(TAG, "setSocketAddress: ${socketAddress.ipAddress} ${socketAddress.port}")
        socketDataStore.edit { socketData ->
            socketData[IP_ADDRESS] = socketAddress.ipAddress
            socketData[PORT] = socketAddress.port
        }
    }

    val socketAddress: Flow<SocketAddress> = socketDataStore.data.map { preferences ->
//        Log.d(TAG, "socketAddress read: ${preferences[IP_ADDRESS]} ${preferences[PORT]}]")
        SocketAddress(
            preferences[IP_ADDRESS] ?: "",
            preferences[PORT] ?: ""
        )
    }

    suspend fun setName(userName: String) {
        userNameDataStore.edit {
            it[USER_NAME_PREF_KEY] = userName
        }
    }

    val userName: Flow<String> = userNameDataStore.data.map { preferences ->
        preferences[USER_NAME_PREF_KEY] ?: ""
    }
}

data class SocketAddress(
    val ipAddress: String,
    val port: String
)