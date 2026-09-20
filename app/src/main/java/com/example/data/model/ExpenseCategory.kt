package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ExpenseCategory(
    val id: String,
    val displayName: String,
    val color: Color,
    val icon: ImageVector
) {
    ALIMENTACAO("ALIMENTACAO", "Alimentação", Color(0xFF10B981), Icons.Default.Restaurant),
    TRANSPORTE("TRANSPORTE", "Transporte", Color(0xFF3B82F6), Icons.Default.DirectionsCar),
    MORADIA("MORADIA", "Moradia", Color(0xFF8B5CF6), Icons.Default.Home),
    CONTAS("CONTAS", "Contas & Serviços", Color(0xFF06B6D4), Icons.Default.Receipt),
    LAZER("LAZER", "Lazer & Diversão", Color(0xFFF59E0B), Icons.Default.Movie),
    SAUDE("SAUDE", "Saúde & Farmácia", Color(0xFFEF4444), Icons.Default.LocalHospital),
    COMPRAS("COMPRAS", "Compras & Roupas", Color(0xFFEC4899), Icons.Default.ShoppingBag),
    EDUCACAO("EDUCACAO", "Educação", Color(0xFF6366F1), Icons.Default.School),
    OUTROS("OUTROS", "Outros", Color(0xFF64748B), Icons.Default.MoreHoriz);

    companion object {
        fun fromId(id: String): ExpenseCategory {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OUTROS
        }
    }
}
