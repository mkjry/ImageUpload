package com.ssj.imageupload.data.model

data class ImgBBResponse(
    val data: ImgBBData?,
    val success: Boolean,
    val status: Int,
    val error: String?
)

data class ImgBBData(
    val url: String,
    val title: String
)