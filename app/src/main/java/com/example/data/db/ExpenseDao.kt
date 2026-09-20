package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE monthKey = :monthKey ORDER BY dateMillis DESC, id DESC")
    fun getExpensesByMonth(monthKey: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dateMillis >= :startTime AND dateMillis <= :endTime ORDER BY dateMillis DESC")
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC LIMIT :limit")
    fun getRecentExpenses(limit: Int = 5): Flow<List<ExpenseEntity>>

    @Query("SELECT DISTINCT monthKey FROM expenses ORDER BY monthKey DESC")
    fun getDistinctMonthKeys(): Flow<List<String>>

    @Query("SELECT * FROM expenses WHERE accountId = :accountId ORDER BY dateMillis DESC")
    fun getExpensesByAccount(accountId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE accountId = :accountId AND monthKey = :monthKey ORDER BY dateMillis DESC")
    fun getExpensesByAccountAndMonth(accountId: String, monthKey: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getCount(): Int
}
