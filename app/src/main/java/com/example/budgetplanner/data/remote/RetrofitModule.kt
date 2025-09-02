package com.example.budgetplanner.data.remote

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitModule {
    val exchangeApi: ExchangeApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.exchangerate.host/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ExchangeApi::class.java)
    }

    val bnrApi: ExchangeApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.bnr.ro/")
            .addConverterFactory(ScalarsConverterFactory.create()) // returns raw String
            .build()
            .create(ExchangeApi::class.java)
    }
}
