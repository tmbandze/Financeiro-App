package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyBudgetEntity
import com.example.data.model.PaymentMethod
import com.example.data.repository.ExpenseRepository
import com.example.utils.Formatters
import com.example.utils.ParsedReceipt
import com.example.utils.SmsReceiptParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategorySpending(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

data class DailySpending(
    val day: Int,
    val amount: Double,
    val formattedDate: String
)

data class MonthlyTrend(
    val monthKey: String,
    val shortName: String,
    val totalAmount: Double,
    val isSelected: Boolean
)

data class PlatformSpendingItem(
    val account: AccountEntity,
    val totalSpentMonth: Double,
    val transactionCount: Int,
    val percentageOfMonth: Float
)

data class MonthlyOverview(
    val monthKey: String,
    val monthDisplay: String,
    val totalSpent: Double,
    val budgetLimit: Double,
    val budgetRemaining: Double,
    val budgetPercentUsed: Float,
    val dailyAverage: Double,
    val categoryBreakdown: List<CategorySpending>,
    val dailySpending: List<DailySpending>,
    val monthlyTrends: List<MonthlyTrend>,
    val highestCategory: CategorySpending?,
    val biggestExpense: ExpenseEntity?,
    val previousMonthTotal: Double,
    val comparisonWithPrevMonthPercent: Double?
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseRepository

    private val _selectedMonthKey = MutableStateFlow(Formatters.currentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCategory = MutableStateFlow<ExpenseCategory?>(null)
    val filterCategory: StateFlow<ExpenseCategory?> = _filterCategory.asStateFlow()

    private val _filterAccountId = MutableStateFlow<String?>(null)
    val filterAccountId: StateFlow<String?> = _filterAccountId.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(db.expenseDao(), db.budgetDao(), db.accountDao())
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = accounts.map { list ->
        list.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val monthlyExpenses: StateFlow<List<ExpenseEntity>> = _selectedMonthKey
        .flatMapLatest { monthKey -> repository.getExpensesForMonth(monthKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentBudget: StateFlow<MonthlyBudgetEntity?> = _selectedMonthKey
        .flatMapLatest { monthKey -> repository.getBudgetForMonth(monthKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Platform Spending Breakdown for the selected month (M-Pesa, e-Mola, Bank, etc.)
    val platformSpending: StateFlow<List<PlatformSpendingItem>> = combine(
        accounts,
        monthlyExpenses
    ) { accList, expenses ->
        val totalMonth = expenses.sumOf { it.amount }
        accList.map { acc ->
            val accExpenses = expenses.filter { it.accountId == acc.id }
            val spent = accExpenses.sumOf { it.amount }
            val pct = if (totalMonth > 0) (spent / totalMonth).toFloat() else 0f
            PlatformSpendingItem(
                account = acc,
                totalSpentMonth = spent,
                transactionCount = accExpenses.size,
                percentageOfMonth = pct
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered expenses for the transaction list
    val filteredExpenses: StateFlow<List<ExpenseEntity>> = combine(
        monthlyExpenses,
        _searchQuery,
        _filterCategory,
        _filterAccountId
    ) { expenses, query, catFilter, accFilter ->
        expenses.filter { expense ->
            val matchesCategory = (catFilter == null) || (expense.categoryId == catFilter.id)
            val matchesAccount = (accFilter == null) || (expense.accountId == accFilter)
            val matchesQuery = query.isBlank() ||
                expense.title.contains(query, ignoreCase = true) ||
                expense.notes.contains(query, ignoreCase = true) ||
                expense.paymentMethod.contains(query, ignoreCase = true) ||
                (expense.accountName?.contains(query, ignoreCase = true) == true) ||
                (expense.platformTransactionId?.contains(query, ignoreCase = true) == true)
            matchesCategory && matchesAccount && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Complete calculated overview for the dashboard and charts
    val monthlyOverview: StateFlow<MonthlyOverview> = combine(
        _selectedMonthKey,
        monthlyExpenses,
        currentBudget,
        allExpenses
    ) { monthKey, expenses, budget, allList ->
        val totalSpent = expenses.sumOf { it.amount }
        val budgetLimit = budget?.budgetLimit ?: 15000.0
        val budgetRemaining = (budgetLimit - totalSpent).coerceAtLeast(0.0)
        val budgetPercent = if (budgetLimit > 0) ((totalSpent / budgetLimit).toFloat()) else 0f

        val daysInMonth = Formatters.getDaysInMonth(monthKey)
        val dailyAverage = if (daysInMonth > 0) totalSpent / daysInMonth else 0.0

        // Category breakdown
        val categoryBreakdown = expenses
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val sum = list.sumOf { it.amount }
                val pct = if (totalSpent > 0) (sum / totalSpent).toFloat() else 0f
                CategorySpending(
                    category = ExpenseCategory.fromId(catId),
                    totalAmount = sum,
                    percentage = pct,
                    count = list.size
                )
            }
            .sortedByDescending { it.totalAmount }

        val highestCategory = categoryBreakdown.firstOrNull()
        val biggestExpense = expenses.maxByOrNull { it.amount }

        // Daily spending distribution
        val dailyGroups = expenses.groupBy { Formatters.getDayOfMonth(it.dateMillis) }
        val dailySpendingList = (1..daysInMonth).map { day ->
            val listForDay = dailyGroups[day] ?: emptyList()
            DailySpending(
                day = day,
                amount = listForDay.sumOf { it.amount },
                formattedDate = "$day/${monthKey.substringAfter("-")}"
            )
        }

        // Monthly trends: last 6 months
        val last6MonthKeys = Formatters.getLastNMonthKeys(6, monthKey)
        val allExpensesByMonth = allList.groupBy { it.monthKey }
        val monthlyTrends = last6MonthKeys.map { mKey ->
            val sum = allExpensesByMonth[mKey]?.sumOf { it.amount } ?: 0.0
            MonthlyTrend(
                monthKey = mKey,
                shortName = Formatters.formatMonthKeyShort(mKey),
                totalAmount = sum,
                isSelected = (mKey == monthKey)
            )
        }

        // Previous month comparison
        val prevMonthKey = Formatters.getAdjacentMonthKey(monthKey, -1)
        val prevMonthExpenses = allExpensesByMonth[prevMonthKey] ?: emptyList()
        val prevTotal = prevMonthExpenses.sumOf { it.amount }

        val comparisonPercent = if (prevTotal > 0) {
            ((totalSpent - prevTotal) / prevTotal) * 100.0
        } else null

        MonthlyOverview(
            monthKey = monthKey,
            monthDisplay = Formatters.formatMonthKeyDisplay(monthKey),
            totalSpent = totalSpent,
            budgetLimit = budgetLimit,
            budgetRemaining = budgetRemaining,
            budgetPercentUsed = budgetPercent,
            dailyAverage = dailyAverage,
            categoryBreakdown = categoryBreakdown,
            dailySpending = dailySpendingList,
            monthlyTrends = monthlyTrends,
            highestCategory = highestCategory,
            biggestExpense = biggestExpense,
            previousMonthTotal = prevTotal,
            comparisonWithPrevMonthPercent = comparisonPercent
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyOverview(
            monthKey = Formatters.currentMonthKey(),
            monthDisplay = Formatters.formatMonthKeyDisplay(Formatters.currentMonthKey()),
            totalSpent = 0.0,
            budgetLimit = 15000.0,
            budgetRemaining = 15000.0,
            budgetPercentUsed = 0f,
            dailyAverage = 0.0,
            categoryBreakdown = emptyList(),
            dailySpending = emptyList(),
            monthlyTrends = emptyList(),
            highestCategory = null,
            biggestExpense = null,
            previousMonthTotal = 0.0,
            comparisonWithPrevMonthPercent = null
        )
    )

    // Month Navigation
    fun nextMonth() {
        _selectedMonthKey.value = Formatters.getAdjacentMonthKey(_selectedMonthKey.value, 1)
    }

    fun prevMonth() {
        _selectedMonthKey.value = Formatters.getAdjacentMonthKey(_selectedMonthKey.value, -1)
    }

    fun selectMonth(monthKey: String) {
        _selectedMonthKey.value = monthKey
    }

    // Filters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterCategory(category: ExpenseCategory?) {
        _filterCategory.value = category
    }

    fun setFilterAccountId(accountId: String?) {
        _filterAccountId.value = accountId
    }

    // CRUD Actions
    fun addExpense(
        title: String,
        amount: Double,
        categoryId: String,
        paymentMethod: String,
        dateMillis: Long = System.currentTimeMillis(),
        notes: String = "",
        accountId: String? = null,
        platformTransactionId: String? = null
    ) {
        viewModelScope.launch {
            val monthKey = Formatters.toMonthKey(dateMillis)
            val accountName = accountId?.let { repository.getAccountById(it)?.name }

            val expense = ExpenseEntity(
                title = title.trim(),
                amount = amount,
                categoryId = categoryId,
                paymentMethod = paymentMethod,
                dateMillis = dateMillis,
                monthKey = monthKey,
                notes = notes.trim(),
                accountId = accountId,
                accountName = accountName,
                platformTransactionId = platformTransactionId?.trim()
            )
            repository.insertExpense(expense)
        }
    }

    fun updateExpense(expense: ExpenseEntity, oldExpense: ExpenseEntity? = null) {
        viewModelScope.launch {
            val accountName = expense.accountId?.let { repository.getAccountById(it)?.name }
            val updated = expense.copy(
                monthKey = Formatters.toMonthKey(expense.dateMillis),
                accountName = accountName
            )
            repository.updateExpense(updated, oldExpense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun updateBudgetLimit(amount: Double) {
        viewModelScope.launch {
            repository.setBudget(_selectedMonthKey.value, amount)
        }
    }

    // Account & Balance Operations
    fun updateAccountBalance(accountId: String, newBalance: Double) {
        viewModelScope.launch {
            repository.updateAccountBalance(accountId, newBalance)
        }
    }

    fun addAccount(
        name: String,
        type: String,
        balance: Double,
        accountNumber: String = "",
        apiKey: String? = null
    ) {
        viewModelScope.launch {
            val newAcc = AccountEntity(
                name = name.trim(),
                type = type,
                balance = balance,
                accountNumber = accountNumber.trim(),
                apiKey = apiKey?.trim(),
                lastSyncMillis = System.currentTimeMillis()
            )
            repository.insertAccount(newAcc)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun syncAccount(accountId: String) {
        viewModelScope.launch {
            val acc = repository.getAccountById(accountId) ?: return@launch
            repository.updateAccount(acc.copy(lastSyncMillis = System.currentTimeMillis()))
        }
    }

    // Parse and apply SMS Receipt from M-Pesa, e-Mola, or Bank
    fun parseAndApplyReceipt(smsText: String): ParsedReceipt? {
        val parsed = SmsReceiptParser.parse(smsText) ?: return null

        viewModelScope.launch {
            // Find or associate account
            val matchingAccount = accounts.value.firstOrNull { acc ->
                acc.type.equals(parsed.platform.id, ignoreCase = true)
            } ?: accounts.value.firstOrNull()

            val paymentMethod = when (parsed.platform) {
                AccountType.MPESA -> PaymentMethod.MPESA.displayName
                AccountType.EMOLA -> PaymentMethod.EMOLA.displayName
                AccountType.BANK -> PaymentMethod.TRANSFERENCIA.displayName
                else -> PaymentMethod.OUTRO.displayName
            }

            val expense = ExpenseEntity(
                title = parsed.title,
                amount = parsed.amount,
                categoryId = parsed.suggestedCategory.id,
                paymentMethod = paymentMethod,
                dateMillis = System.currentTimeMillis(),
                monthKey = Formatters.currentMonthKey(),
                notes = "Importado via SMS: ${parsed.platform.brandName}",
                accountId = matchingAccount?.id,
                accountName = matchingAccount?.name,
                platformTransactionId = parsed.transactionId
            )

            repository.insertExpense(expense)

            // If the SMS explicitly had the new balance, update to that exact balance
            if (parsed.newBalance != null && matchingAccount != null) {
                repository.updateAccountBalance(matchingAccount.id, parsed.newBalance)
            }
        }

        return parsed
    }
}
