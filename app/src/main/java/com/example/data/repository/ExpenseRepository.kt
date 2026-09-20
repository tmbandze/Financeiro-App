package com.example.data.repository

import com.example.data.db.AccountDao
import com.example.data.db.BudgetDao
import com.example.data.db.ExpenseDao
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthlyBudgetEntity
import com.example.data.model.PaymentMethod
import com.example.utils.Formatters
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val accountDao: AccountDao
) {
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesByMonth(monthKey)
    }

    fun getExpensesByAccount(accountId: String): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesByAccount(accountId)
    }

    fun getExpensesByAccountAndMonth(accountId: String, monthKey: String): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesByAccountAndMonth(accountId, monthKey)
    }

    fun getBudgetForMonth(monthKey: String): Flow<MonthlyBudgetEntity?> {
        return budgetDao.getBudgetForMonth(monthKey)
    }

    suspend fun setBudget(monthKey: String, limit: Double) {
        budgetDao.setBudget(MonthlyBudgetEntity(monthKey = monthKey, budgetLimit = limit))
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        val id = expenseDao.insert(expense)
        // Deduct from account balance if linked
        expense.accountId?.let { accId ->
            accountDao.deductBalance(accId, expense.amount)
        }
        return id
    }

    suspend fun updateExpense(newExpense: ExpenseEntity, oldExpense: ExpenseEntity? = null) {
        expenseDao.update(newExpense)
        if (oldExpense != null && oldExpense.accountId == newExpense.accountId) {
            val delta = newExpense.amount - oldExpense.amount
            newExpense.accountId?.let { accId ->
                accountDao.deductBalance(accId, delta)
            }
        } else {
            // Revert old account if changed
            oldExpense?.accountId?.let { oldAccId ->
                accountDao.addBalance(oldAccId, oldExpense.amount)
            }
            // Deduct from new account
            newExpense.accountId?.let { newAccId ->
                accountDao.deductBalance(newAccId, newExpense.amount)
            }
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.delete(expense)
        // Restore account balance if linked
        expense.accountId?.let { accId ->
            accountDao.addBalance(accId, expense.amount)
        }
    }

    suspend fun deleteExpenseById(id: Long) {
        val expense = expenseDao.getExpenseById(id)
        if (expense != null) {
            deleteExpense(expense)
        } else {
            expenseDao.deleteById(id)
        }
    }

    // Account Management
    suspend fun insertAccount(account: AccountEntity) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }

    suspend fun updateAccountBalance(accountId: String, newBalance: Double) {
        accountDao.updateBalance(accountId, newBalance)
    }

    suspend fun getAccountById(accountId: String): AccountEntity? {
        return accountDao.getAccountById(accountId)
    }

    suspend fun checkAndSeedInitialData() {
        // Seed default Accounts (M-Pesa, e-Mola, Bank) if none exist
        val mpesaId = "acc-mpesa-vodacom"
        val emolaId = "acc-emola-movitel"
        val bankId = "acc-bank-bim"

        if (accountDao.getAccountCount() == 0) {
            val defaultAccounts = listOf(
                AccountEntity(
                    id = mpesaId,
                    name = "Vodacom M-Pesa",
                    type = AccountType.MPESA.id,
                    balance = 5420.00,
                    accountNumber = "+258 84 555 1234",
                    currency = "MT",
                    lastSyncMillis = System.currentTimeMillis()
                ),
                AccountEntity(
                    id = emolaId,
                    name = "Movitel e-Mola",
                    type = AccountType.EMOLA.id,
                    balance = 2150.00,
                    accountNumber = "+258 86 777 9876",
                    currency = "MT",
                    lastSyncMillis = System.currentTimeMillis()
                ),
                AccountEntity(
                    id = bankId,
                    name = "Millennium BIM",
                    type = AccountType.BANK.id,
                    balance = 18500.00,
                    accountNumber = "0001.23456789.10",
                    currency = "MT",
                    lastSyncMillis = System.currentTimeMillis()
                )
            )
            accountDao.insertAllAccounts(defaultAccounts)
        }

        if (expenseDao.getCount() == 0) {
            val currentMonth = Formatters.currentMonthKey()
            val prevMonth = Formatters.getAdjacentMonthKey(currentMonth, -1)

            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonthIdx = cal.get(Calendar.MONTH)

            // Seed default budget in MT (Meticais)
            budgetDao.setBudget(MonthlyBudgetEntity(currentMonth, 15000.0))
            budgetDao.setBudget(MonthlyBudgetEntity(prevMonth, 14000.0))

            fun makeDate(year: Int, monthIdx: Int, day: Int): Long {
                val c = Calendar.getInstance()
                c.set(year, monthIdx, day, 12, 0, 0)
                return c.timeInMillis
            }

            val samples = listOf(
                // Current month expenses linked to platforms
                ExpenseEntity(
                    title = "Supermercado Recheio",
                    amount = 1850.00,
                    categoryId = ExpenseCategory.ALIMENTACAO.id,
                    paymentMethod = PaymentMethod.EMOLA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx, 2),
                    monthKey = currentMonth,
                    notes = "Compras de mantimentos no Recheio",
                    accountId = emolaId,
                    accountName = "Movitel e-Mola",
                    platformTransactionId = "EM260902.1420.A01"
                ),
                ExpenseEntity(
                    title = "Energia Credelec EDM",
                    amount = 500.00,
                    categoryId = ExpenseCategory.MORADIA.id,
                    paymentMethod = PaymentMethod.MPESA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx, 4),
                    monthKey = currentMonth,
                    notes = "Recarga de energia pré-paga",
                    accountId = mpesaId,
                    accountName = "Vodacom M-Pesa",
                    platformTransactionId = "CI260904.0930.H54321"
                ),
                ExpenseEntity(
                    title = "Renda da Casa (Aluguer)",
                    amount = 6500.00,
                    categoryId = ExpenseCategory.MORADIA.id,
                    paymentMethod = PaymentMethod.TRANSFERENCIA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx, 5),
                    monthKey = currentMonth,
                    notes = "Transferência bancária para senhorio",
                    accountId = bankId,
                    accountName = "Millennium BIM",
                    platformTransactionId = "BIM-TR-998811"
                ),
                ExpenseEntity(
                    title = "Combustível Petromoc",
                    amount = 800.00,
                    categoryId = ExpenseCategory.TRANSPORTE.id,
                    paymentMethod = PaymentMethod.MPESA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx, 8),
                    monthKey = currentMonth,
                    notes = "Abastecimento M-Pesa QR",
                    accountId = mpesaId,
                    accountName = "Vodacom M-Pesa",
                    platformTransactionId = "CI260908.1145.K87654"
                ),
                ExpenseEntity(
                    title = "Farmácia Moderna",
                    amount = 450.00,
                    categoryId = ExpenseCategory.SAUDE.id,
                    paymentMethod = PaymentMethod.EMOLA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx, 10),
                    monthKey = currentMonth,
                    notes = "Medicamentos e vitaminas",
                    accountId = emolaId,
                    accountName = "Movitel e-Mola",
                    platformTransactionId = "EM260910.1630.B22"
                ),
                ExpenseEntity(
                    title = "Internet Fibra TVCabo",
                    amount = 1250.00,
                    categoryId = ExpenseCategory.CONTAS.id,
                    paymentMethod = PaymentMethod.TRANSFERENCIA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx, 12),
                    monthKey = currentMonth,
                    notes = "Mensalidade TVCabo",
                    accountId = bankId,
                    accountName = "Millennium BIM",
                    platformTransactionId = "BIM-DEB-443322"
                ),
                ExpenseEntity(
                    title = "Recarga Vodacom & Megas",
                    amount = 200.00,
                    categoryId = ExpenseCategory.CONTAS.id,
                    paymentMethod = PaymentMethod.MPESA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx, 14),
                    monthKey = currentMonth,
                    notes = "Pacote Tudo-em-Um Vodacom",
                    accountId = mpesaId,
                    accountName = "Vodacom M-Pesa",
                    platformTransactionId = "CI260914.0815.M11223"
                ),
                ExpenseEntity(
                    title = "Almoço Restaurante Costa do Sol",
                    amount = 680.00,
                    categoryId = ExpenseCategory.LAZER.id,
                    paymentMethod = PaymentMethod.MPESA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx, 15),
                    monthKey = currentMonth,
                    notes = "Almoço de fim de semana",
                    accountId = mpesaId,
                    accountName = "Vodacom M-Pesa",
                    platformTransactionId = "CI260915.1320.P99887"
                ),

                // Previous month expenses for trend graphs
                ExpenseEntity(
                    title = "Renda Anterior",
                    amount = 6500.00,
                    categoryId = ExpenseCategory.MORADIA.id,
                    paymentMethod = PaymentMethod.TRANSFERENCIA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx - 1, 5),
                    monthKey = prevMonth,
                    accountId = bankId,
                    accountName = "Millennium BIM"
                ),
                ExpenseEntity(
                    title = "Supermercado Recheio",
                    amount = 2100.00,
                    categoryId = ExpenseCategory.ALIMENTACAO.id,
                    paymentMethod = PaymentMethod.EMOLA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx - 1, 8),
                    monthKey = prevMonth,
                    accountId = emolaId,
                    accountName = "Movitel e-Mola"
                ),
                ExpenseEntity(
                    title = "Combustível TotalEnergies",
                    amount = 950.00,
                    categoryId = ExpenseCategory.TRANSPORTE.id,
                    paymentMethod = PaymentMethod.MPESA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx - 1, 12),
                    monthKey = prevMonth,
                    accountId = mpesaId,
                    accountName = "Vodacom M-Pesa"
                ),
                ExpenseEntity(
                    title = "Energia Credelec",
                    amount = 500.00,
                    categoryId = ExpenseCategory.MORADIA.id,
                    paymentMethod = PaymentMethod.MPESA.displayName,
                    dateMillis = makeDate(currentYear, currentMonthIdx - 1, 18),
                    monthKey = prevMonth,
                    accountId = mpesaId,
                    accountName = "Vodacom M-Pesa"
                )
            )

            expenseDao.insertAll(samples)
        }
    }
}
