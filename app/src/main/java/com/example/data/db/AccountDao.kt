package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY type ASC, name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE type = :type LIMIT 1")
    suspend fun getAccountByType(type: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("UPDATE accounts SET balance = :newBalance, lastSyncMillis = :syncMillis WHERE id = :id")
    suspend fun updateBalance(id: String, newBalance: Double, syncMillis: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET balance = balance - :amount, lastSyncMillis = :syncMillis WHERE id = :id")
    suspend fun deductBalance(id: String, amount: Double, syncMillis: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET balance = balance + :amount, lastSyncMillis = :syncMillis WHERE id = :id")
    suspend fun addBalance(id: String, amount: Double, syncMillis: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
}
