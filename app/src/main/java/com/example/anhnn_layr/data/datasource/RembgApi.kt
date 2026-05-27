package com.example.anhnn_layr.data.datasource

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface RembgApi {
    @Multipart
    @POST("api/remove")
    suspend fun removeBackground(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("a") alphaMatting: RequestBody,
        @Part("af") foregroundThreshold: RequestBody,
        @Part("ab") backgroundThreshold: RequestBody,
        @Part("ae") erodeSize: RequestBody,
        @Part("om") onlyMask: RequestBody,
        @Part("ppm") postProcessMask: RequestBody,
        @Query("bgc") backgroundColor: String? = null,
    ): ResponseBody

    @Multipart
    @POST("api/upscale")
    suspend fun upscale(
        @Part file: MultipartBody.Part,
        @Query("model") model: String,
        @Query("outscale") outscale: Float,
        @Query("half") half: Boolean,
        @Query("tile") tile: Int,
        @Query("tile_pad") tilePad: Int,
        @Query("pre_pad") prePad: Int,
    ): Response<ResponseBody>
}
