package com.example.data.repository

import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.domain.model.HistoryItem
import com.example.domain.model.HistoryMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val historyDao: HistoryDao) {

    val historyFlow: Flow<List<HistoryItem>> = historyDao.observeHistory().map { list ->
        list.map { entity ->
            HistoryItem(
                id = entity.id,
                mode = runCatching { HistoryMode.valueOf(entity.mode) }.getOrDefault(HistoryMode.STANDARD),
                expressionDisplay = entity.expressionDisplay,
                resultDisplay = entity.resultDisplay,
                rawResult = entity.rawResult,
                fractionJson = entity.fractionJson,
                powerJson = entity.powerJson,
                timestampMillis = entity.timestampMillis
            )
        }
    }

    suspend fun saveEntry(
        mode: HistoryMode,
        expressionDisplay: String,
        resultDisplay: String,
        rawResult: String,
        fractionJson: String? = null,
        powerJson: String? = null
    ) {
        historyDao.insertHistory(
            HistoryEntity(
                mode = mode.name,
                expressionDisplay = expressionDisplay,
                resultDisplay = resultDisplay,
                rawResult = rawResult,
                fractionJson = fractionJson,
                powerJson = powerJson,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteEntry(id: Long) {
        historyDao.deleteById(id)
    }

    suspend fun clearAll() {
        historyDao.clearAll()
    }
}
