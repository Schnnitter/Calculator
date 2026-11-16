package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "calculation_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mode: String,
    val expressionDisplay: String,
    val resultDisplay: String,
    val rawResult: String,
    val fractionJson: String? = null,
    val powerJson: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val code: String,
    val ratePerUsd: String,
    val updatedAtMillis: Long,
    val sourceLabel: String
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestampMillis DESC")
    fun observeHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: HistoryEntity)

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAll()
}

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currency_rates ORDER BY code ASC")
    fun observeRates(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates")
    suspend fun getAllRatesOnce(): List<CurrencyRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRates(rates: List<CurrencyRateEntity>)

    @Query("DELETE FROM currency_rates")
    suspend fun clearRates()
}

@Database(
    entities = [HistoryEntity::class, CurrencyRateEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun currencyDao(): CurrencyDao

    companion object {
        @Volatile
        private var INSTANCE: CalculatorDatabase? = null

        fun getInstance(context: Context): CalculatorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CalculatorDatabase::class.java,
                    "calculator_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
