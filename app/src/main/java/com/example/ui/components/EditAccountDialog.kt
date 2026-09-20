package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditAccountDialog(
    initialAccount: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, balance: Double, accountNumber: String, apiKey: String?) -> Unit
) {
    val isEditing = initialAccount != null

    var name by remember {
        mutableStateOf(initialAccount?.name ?: "Vodacom M-Pesa")
    }
    var selectedType by remember {
        mutableStateOf(initialAccount?.getAccountType() ?: AccountType.MPESA)
    }
    var balanceText by remember {
        mutableStateOf(
            if (initialAccount != null) String.format("%.2f", initialAccount.balance).replace(".", ",")
            else "0,00"
        )
    }
    var accountNumber by remember {
        mutableStateOf(initialAccount?.accountNumber ?: "+258 84 ")
    }
    var apiKey by remember {
        mutableStateOf(initialAccount?.apiKey ?: "")
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .testTag("edit_account_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = if (isEditing) "Editar Conta / Carteira" else "Conectar Nova Conta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Acompanhe saldos e despesas de M-Pesa, e-Mola ou Banco",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Platform Type Selector Chips
                Text(
                    text = "Plataforma / Tipo",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountType.entries.forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            modifier = Modifier
                                .testTag("select_account_type_${type.id}")
                                .clickable {
                                    selectedType = type
                                    if (!isEditing) {
                                        name = when (type) {
                                            AccountType.MPESA -> "Vodacom M-Pesa"
                                            AccountType.EMOLA -> "Movitel e-Mola"
                                            AccountType.BANK -> "Conta Bancária"
                                            AccountType.CASH -> "Carteira Dinheiro"
                                            AccountType.OTHER -> "Outra Carteira"
                                        }
                                        accountNumber = when (type) {
                                            AccountType.MPESA -> "+258 84 "
                                            AccountType.EMOLA -> "+258 86 "
                                            else -> ""
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) type.brandColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, type.brandColor) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(type.brandColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) type.brandColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Conta") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_account_name"),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current Balance
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = {
                        balanceText = it
                        errorText = null
                    },
                    label = { Text("Saldo Atual") },
                    suffix = { Text("MT", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = {
                        if (errorText != null) {
                            Text(errorText!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_account_balance"),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Account / Phone Number
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Número de Telefone / Conta") },
                    placeholder = { Text("+258 84 123 4567 ou NIB/IBAN") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_account_number"),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Optional API / Consumer Key
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Chave de API / Chave do Serviço (Opcional)") },
                    placeholder = { Text("Chave de integração M-Pesa OpenAPI / Gateway") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_account_api_key"),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_account_btn")
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val parsedBalance = balanceText.replace(",", ".").toDoubleOrNull()
                            if (parsedBalance == null) {
                                errorText = "Insira um valor numérico válido"
                            } else if (name.isBlank()) {
                                errorText = "Informe o nome da conta"
                            } else {
                                onSave(name, selectedType.id, parsedBalance, accountNumber, apiKey.ifBlank { null })
                            }
                        },
                        modifier = Modifier.testTag("save_account_btn"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (isEditing) "Salvar Alterações" else "Conectar Conta")
                    }
                }
            }
        }
    }
}
