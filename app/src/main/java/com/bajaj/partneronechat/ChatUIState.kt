package com.bajaj.partneronechat

sealed class ChatUIState {
  object Loading : ChatUIState()
  data class Error(val errorMessage: String?) : ChatUIState()
  data class Success(val successMessage: String?) : ChatUIState()
}
