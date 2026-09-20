package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // MPESA, EMOLA, BANK, CASH, OTHER
    val balance: Double,
    val accountNumber: String = "",
    val currency: String = "MT",
    val apiKey: String? = null,
    val apiSecret: String? = null,
    val lastSyncMillis: Long = System.currentTimeMillis(),
    val isConnected: Boolean = true
) {
    fun getAccountType(): AccountType = AccountType.fromId(type)
}
