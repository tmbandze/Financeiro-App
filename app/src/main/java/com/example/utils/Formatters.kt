package com.example.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    private val ptBrLocale = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBrLocale)
    private val monthYearFormat = SimpleDateFormat("MMMM 'de' yyyy", ptBrLocale)
    private val shortMonthFormat = SimpleDateFormat("MMM", ptBrLocale)
    private val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)
    private val dayMonthFormat = SimpleDateFormat("dd 'de' MMM", ptBrLocale)
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)

    var defaultCurrency: String = "MT"

    fun formatCurrency(amount: Double, currency: String = defaultCurrency): String {
        return try {
            val numStr = String.format(ptBrLocale, "%,.2f", amount)
            if (currency == "R$") {
                "R$ $numStr"
            } else {
                "$numStr $currency"
            }
        } catch (e: Exception) {
            String.format(ptBrLocale, "%.2f %s", amount, currency)
        }
    }

    fun currentMonthKey(): String {
        return monthKeyFormat.format(Date())
    }

    fun toMonthKey(dateMillis: Long): String {
        return monthKeyFormat.format(Date(dateMillis))
    }

    fun formatMonthKeyDisplay(monthKey: String): String {
        return try {
            val date = monthKeyFormat.parse(monthKey) ?: return monthKey
            monthYearFormat.format(date).replaceFirstChar { it.uppercase(ptBrLocale) }
        } catch (e: Exception) {
            monthKey
        }
    }

    fun formatMonthKeyShort(monthKey: String): String {
        return try {
            val date = monthKeyFormat.parse(monthKey) ?: return monthKey
            shortMonthFormat.format(date).replace(".", "").replaceFirstChar { it.uppercase(ptBrLocale) }
        } catch (e: Exception) {
            monthKey
        }
    }

    fun formatDateShort(dateMillis: Long): String {
        return shortDateFormat.format(Date(dateMillis))
    }

    fun formatDateFriendly(dateMillis: Long): String {
        val calNow = Calendar.getInstance()
        val calDate = Calendar.getInstance().apply { timeInMillis = dateMillis }

        return if (calNow.get(Calendar.YEAR) == calDate.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calDate.get(Calendar.DAY_OF_YEAR)
        ) {
            "Hoje"
        } else if (calNow.get(Calendar.YEAR) == calDate.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) - calDate.get(Calendar.DAY_OF_YEAR) == 1
        ) {
            "Ontem"
        } else {
            dayMonthFormat.format(Date(dateMillis))
        }
    }

    fun getAdjacentMonthKey(monthKey: String, offsetMonths: Int): String {
        return try {
            val date = monthKeyFormat.parse(monthKey) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.MONTH, offsetMonths)
            }
            monthKeyFormat.format(cal.time)
        } catch (e: Exception) {
            monthKey
        }
    }

    fun getLastNMonthKeys(count: Int, fromMonthKey: String = currentMonthKey()): List<String> {
        val result = mutableListOf<String>()
        for (i in (count - 1) downTo 0) {
            result.add(getAdjacentMonthKey(fromMonthKey, -i))
        }
        return result
    }

    fun getDaysInMonth(monthKey: String): Int {
        return try {
            val date = monthKeyFormat.parse(monthKey) ?: return 30
            val cal = Calendar.getInstance().apply { time = date }
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        } catch (e: Exception) {
            30
        }
    }

    fun getDayOfMonth(dateMillis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        return cal.get(Calendar.DAY_OF_MONTH)
    }
}
