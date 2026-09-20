package com.example.data.model

enum class PaymentMethod(val displayName: String) {
    MPESA("M-Pesa"),
    EMOLA("e-Mola"),
    TRANSFERENCIA("Transferência Bancária"),
    DEBITO("Cartão de Débito"),
    CREDITO("Cartão de Crédito"),
    DINHEIRO("Dinheiro em Espécie"),
    PIX("PIX"),
    OUTRO("Outro");

    companion object {
        fun fromString(value: String): PaymentMethod {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: MPESA
        }
    }
}
