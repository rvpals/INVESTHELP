package com.investhelp.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.investhelp.app.data.local.converter.Converters
import com.investhelp.app.data.local.dao.CsvImportMappingDao
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.dao.InvestmentTransactionDao
import com.investhelp.app.data.local.dao.AccountPerformanceDao
import com.investhelp.app.data.local.dao.BankTransferDao
import com.investhelp.app.data.local.dao.WatchListDao
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import com.investhelp.app.data.local.entity.BankTransferEntity
import com.investhelp.app.data.local.entity.CsvImportMappingEntity
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.data.local.entity.WatchListEntity
import com.investhelp.app.data.local.entity.WatchListItemEntity

@Database(
    entities = [
        InvestmentAccountEntity::class,
        InvestmentItemEntity::class,
        InvestmentTransactionEntity::class,
        BankTransferEntity::class,
        AccountPerformanceEntity::class,
        WatchListEntity::class,
        WatchListItemEntity::class,
        CsvImportMappingEntity::class
    ],
    version = 16,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class InvestHelpDatabase : RoomDatabase() {
    abstract fun accountDao(): InvestmentAccountDao
    abstract fun itemDao(): InvestmentItemDao
    abstract fun transactionDao(): InvestmentTransactionDao
    abstract fun bankTransferDao(): BankTransferDao
    abstract fun accountPerformanceDao(): AccountPerformanceDao
    abstract fun watchListDao(): WatchListDao
    abstract fun csvImportMappingDao(): CsvImportMappingDao
}
