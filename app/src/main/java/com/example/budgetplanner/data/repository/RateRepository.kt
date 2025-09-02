package com.example.budgetplanner.data.repository

import com.example.budgetplanner.data.remote.RetrofitModule
import com.example.budgetplanner.data.settings.RateStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RateRepository(
    private val store: RateStore
) {
    // Prefer BNR official fix → fallback to exchangerate.host → fallback to cache
    suspend fun latestEurRon(): Result<Double> {
        // 1) BNR (exact daily fix)
        runCatching {
            val xml = RetrofitModule.bnrApi.bnrXml()
            parseEurFromBnr(xml)
        }.onSuccess { rate ->
            store.save(rate, System.currentTimeMillis())
            return Result.success(rate)
        }

        // 2) Exchangerate.host fallback
        runCatching {
            val resp = RetrofitModule.exchangeApi.latest()
            resp.rates["RON"] ?: error("RON not found")
        }.onSuccess { rate ->
            store.save(rate, System.currentTimeMillis())
            return Result.success(rate)
        }

        // 3) Cache fallback
        val (cached, _) = store.flow.first()
        return if (cached > 0.0) Result.success(cached)
        else Result.failure(IllegalStateException("No EUR→RON rate available"))
    }

    fun rateFlow(): Flow<Double> =
        store.flow.map { (rate, _) -> rate }

    private fun parseEurFromBnr(xml: String): Double {
        // Look for: Rate currency="EUR">5.07...</Rate>
        val key = "Rate currency=\"EUR\">"
        val i = xml.indexOf(key)
        require(i >= 0) { "EUR not found in BNR XML" }
        val start = i + key.length
        val end = xml.indexOf('<', start)
        val raw = xml.substring(start, end).trim()
        return raw.replace(',', '.').toDouble()
    }
}
