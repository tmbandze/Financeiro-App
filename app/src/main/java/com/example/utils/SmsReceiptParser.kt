package com.example.utils

import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import java.util.regex.Pattern

data class ParsedReceipt(
    val platform: AccountType,
    val title: String,
    val amount: Double,
    val newBalance: Double?,
    val transactionId: String?,
    val suggestedCategory: ExpenseCategory,
    val rawText: String
)

object SmsReceiptParser {

    /**
     * Parses an M-Pesa, e-Mola, or Bank SMS receipt string into structured data.
     */
    fun parse(smsText: String): ParsedReceipt? {
        val cleanText = smsText.trim()
        if (cleanText.isEmpty()) return null

        val upperText = cleanText.uppercase()

        val platform = when {
            upperText.contains("M-PESA") || upperText.contains("MPESA") || upperText.contains("VODACOM") -> AccountType.MPESA
            upperText.contains("E-MOLA") || upperText.contains("EMOLA") || upperText.contains("MOVITEL") -> AccountType.EMOLA
            upperText.contains("MILLENNIUM") || upperText.contains("BIM") || upperText.contains("BCI") || upperText.contains("STANDARD BANK") || upperText.contains("BANCO") -> AccountType.BANK
            else -> {
                // Heuristic: If it has "transferiste" or "pagaste" with MT, likely M-Pesa or e-Mola
                if (cleanText.contains("MT", ignoreCase = true) && (cleanText.contains("saldo", ignoreCase = true) || cleanText.contains("transaccao", ignoreCase = true) || cleanText.contains("txid", ignoreCase = true))) {
                    AccountType.MPESA
                } else {
                    AccountType.OTHER
                }
            }
        }

        // Extract Amount: looks for patterns like "1.500,00 MT", "500.00MT", "1,200.00 MT", "Valor: 350.00 MT"
        val amount = extractAmount(cleanText) ?: return null

        // Extract New Balance: looks for "Saldo actual: 3,450.00MT", "Saldo: 1,800.00 MT", "Saldo disponivel: ..."
        val newBalance = extractBalance(cleanText)

        // Extract Transaction ID: "Transaccao: CI260919.1015.H12345", "TxID: EM2609191030", "Ref: ..."
        val txId = extractTxId(cleanText)

        // Extract Title / Beneficiary
        val title = extractTitle(cleanText, platform)

        // Suggest Category based on merchant/notes
        val category = guessCategory(title, cleanText)

        return ParsedReceipt(
            platform = platform,
            title = title,
            amount = amount,
            newBalance = newBalance,
            transactionId = txId,
            suggestedCategory = category,
            rawText = cleanText
        )
    }

    private fun extractAmount(text: String): Double? {
        // Match patterns like: (pagaste|transferiste|compra|de|valor|pago|quantia)\s*:?\s*([0-9.,]+)\s*(?:MT|MZN)?
        // Or simply ([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)\s*(?:MT|MZN)
        val patterns = listOf(
            Pattern.compile("""(?:pagaste|transferiste|compra(?:\s+pos)?|valor(?:\s+de)?|debito)\s*:?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?)\s*(?:MT|MZN)?""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2}))\s*(?:MT|MZN)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:MT|MZN)\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2}))""", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val rawNum = matcher.group(1) ?: continue
                val parsed = parseFormattedNumber(rawNum)
                if (parsed != null && parsed > 0) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun extractBalance(text: String): Double? {
        val pattern = Pattern.compile("""saldo(?:\s+actual|\s+disponivel)?\s*:?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?)\s*(?:MT|MZN)?""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val rawNum = matcher.group(1) ?: return null
            return parseFormattedNumber(rawNum)
        }
        return null
    }

    private fun extractTxId(text: String): String? {
        val patterns = listOf(
            Pattern.compile("""(?:transac(?:c|ç)(?:a|ã)o|txid|ref(?:\.)?)\s*:?\s*([A-Za-z0-9._-]+)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""([A-Z]{2}[0-9]{6}\.[0-9]{4}\.[A-Z0-9]+)""") // M-Pesa classic tx format
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    private fun extractTitle(text: String, platform: AccountType): String {
        // Check for merchant or recipient after 'a ' or 'para '
        val merchantPattern = Pattern.compile("""(?:a|para|beneficiario|comerciante|em)\s+([A-Za-z0-9\s&.-]{3,35})(?:\s+no\s+dia|\s+as|\s+saldo|\.|\,)""", Pattern.CASE_INSENSITIVE)
        val matcher = merchantPattern.matcher(text)
        if (matcher.find()) {
            val name = matcher.group(1)?.trim()
            if (!name.isNullOrBlank() && !name.equals("saldo", ignoreCase = true) && !name.equals("dia", ignoreCase = true)) {
                return name
            }
        }

        if (text.contains("Credelec", ignoreCase = true)) return "Energia Credelec"
        if (text.contains("Fipag", ignoreCase = true)) return "Água Fipag"
        if (text.contains("TvCabo", ignoreCase = true) || text.contains("DSTV", ignoreCase = true) || text.contains("Gotv", ignoreCase = true)) return "TV / Internet"

        return when (platform) {
            AccountType.MPESA -> "Despesa M-Pesa"
            AccountType.EMOLA -> "Despesa e-Mola"
            AccountType.BANK -> "Despesa Bancária"
            else -> "Pagamento Registrado"
        }
    }

    private fun guessCategory(title: String, fullText: String): ExpenseCategory {
        val content = "$title $fullText".lowercase()
        return when {
            content.contains("supermercado") || content.contains("recheio") || content.contains("shoprite") ||
            content.contains("restaurante") || content.contains("cafe") || content.contains("padaria") ||
            content.contains("almoço") || content.contains("lanche") || content.contains("pizza") ||
            content.contains("takeaway") || content.contains("kfc") -> ExpenseCategory.ALIMENTACAO

            content.contains("combustivel") || content.contains("petromoc") || content.contains("total") ||
            content.contains("engen") || content.contains("uber") || content.contains("yango") ||
            content.contains("chapa") || content.contains("transporte") || content.contains("tmov") -> ExpenseCategory.TRANSPORTE

            content.contains("credelec") || content.contains("fipag") || content.contains("renda") ||
            content.contains("aluguer") || content.contains("condominio") || content.contains("luz") ||
            content.contains("agua") -> ExpenseCategory.MORADIA

            content.contains("farmacia") || content.contains("clinica") || content.contains("hospital") ||
            content.contains("medico") || content.contains("dentista") -> ExpenseCategory.SAUDE

            content.contains("escola") || content.contains("faculdade") || content.contains("universidade") ||
            content.contains("curso") || content.contains("livro") || content.contains("propinas") -> ExpenseCategory.EDUCACAO

            content.contains("cinema") || content.contains("dstv") || content.contains("gotv") ||
            content.contains("netflix") || content.contains("spotify") || content.contains("show") ||
            content.contains("festa") -> ExpenseCategory.LAZER

            content.contains("movitel") || content.contains("vodacom") || content.contains("tvcabo") ||
            content.contains("internet") || content.contains("recarga") || content.contains("megas") -> ExpenseCategory.CONTAS

            content.contains("roupa") || content.contains("tenis") || content.contains("calcado") ||
            content.contains("loja") || content.contains("shopping") -> ExpenseCategory.COMPRAS

            else -> ExpenseCategory.OUTROS
        }
    }

    private fun parseFormattedNumber(raw: String): Double? {
        val clean = raw.trim()
        // Format can be 1,250.50 or 1.250,50 or 500.00 or 500,00
        return try {
            if (clean.contains(",") && clean.contains(".")) {
                val lastComma = clean.lastIndexOf(',')
                val lastDot = clean.lastIndexOf('.')
                if (lastDot > lastComma) {
                    // English: 1,250.50
                    clean.replace(",", "").toDoubleOrNull()
                } else {
                    // Portuguese: 1.250,50
                    clean.replace(".", "").replace(",", ".").toDoubleOrNull()
                }
            } else if (clean.contains(",")) {
                clean.replace(",", ".").toDoubleOrNull()
            } else {
                clean.toDoubleOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }
}
