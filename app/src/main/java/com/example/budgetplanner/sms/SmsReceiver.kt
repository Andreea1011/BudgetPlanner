package com.example.budgetplanner.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.domain.model.Source
import com.example.budgetplanner.domain.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) return

        val extras: Bundle = intent.extras ?: return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val body = msgs.joinToString(separator = "") { it.displayMessageBody ?: "" }
        val address = msgs.firstOrNull()?.displayOriginatingAddress ?: ""

        // Optional sender filter (BT often uses short codes / branded IDs)
        // if (!address.contains("BT", ignoreCase = true)) return

        val parsed = SmsParsers.tryParse(body) ?: return

        val app = context.applicationContext as BudgetApplication
        val repo = app.repository
        val zone = ZoneId.systemDefault()

        CoroutineScope(Dispatchers.IO).launch {
            // Create an expense (negative)
            val tx = Transaction(
                id = 0L,
                timestamp = System.currentTimeMillis(), // SMS timestamp available from PDU too if you prefer
                originalAmount = -parsed.amount,        // negative
                originalCurrency = parsed.currency,
                merchant = parsed.merchantCore,         // short, clean name (e.g., PENNY)
                note = parsed.merchantRaw,              // keep full string in note
                category = Category.OTHER,              // will be auto-labeled by rules
                source = Source.SMS,
                amountRon = 0.0,                        // let a normalizer set this if currency != RON
                pending = false
            )
            // simple RON normalization for now
            val finalTx = if (parsed.currency == "RON") tx.copy(amountRon = tx.originalAmount) else tx
            runCatching { repo.addTransaction(finalTx) }
        }
    }
}