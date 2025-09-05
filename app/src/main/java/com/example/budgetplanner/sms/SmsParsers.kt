package com.example.budgetplanner.sms

data class ParsedTxn(
    val amount: Double,
    val currency: String,
    val merchantRaw: String,   // full string after “Comerciant:”
    val merchantCore: String,  // normalized first word, e.g. “PENNY”
)

object SmsParsers {

    // Example BT POS format:
    // "Tranz POS. Suma 53.96 RON. Card nr. ***3211, MONICA ELENA LITA. 29.08.25 14:47.
    //  Suma disponibila: 7963.03 RON. Comerciant: PENNY 4562 RM VL2 C3, RO,RAMNICU VALC"
    private val POS_REGEX = Regex(
        pattern = """Tranz\s+POS\.\s*Suma\s+([\d.,]+)\s+(RON|EUR|USD)\.[\s\S]*?Comerciant:\s*([^\n\r]+)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun tryParse(body: String): ParsedTxn? {
        val m = POS_REGEX.find(body) ?: return null
        val amountRaw = m.groupValues[1].replace(',', '.')
        val amount = amountRaw.toDoubleOrNull() ?: return null
        val currency = m.groupValues[2].uppercase()
        val merchantRaw = m.groupValues[3].trim()

        // “PENNY 4562 RM VL2 C3, RO,RAMNICU VALC” → core “PENNY”
        val merchantCore = merchantRaw
            .split(',', limit = 2)[0]   // drop country/city tail
            .trim()
            .split(' ')
            .firstOrNull()
            ?.uppercase()
            ?: merchantRaw.uppercase()

        return ParsedTxn(
            amount = amount,
            currency = currency,
            merchantRaw = merchantRaw,
            merchantCore = merchantCore
        )
    }
}
