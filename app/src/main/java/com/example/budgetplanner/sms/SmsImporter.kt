package com.example.budgetplanner.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.domain.model.Source
import com.example.budgetplanner.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

object SmsImporter {

    suspend fun importRecent(context: Context, days: Int = 7): Int = withContext(Dispatchers.IO) {
        val app = context.applicationContext as BudgetApplication
        val repo = app.repository
        val zone = ZoneId.systemDefault()

        val since = System.currentTimeMillis() - days * 24L * 3600_000L
        val uri = Telephony.Sms.Inbox.CONTENT_URI

        val proj = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.BODY,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE
        )

        val sel = "${Telephony.Sms.DATE} >= ?"
        val args = arrayOf(since.toString())
        var inserted = 0

        context.contentResolver.query(uri, proj, sel, args, "${Telephony.Sms.DATE} DESC")?.use { c ->
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iAddr = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (c.moveToNext()) {
                val body = c.getString(iBody) ?: continue
                val addr = c.getString(iAddr) ?: ""
                val whenMs = c.getLong(iDate)

                val parsed = SmsParsers.tryParse(body) ?: continue
                val tx = Transaction(
                    id = 0L,
                    timestamp = whenMs,
                    originalAmount = -parsed.amount,
                    originalCurrency = parsed.currency,
                    merchant = parsed.merchantCore,
                    note = parsed.merchantRaw,
                    category = Category.OTHER,
                    source = Source.SMS,
                    amountRon = if (parsed.currency == "RON") -parsed.amount else 0.0,
                    pending = false
                )
                runCatching { repo.addTransaction(tx) }.onSuccess { inserted++ }
            }
        }
        inserted
    }
}
