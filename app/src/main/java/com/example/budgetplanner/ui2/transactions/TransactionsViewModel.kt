package com.example.budgetplanner.ui2.transactions

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgetplanner.BudgetApplication
import com.example.budgetplanner.domain.model.Category
import com.example.budgetplanner.domain.model.Source
import com.example.budgetplanner.domain.model.Transaction
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ===== Receipt OCR + parse state =====

data class ReceiptDraft(
    var vendor: String,
    var totalRon: BigDecimal,
    var dateTime: LocalDateTime,
    val rawText: String
)

sealed interface ReceiptUiState {
    data object Hidden : ReceiptUiState
    data class Editing(val draft: ReceiptDraft, val vendorSuggestions: List<String>) : ReceiptUiState
    data object Saving : ReceiptUiState
}

// tiny, local parser tuned for RO receipts
private object ReceiptParser {
    private val money = Regex("""(?<!\d)(\d{1,3}(?:[.,]\d{3})*[.,]\d{2})(?!\d)""")
    private val totalHints = listOf("TOTAL", "TOTAL DE PLATA", "TOTAL DE PLATĂ", "DE PLATA", "SUMA", "DATORAT")
    private val datePatterns = listOf(
        Regex("""\b(\d{2})\.(\d{2})\.(\d{4})(?:\s+(\d{2}):(\d{2}))?\b"""), // 03.09.2025 14:23
        Regex("""\b(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2}))?\b""")    // 2025-09-03 14:23
    )

    fun parse(text: String, now: LocalDateTime): ReceiptDraft {
        val lines = text.replace('\u00A0', ' ')
            .lines().map { it.trim() }.filter { it.isNotEmpty() }

        val vendor = lines.firstOrNull { it == it.uppercase() && it.length in 4..40 }
            ?: lines.firstOrNull().orEmpty()

        val date = run {
            for (l in lines) for (p in datePatterns) {
                val m = p.find(l) ?: continue
                try {
                    if (p === datePatterns[0]) {
                        val (dd, mm, yyyy, hh, mi) = m.destructured
                        return@run LocalDateTime.of(
                            yyyy.toInt(), mm.toInt(), dd.toInt(),
                            hh.ifEmpty { "00" }.toInt(),
                            mi.ifEmpty { "00" }.toInt()
                        )
                    } else {
                        val (yyyy, mm, dd, hh, mi) = m.destructured
                        return@run LocalDateTime.of(
                            yyyy.toInt(), mm.toInt(), dd.toInt(),
                            hh.ifEmpty { "00" }.toInt(),
                            mi.ifEmpty { "00" }.toInt()
                        )
                    }
                } catch (_: Throwable) { /* ignore */ }
            }
            null
        } ?: now

        val hinted = lines.filter { l -> totalHints.any { l.uppercase().contains(it) } }
            .flatMap { money.findAll(it).map { m -> normalizeMoney(m.groupValues[1]) } }

        val total = (hinted.ifEmpty {
            lines.flatMap { money.findAll(it).map { m -> normalizeMoney(m.groupValues[1]) } }
        }).maxOrNull() ?: BigDecimal.ZERO

        return ReceiptDraft(
            vendor = vendor,
            totalRon = total,
            dateTime = date,
            rawText = text
        )
    }

    private fun normalizeMoney(s: String): BigDecimal {
        val hasComma = s.contains(',')
        val hasDot = s.contains('.')
        val normalized = when {
            hasComma && hasDot -> {
                val lastComma = s.lastIndexOf(',')
                val lastDot = s.lastIndexOf('.')
                if (lastComma > lastDot) s.replace(".", "").replace(",", ".")
                else s.replace(",", "")
            }
            hasComma -> s.replace(",", ".")
            else -> s
        }
        return normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }
}

// ===== existing UI state =====

data class TxRowUi(val tx: Transaction)
data class DayGroup(val date: LocalDate, val items: List<TxRowUi>)
data class TransactionsUiState(
    val month: YearMonth = YearMonth.now(),
    val rows: List<DayGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selected: Transaction? = null,

    // NEW:
    val expenseCoverage: ExpenseCoverage? = null,
    val creditAllocation: CreditAllocation? = null,

    val totalPersonalSpend: Double = 0.0,
    val totalOpenForMom: Double = 0.0,

    // Receipt editor UI
    val receipt: ReceiptUiState = ReceiptUiState.Hidden
)

class TransactionsViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo get() = (app as BudgetApplication).repository
    private val zone: ZoneId = ZoneId.systemDefault()

    private val _ui = MutableStateFlow(TransactionsUiState())
    val ui: StateFlow<TransactionsUiState> = _ui.asStateFlow()

    private var monthJob: kotlinx.coroutines.Job? = null

    init { selectMonth(_ui.value.month) }

    fun prevMonth() = selectMonth(_ui.value.month.minusMonths(1))
    fun nextMonth() = selectMonth(_ui.value.month.plusMonths(1))

    fun sync() = viewModelScope.launch {
        _ui.update { it.copy(isLoading = true, error = null) }
        val m = _ui.value.month
        val res = repo.syncMonthFromBank(m)
        runCatching { repo.applyRulesToMonth(m, zone) }

        val personal = repo.personalSpendForMonth(m, zone)
        val openMom  = repo.openForMomForMonth(m, zone)

        _ui.update {
            it.copy(
                isLoading = false,
                error = res.exceptionOrNull()?.message,
                totalPersonalSpend = personal,
                totalOpenForMom = openMom
            )
        }
    }

    private fun selectMonth(m: YearMonth) {
        _ui.update { it.copy(month = m, isLoading = true, error = null) }
        monthJob?.cancel()
        monthJob = viewModelScope.launch {
            try {
                // list
                launch {
                    repo.observeMonth(m, zone).collectLatest { list ->
                        _ui.update { it.copy(rows = groupForUi(list), isLoading = false) }
                    }
                }
                // totals
                launch {
                    val personal = repo.personalSpendForMonth(m, zone)
                    val openMom = repo.openForMomForMonth(m, zone)
                    _ui.update { it.copy(totalPersonalSpend = personal, totalOpenForMom = openMom) }
                }
            } catch (t: Throwable) {
                _ui.update { it.copy(isLoading = false, error = t.message) }
            }
        }
    }

    private fun groupForUi(list: List<Transaction>): List<DayGroup> {
        val byDate = list.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        return byDate.toSortedMap(compareByDescending { it }).map { (date, txs) ->
            DayGroup(date, txs.map { TxRowUi(it) })
        }
    }

    // ---------- row actions ----------
    fun onRowClick(t: Transaction) {
        _ui.update { it.copy(selected = t, expenseCoverage = null, creditAllocation = null) }
        viewModelScope.launch {
            if (t.originalAmount < 0) {
                val total = kotlin.math.abs(t.amountRon)
                val covered = repo.sumCoveredForExpense(t.id)
                _ui.update { it.copy(expenseCoverage = ExpenseCoverage(total, covered)) }
            } else if (t.originalAmount > 0) {
                val allocated = repo.sumAllocatedFromCredit(t.id)
                val links = repo.getLinksForCredit(t.id)
                val surplus = (t.amountRon - allocated).coerceAtLeast(0.0)
                _ui.update { it.copy(creditAllocation = CreditAllocation(allocated, links.size, surplus)) }
            }
        }
    }

    fun dismissSheet() { _ui.update { it.copy(selected = null) } }

    fun deleteSelected() {
        val t = _ui.value.selected ?: return
        viewModelScope.launch {
            repo.deleteTransactionById(t.id)
            dismissSheet()
        }
    }

    fun saveSelected(note: String?, category: Category) {
        val t = _ui.value.selected ?: return
        viewModelScope.launch {
            repo.updateTransactionNoteAndCategory(t.id, note, category.name)
            dismissSheet()
        }
    }

    // ---------- debug: seed a few rows ----------
    fun seedDemo() = viewModelScope.launch {
        val now = LocalDate.now()
        suspend fun add(dayOffset: Long, amount: Double, cur: String, merchant: String, cat: Category) {
            val ts = now.minusDays(dayOffset).atStartOfDay(zone).toInstant().toEpochMilli()
            val tx = Transaction(
                id = 0L,
                timestamp = ts,
                originalAmount = amount,
                originalCurrency = cur,
                merchant = merchant,
                note = null,
                category = cat,
                source = Source.MANUAL,
                amountRon = amount,
                pending = false
            )
            repo.addTransaction(tx)
        }
        add(0, -49.99, "RON", "MEGA IMAGE", Category.FOOD)
        add(1, -12.50, "RON", "STB TICKET", Category.TRANSPORT)
        add(2, +5000.00, "RON", "Salary", Category.OTHER)
        add(5, -83.40, "RON", "Decathlon", Category.DORINTE)
        add(7, -350.00, "EUR", "Rent", Category.APARTMENT)
    }

    fun formatSigned(a: Double, cur: String): String {
        val sign = if (a < 0) "-" else "+"
        val abs = kotlin.math.abs(a)
        return "$sign${"%.2f".format(Locale.getDefault(), abs)} $cur"
    }

    fun formatHeader(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("dd MMMM, EEEE", Locale.getDefault()))

    fun seedMomScenario() = viewModelScope.launch {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()

        suspend fun add(d: Int, amt: Double, merch: String, note: String? = null) {
            val ts = today.atStartOfDay(zone).plusDays(d.toLong())
                .toInstant().toEpochMilli()
            val tx = com.example.budgetplanner.domain.model.Transaction(
                id = 0L,
                timestamp = ts,
                originalAmount = amt,           // negative = expense, positive = credit
                originalCurrency = "RON",
                merchant = merch,
                note = note,
                category = com.example.budgetplanner.domain.model.Category.FOOD,
                source = com.example.budgetplanner.domain.model.Source.MANUAL,
                amountRon = amt,                // keep 1:1 for testing
                pending = false
            )
            repo.addTransaction(tx)
        }

        // 2 grocery expenses (for Mom)
        add(0, -40.00, "Carrefour Rahova", "groceries")
        add(0, -25.50, "Mega Image", "groceries")

        // one of your own expenses (should stay in personal spend)
        add(0, -19.99, "McDonalds", "my lunch")

        // Mom reimburses later the same day with a little extra (+70)
        add(0, +70.00, "Transfer", "from mom")
    }

    fun dumpMomLinksToLog() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val from = now - 30L * 24 * 3600 * 1000
        val credits = repo.getMomCredits(from, now)         // ✅ wrapper
        credits.forEach { c ->
            val links = repo.getLinksForCredit(c.id)        // ✅ wrapper
            android.util.Log.d("MOM", "credit id=${c.id}, amt=${c.amountRon}, links=$links")
        }
    }

    fun setExcludePersonal(id: Long, exclude: Boolean) = viewModelScope.launch {
        val t = ui.value.selected ?: return@launch
        if (t.originalAmount >= 0) return@launch

        repo.setExcludePersonal(id, exclude)
        if (exclude) {
            runCatching { repo.applyMomReimbursementForExpense(id) }
        }
        refreshMonthTotals()
    }

    fun setParty(id: Long, party: String?) = viewModelScope.launch {
        repo.setParty(id, party)
        if (party == "MOM") {
            runCatching { repo.markCreditFromMom(id) }
            refreshMonthTotals()
        }
    }

    private suspend fun refreshMonthTotals() {
        val m = _ui.value.month
        val personal = repo.personalSpendForMonth(m, zone)
        val openMom  = repo.openForMomForMonth(m, zone)
        _ui.update { it.copy(totalPersonalSpend = personal, totalOpenForMom = openMom) }
    }

    fun autoLabel() = viewModelScope.launch {
        val zone = ZoneId.systemDefault()
        repo.applyRulesToMonth(_ui.value.month, zone)
    }

    // ===== RECEIPT: OCR entrypoint called by UI after Photo Picker =====

    fun onImagePicked(uri: Uri) = viewModelScope.launch {
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val img = InputImage.fromFilePath(app, uri)
            val text = recognizer.process(img).await().text

            val now = LocalDateTime.now()
            val draft = ReceiptParser.parse(text, now)

            // get your predefined vendors list from the repository
            val suggestions = runCatching { repo.vendorSuggestions() }.getOrDefault(emptyList())

            _ui.update { it.copy(receipt = ReceiptUiState.Editing(draft, suggestions)) }
        } catch (t: Throwable) {
            _ui.update { it.copy(error = t.message) }
        }
    }

    fun dismissReceipt() {
        _ui.update { it.copy(receipt = ReceiptUiState.Hidden) }
    }

    fun saveReceipt(d: ReceiptDraft) = viewModelScope.launch {
        _ui.update { it.copy(receipt = ReceiptUiState.Saving) }

        // Map draft -> your domain Transaction (expense = negative)
        val ts = d.dateTime.atZone(zone).toInstant().toEpochMilli()
        val amountRon = d.totalRon.toDouble() * -1.0

        val tx = Transaction(
            id = 0L,
            timestamp = ts,
            originalAmount = amountRon,       // negative expense
            originalCurrency = "RON",
            merchant = d.vendor.ifBlank { "Unknown" },
            note = null,                      // you can store d.rawText if you want
            category = Category.OTHER,        // or guess by vendor if you prefer
            source = try {
                // if you have a specific enum, use it; else fall back to MANUAL
                Source.valueOf("RECEIPT_OCR")
            } catch (_: Throwable) { Source.MANUAL },
            amountRon = amountRon,
            pending = false
        )

        runCatching { repo.addTransaction(tx) }
        _ui.update { it.copy(receipt = ReceiptUiState.Hidden) }
    }
}

data class ExpenseCoverage(val total: Double, val covered: Double) {
    val open: Double get() = (total - covered).coerceAtLeast(0.0)
}
data class CreditAllocation(val allocated: Double, val items: Int, val surplus: Double)

class TransactionsVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionsViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown VM class")
    }
}
