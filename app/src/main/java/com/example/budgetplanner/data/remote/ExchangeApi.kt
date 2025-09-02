package com.example.budgetplanner.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class ExchangeResponse(
    val success: Boolean? = null,
    val base: String? = null,
    val rates: Map<String, Double> = emptyMap()
)

interface ExchangeApi {
    @GET("latest")
    suspend fun latest(
        @Query("base") base: String = "EUR",
        @Query("symbols") symbols: String = "RON"
    ): ExchangeResponse

    @GET("nbrfxrates.xml")
    suspend fun bnrXml(): String
}
