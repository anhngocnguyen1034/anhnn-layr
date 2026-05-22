package com.example.anhnn_layr.data.datasource

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface RembgApi {
    @Multipart
    @POST("api/remove")
    suspend fun removeBackground(
        @Part file: MultipartBody.Part,
        @Query("model") model: String = "u2net",
        @Query("a") alphaMatting: Boolean = false,
        @Query("af") foregroundThreshold: Int = 240,
        @Query("ab") backgroundThreshold: Int = 10,
        @Query("ae") erodeSize: Int = 10,
        @Query("om") onlyMask: Boolean = false,
        @Query("ppm") postProcessMask: Boolean = false,
        @Query("bgc") backgroundColor: String? = null,
    ): ResponseBody
}
