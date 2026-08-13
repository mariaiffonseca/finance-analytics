package com.mariafonseca.financeanalytics.features.transactions.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        // Backstop for CsvImportPipeline's app-level (date, merchant, amount)
        // dedup check: catches a duplicate insert even if that check is ever
        // bypassed by a future write path.
        Index(value = ["date", "merchant", "amountMinorUnits"], unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val merchant: String,
    val amountMinorUnits: Long,
    val category: String?,
)
