package remix.myplayer.ui.state

sealed class DataUiState<out T> {

  data class Loading(val message: String = "") : DataUiState<Nothing>()

  data class Error(val throwable: Throwable) : DataUiState<Nothing>()

  data class Success<out T>(val data: T) : DataUiState<T>()

  fun get() = (this as Success).data

  fun isSuccess() = this is Success

  fun isError() = this is Error

  fun isLoading() = this is Loading
}