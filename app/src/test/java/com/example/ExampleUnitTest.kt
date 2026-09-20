package com.example

import com.example.data.model.AccountType
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.utils.Formatters
import com.example.utils.SmsReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCurrencyFormatting() {
    val formatted = Formatters.formatCurrency(1250.50)
    assertTrue(formatted.contains("1.250,50") || formatted.contains("1250"))
    assertTrue(formatted.contains("MT"))
  }

  @Test
  fun testMonthKeyNavigation() {
    val next = Formatters.getAdjacentMonthKey("2026-09", 1)
    assertEquals("2026-10", next)

    val prev = Formatters.getAdjacentMonthKey("2026-09", -1)
    assertEquals("2026-08", prev)

    val yearTurn = Formatters.getAdjacentMonthKey("2026-01", -1)
    assertEquals("2025-12", yearTurn)
  }

  @Test
  fun testCategoryLookup() {
    val alimentacao = ExpenseCategory.fromId("ALIMENTACAO")
    assertEquals(ExpenseCategory.ALIMENTACAO, alimentacao)
    assertEquals("Alimentação", alimentacao.displayName)

    val unknown = ExpenseCategory.fromId("UNKNOWN_CAT")
    assertEquals(ExpenseCategory.OUTROS, unknown)
  }

  @Test
  fun testPaymentMethodLookup() {
    val mpesa = PaymentMethod.fromString("M-Pesa")
    assertEquals(PaymentMethod.MPESA, mpesa)

    val emola = PaymentMethod.fromString("e-Mola")
    assertEquals(PaymentMethod.EMOLA, emola)

    val credito = PaymentMethod.fromString("Cartão de Crédito")
    assertEquals(PaymentMethod.CREDITO, credito)
  }

  @Test
  fun testAccountTypeLookup() {
    assertEquals(AccountType.MPESA, AccountType.fromId("MPESA"))
    assertEquals(AccountType.EMOLA, AccountType.fromId("EMOLA"))
    assertEquals(AccountType.BANK, AccountType.fromId("BANK"))
    assertEquals("Vodacom M-Pesa", AccountType.MPESA.displayName)
    assertEquals("Movitel e-Mola", AccountType.EMOLA.displayName)
  }

  @Test
  fun testMpesaSmsParsing() {
    val mpesaSms = "Confirmado. Pagaste 850.00MT a Supermercado Recheio no dia 2026-09-19 as 10:30. Saldo actual: 4,570.00MT. Transaccao: CI260919.1030.A9988."
    val parsed = SmsReceiptParser.parse(mpesaSms)

    assertNotNull(parsed)
    assertEquals(AccountType.MPESA, parsed?.platform)
    assertEquals(850.0, parsed?.amount ?: 0.0, 0.01)
    assertEquals(4570.0, parsed?.newBalance ?: 0.0, 0.01)
    assertEquals("CI260919.1030.A9988", parsed?.transactionId)
    assertEquals(ExpenseCategory.ALIMENTACAO, parsed?.suggestedCategory)
  }

  @Test
  fun testEmolaSmsParsing() {
    val emolaSms = "e-Mola: Pagamento de 350.00 MT a Farmacia Moderna efectuado com sucesso. Saldo: 1,800.00 MT. TxID: EM2609191030."
    val parsed = SmsReceiptParser.parse(emolaSms)

    assertNotNull(parsed)
    assertEquals(AccountType.EMOLA, parsed?.platform)
    assertEquals(350.0, parsed?.amount ?: 0.0, 0.01)
    assertEquals(1800.0, parsed?.newBalance ?: 0.0, 0.01)
    assertEquals("EM2609191030", parsed?.transactionId)
    assertEquals(ExpenseCategory.SAUDE, parsed?.suggestedCategory)
  }
}
