package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AccountEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.EditAccountDialog
import com.example.ui.components.ImportReceiptDialog
import com.example.ui.components.SetBudgetDialog
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.utils.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val overview by viewModel.monthlyOverview.collectAsStateWithLifecycle()
    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val platformSpendings by viewModel.platformSpending.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()
    val filterAccountId by viewModel.filterAccountId.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Gráficos, 1: Contas & Saldos, 2: Despesas
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    var showAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showImportReceiptDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "Controle de Despesas"
                            1 -> "Contas & Carteiras"
                            else -> "Extrato de Despesas"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.DonutLarge else Icons.Outlined.DonutLarge,
                            contentDescription = "Gráficos"
                        )
                    },
                    label = { Text("Gráficos") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Contas e Saldos"
                        )
                    },
                    label = { Text("Contas & Saldos") },
                    modifier = Modifier.testTag("nav_tab_accounts")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Despesas"
                        )
                    },
                    label = { Text("Despesas") },
                    modifier = Modifier.testTag("nav_tab_expenses")
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingExpense = null
                    showAddDialog = true
                },
                modifier = Modifier.testTag("add_expense_fab"),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nova Despesa", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    ExpenseDashboardScreen(
                        overview = overview,
                        recentExpenses = filteredExpenses,
                        totalBalance = totalBalance,
                        accounts = accounts,
                        platformSpendings = platformSpendings,
                        onPrevMonth = { viewModel.prevMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onSelectMonth = { viewModel.selectMonth(it) },
                        onCategoryFilterSelect = { cat ->
                            viewModel.setFilterCategory(cat)
                            if (cat != null) {
                                selectedTab = 2
                            }
                        },
                        onOpenBudgetDialog = { showBudgetDialog = true },
                        onOpenAddExpense = {
                            editingExpense = null
                            showAddDialog = true
                        },
                        onViewAllExpenses = { selectedTab = 2 },
                        onNavigateToAccounts = { selectedTab = 1 },
                        onEditExpense = {
                            editingExpense = it
                            showAddDialog = true
                        },
                        onDeleteExpense = { expenseToDelete = it }
                    )
                }

                1 -> {
                    AccountsScreen(
                        accounts = accounts,
                        totalBalance = totalBalance,
                        platformSpendings = platformSpendings,
                        totalSpentMonth = overview.totalSpent,
                        onEditAccount = { acc ->
                            editingAccount = acc
                            showAccountDialog = true
                        },
                        onAddAccount = {
                            editingAccount = null
                            showAccountDialog = true
                        },
                        onOpenImportReceipt = {
                            showImportReceiptDialog = true
                        },
                        onSyncAccount = { accId ->
                            viewModel.syncAccount(accId)
                            scope.launch {
                                snackbarHostState.showSnackbar("Saldo da carteira sincronizado com sucesso!")
                            }
                        },
                        onViewAccountExpenses = { accId ->
                            viewModel.setFilterAccountId(accId)
                            selectedTab = 2
                        }
                    )
                }

                else -> {
                    ExpenseListScreen(
                        expenses = filteredExpenses,
                        accounts = accounts,
                        totalSpent = overview.totalSpent,
                        monthDisplay = overview.monthDisplay,
                        searchQuery = searchQuery,
                        selectedCategory = filterCategory,
                        selectedAccountId = filterAccountId,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategorySelect = { viewModel.setFilterCategory(it) },
                        onAccountSelect = { viewModel.setFilterAccountId(it) },
                        onPrevMonth = { viewModel.prevMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onEditExpense = {
                            editingExpense = it
                            showAddDialog = true
                        },
                        onDeleteExpense = { expenseToDelete = it },
                        onAddExpense = {
                            editingExpense = null
                            showAddDialog = true
                        }
                    )
                }
            }
        }
    }

    // Add or Edit Expense Dialog
    if (showAddDialog) {
        AddExpenseDialog(
            initialExpense = editingExpense,
            accounts = accounts,
            onDismiss = {
                showAddDialog = false
                editingExpense = null
            },
            onSave = { title, amount, categoryId, paymentMethod, notes, accountId, txId ->
                if (editingExpense == null) {
                    viewModel.addExpense(
                        title = title,
                        amount = amount,
                        categoryId = categoryId,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        accountId = accountId,
                        platformTransactionId = txId
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar("Despesa de ${Formatters.formatCurrency(amount)} cadastrada!")
                    }
                } else {
                    viewModel.updateExpense(
                        editingExpense!!.copy(
                            title = title,
                            amount = amount,
                            categoryId = categoryId,
                            paymentMethod = paymentMethod,
                            notes = notes,
                            accountId = accountId,
                            platformTransactionId = txId
                        ),
                        oldExpense = editingExpense
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar("Despesa atualizada com sucesso!")
                    }
                }
                showAddDialog = false
                editingExpense = null
            }
        )
    }

    // Edit or Add Account Dialog
    if (showAccountDialog) {
        EditAccountDialog(
            initialAccount = editingAccount,
            onDismiss = {
                showAccountDialog = false
                editingAccount = null
            },
            onSave = { name, type, balance, accountNumber, apiKey ->
                if (editingAccount == null) {
                    viewModel.addAccount(
                        name = name,
                        type = type,
                        balance = balance,
                        accountNumber = accountNumber,
                        apiKey = apiKey
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar("Conta \"$name\" conectada com sucesso!")
                    }
                } else {
                    viewModel.updateAccount(
                        editingAccount!!.copy(
                            name = name,
                            type = type,
                            balance = balance,
                            accountNumber = accountNumber,
                            apiKey = apiKey,
                            lastSyncMillis = System.currentTimeMillis()
                        )
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar("Dados de \"$name\" atualizados!")
                    }
                }
                showAccountDialog = false
                editingAccount = null
            }
        )
    }

    // Import SMS Receipt Dialog
    if (showImportReceiptDialog) {
        ImportReceiptDialog(
            onDismiss = { showImportReceiptDialog = false },
            onConfirmImport = { receipt ->
                val result = viewModel.parseAndApplyReceipt(receipt.rawText)
                showImportReceiptDialog = false
                scope.launch {
                    if (result != null) {
                        snackbarHostState.showSnackbar(
                            "Comprovativo ${result.platform.brandName} de ${Formatters.formatCurrency(result.amount)} registrado!"
                        )
                    }
                }
            }
        )
    }

    // Set Budget Dialog
    if (showBudgetDialog) {
        SetBudgetDialog(
            currentLimit = overview.budgetLimit,
            monthDisplay = overview.monthDisplay,
            onDismiss = { showBudgetDialog = false },
            onSave = { newLimit ->
                viewModel.updateBudgetLimit(newLimit)
                showBudgetDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Meta mensal atualizada para ${Formatters.formatCurrency(newLimit)}")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Excluir despesa?") },
            text = {
                Text("Deseja realmente remover a despesa \"${expenseToDelete?.title}\"? O saldo da carteira vinculada será restaurado.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it) }
                        expenseToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { expenseToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_btn")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
