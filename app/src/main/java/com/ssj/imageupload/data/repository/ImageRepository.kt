package com.ssj.imageupload.data.repository

import com.ssj.imageupload.data.remote.ApiService
import com.ssj.imageupload.data.remote.UploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ImageRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun uploadImage(imageFile: File): Result<UploadResponse> {
        return try {
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            val response = apiService.uploadImage(body)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}