package com.investhelp.app.data.repository

import com.investhelp.app.data.local.dao.WatchListDao
import com.investhelp.app.data.local.entity.WatchListEntity
import com.investhelp.app.data.local.entity.WatchListItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface WatchListRepository {
    fun getAllWatchLists(): Flow<List<WatchListEntity>>
    fun getItemsByWatchList(watchListId: Long): Flow<List<WatchListItemEntity>>
    fun getItemById(id: Long): Flow<WatchListItemEntity?>
    suspend fun insertWatchList(watchList: WatchListEntity): Long
    suspend fun updateWatchList(watchList: WatchListEntity)
    suspend fun deleteWatchList(watchList: WatchListEntity)
    suspend fun insertItem(item: WatchListItemEntity): Long
    suspend fun updateItem(item: WatchListItemEntity)
    suspend fun deleteItem(item: WatchListItemEntity)
}

@Singleton
class WatchListRepositoryImpl @Inject constructor(
    private val dao: WatchListDao
) : WatchListRepository {
    override fun getAllWatchLists() = dao.getAllWatchLists()
    override fun getItemsByWatchList(watchListId: Long) = dao.getItemsByWatchList(watchListId)
    override fun getItemById(id: Long) = dao.getItemById(id)
    override suspend fun insertWatchList(watchList: WatchListEntity) = dao.insertWatchList(watchList)
    override suspend fun updateWatchList(watchList: WatchListEntity) = dao.updateWatchList(watchList)
    override suspend fun deleteWatchList(watchList: WatchListEntity) = dao.deleteWatchList(watchList)
    override suspend fun insertItem(item: WatchListItemEntity) = dao.insertItem(item)
    override suspend fun updateItem(item: WatchListItemEntity) = dao.updateItem(item)
    override suspend fun deleteItem(item: WatchListItemEntity) = dao.deleteItem(item)
}
