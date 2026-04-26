package com.investhelp.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_lists")
data class WatchListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
