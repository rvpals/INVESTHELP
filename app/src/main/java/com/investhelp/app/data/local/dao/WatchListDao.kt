package com.investhelp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.investhelp.app.data.local.entity.WatchListEntity
import com.investhelp.app.data.local.entity.WatchListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchListDao {

    @Query("SELECT * FROM watch_lists ORDER BY name ASC")
    fun getAllWatchLists(): Flow<List<WatchListEntity>>

    @Query("SELECT * FROM watch_list_items WHERE watchListId = :watchListId ORDER BY addedDate DESC")
    fun getItemsByWatchList(watchListId: Long): Flow<List<WatchListItemEntity>>

    @Query("SELECT * FROM watch_list_items WHERE id = :id")
    fun getItemById(id: Long): Flow<WatchListItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchList(watchList: WatchListEntity): Long

    @Update
    suspend fun updateWatchList(watchList: WatchListEntity)

    @Delete
    suspend fun deleteWatchList(watchList: WatchListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WatchListItemEntity): Long

    @Update
    suspend fun updateItem(item: WatchListItemEntity)

    @Delete
    suspend fun deleteItem(item: WatchListItemEntity)
}
