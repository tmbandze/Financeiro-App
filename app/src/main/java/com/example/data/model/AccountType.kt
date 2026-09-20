package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class AccountType(
    val id: String,
    val displayName: String,
    val brandName: String,
    val brandColor: Color,
    val defaultPrefix: String
) {
    MPESA(
        id = "MPESA",
        displayName = "Vodacom M-Pesa",
        brandName = "M-Pesa",
        brandColor = Color(0xFFE60000), // Iconic Vodacom Red
        defaultPrefix = "+258 84/85"
    ),
    EMOLA(
        id = "EMOLA",
        displayName = "Movitel e-Mola",
        brandName = "e-Mola",
        brandColor = Color(0xFFFF7900), // Movitel Orange
        defaultPrefix = "+258 86/87"
    ),
    BANK(
        id = "BANK",
        displayName = "Conta Bancária",
        brandName = "Banco",
        brandColor = Color(0xFF1E40AF), // Deep Bank Navy
        defaultPrefix = "IBAN / Conta"
    ),
    CASH(
        id = "CASH",
        displayName = "Dinheiro em Espécie",
        brandName = "Dinheiro",
        brandColor = Color(0xFF059669), // Green
        defaultPrefix = "Carteira Física"
    ),
    OTHER(
        id = "OTHER",
        displayName = "Outra Carteira",
        brandName = "Outro",
        brandColor = Color(0xFF6B7280),
        defaultPrefix = "Outro"
    );

    companion object {
        fun fromId(id: String): AccountType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}
