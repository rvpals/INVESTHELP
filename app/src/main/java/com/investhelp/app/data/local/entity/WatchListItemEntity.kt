package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "watch_list_items",
    foreignKeys = [
        ForeignKey(
            entity = WatchListEntity::class,
            parentColumns = ["id"],
            childColumns = ["watchListId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("watchListId")]
)
data class WatchListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val watchListId: Long,
    val ticker: String,
    val shares: Double,
    val priceWhenAdded: Double,
    val addedDate: LocalDate
)
