package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class MpesaBalanceRequest(
    @Json(name = "input_QueryReference") val queryReference: String,
    @Json(name = "input_ServiceProviderCode") val serviceProviderCode: String,
    @Json(name = "input_ThirdPartyConversationID") val thirdPartyConversationId: String
)

@JsonClass(generateAdapter = true)
data class MpesaApiResponse(
    @Json(name = "output_ResponseCode") val responseCode: String,
    @Json(name = "output_ResponseDesc") val responseDesc: String,
    @Json(name = "output_TransactionID") val transactionId: String? = null,
    @Json(name = "output_ConversationID") val conversationId: String? = null,
    @Json(name = "output_ThirdPartyConversationID") val thirdPartyConversationId: String? = null
)

interface MpesaApiService {
    @Headers("Content-Type: application/json", "Origin: developer.mpesa.vm.co.mz")
    @POST("ipg/v2/vodacomMZ/queryTransactionStatus/")
    suspend fun queryTransactionStatus(
        @Header("Authorization") bearerToken: String,
        @Body request: MpesaBalanceRequest
    ): MpesaApiResponse

    companion object {
        private const val BASE_URL = "https://api.sandbox.vm.co.mz:18352/"

        fun create(): MpesaApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(MpesaApiService::class.java)
        }
    }
}
