package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val categoryId: String,
    val paymentMethod: String,
    val dateMillis: Long,
    val monthKey: String, // Format: "yyyy-MM", e.g. "2026-09"
    val notes: String = "",
    val accountId: String? = null, // ID of linked AccountEntity (e.g., M-Pesa, e-Mola, Bank)
    val accountName: String? = null, // e.g. "Vodacom M-Pesa", "Movitel e-Mola"
    val platformTransactionId: String? = null // e.g. "CI260919.1015.H12345"
)
