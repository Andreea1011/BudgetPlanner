package com.example.budgetplanner.data.receipt

import java.math.BigDecimal
import java.time.LocalDateTime

data class ParsedReceipt(
    val vendor: String?,
    val total: BigDecimal?,
    val date: LocalDateTime?,
    val rawText: String
)

object ReceiptParser {
    private val money = Regex("""(?<!\d)(\d{1,3}(?:[.,]\d{3})*[.,]\d{2})(?!\d)""")
    private val datePatterns = listOf(
        Regex("""\b(\d{2})\.(\d{2})\.(\d{4})(?:\s+(\d{2}):(\d{2}))?\b"""), // 03.09.2025 14:23
        Regex("""\b(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2}))?\b""")    // 2025-09-03 14:23
    )
    private val totalHints = listOf("TOTAL", "TOTAL DE PLATA", "TOTAL DE PLATĂ", "DE PLATA", "SUMA", "DATORAT")

    fun parse(ocrText: String): ParsedReceipt {
        val lines = ocrText.replace('\u00A0', ' ')
            .lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Vendor: first loud header line (ALL CAPS) or first line
        val vendor = lines.firstOrNull { it == it.uppercase() && it.length in 4..40 } ?: lines.firstOrNull()

        // Date
        val date = run {
            for (l in lines) for (p in datePatterns) {
                val m = p.find(l) ?: continue
                return@run try {
                    if (p === datePatterns[0]) {
                        val (dd, mm, yyyy, hh, mi) = m.destructured
                        LocalDateTime.of(yyyy.toInt(), mm.toInt(), dd.toInt(), hh.ifEmpty { "00" }.toInt(), mi.ifEmpty { "00" }.toInt())
                    } else {
                        val (yyyy, mm, dd, hh, mi) = m.destructured
                        LocalDateTime.of(yyyy.toInt(), mm.toInt(), dd.toInt(), hh.ifEmpty { "00" }.toInt(), mi.ifEmpty { "00" }.toInt())
                    }
                } catch (_: Throwable) { null }
            }
            null
        }

        // Totals (prefer lines with hints; fallback to max money on the receipt)
        val hinted = lines.filter { l -> totalHints.any { l.uppercase().contains(it) } }
            .flatMap { money.findAll(it).map { m -> parseMoney(m.groupValues[1]) } }
        val total = (hinted.ifEmpty {
            lines.flatMap { money.findAll(it).map { m -> parseMoney(m.groupValues[1]) } }
        }).maxOrNull()

        return ParsedReceipt(
            vendor = vendor?.replace(Regex("""\s{2,}"""), " ")?.trim(),
            total = total,
            date = date,
            rawText = ocrText
        )
    }

    private fun parseMoney(s: String) = run {
        val hasComma = s.contains(',')
        val hasDot = s.contains('.')
        val normalized = when {
            hasComma && hasDot -> { // decide by last separator as decimal
                val lastComma = s.lastIndexOf(',')
                val lastDot   = s.lastIndexOf('.')
                if (lastComma > lastDot) s.replace(".", "").replace(",", ".") else s.replace(",", "")
            }
            hasComma -> s.replace(",", ".")
            else -> s
        }
        normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }
}
