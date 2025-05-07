package com.ssj.imageupload

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssj.imageupload.data.remote.UploadResponse
import com.ssj.imageupload.data.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: ImageRepository) : ViewModel() {
    private val _uploadState = MutableLiveData<UploadState>()
    val uploadState: LiveData<UploadState> = _uploadState

    fun uploadImage(imageFile: File) {
        _uploadState.value = UploadState.Loading
        viewModelScope.launch {
            repository.uploadImage(imageFile).fold(
                onSuccess = { response ->
                    _uploadState.value = UploadState.Success(response)
                },
                onFailure = { error ->
                    _uploadState.value = UploadState.Error(error.message ?: "Upload failed")
                }
            )
        }
    }
}

sealed class UploadState {
    object Loading : UploadState()
    data class Success(val response: UploadResponse) : UploadState()
    data class Error(val message: String) : UploadState()
}