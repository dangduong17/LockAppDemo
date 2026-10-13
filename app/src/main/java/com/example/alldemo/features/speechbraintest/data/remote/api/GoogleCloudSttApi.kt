package com.example.alldemo.features.speechbraintest.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GoogleCloudSttApi {

    @POST("v1/speech:recognize")
    suspend fun recognizeSpeech(
        @Query("key") apiKey: String,
        @Body request: RecognizeRequest
    ): Response<RecognizeResponse>
}
