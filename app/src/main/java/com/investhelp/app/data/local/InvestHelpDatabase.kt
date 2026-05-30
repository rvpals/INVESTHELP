package com.investhelp.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.investhelp.app.data.local.converter.Converters
import com.investhelp.app.data.local.dao.ChangeHistoryDao
import com.investhelp.app.data.local.dao.CsvImportMappingDao
import com.investhelp.app.data.local.dao.DefinitionDao
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.dao.InvestmentTransactionDao
import com.investhelp.app.data.local.dao.AccountPerformanceDao
import com.investhelp.app.data.local.dao.WatchListDao
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import com.investhelp.app.data.local.entity.ChangeHistoryEntity
import com.investhelp.app.data.local.entity.CsvImportMappingEntity
import com.investhelp.app.data.local.entity.DefinitionEntity
import com.investhelp.app.data.local.entity.NamedCsvMappingEntity
import com.investhelp.app.data.local.entity.AiLibraryEntity
import com.investhelp.app.data.local.entity.SqlLibraryEntity
import com.investhelp.app.data.local.dao.AiLibraryDao
import com.investhelp.app.data.local.dao.SqlLibraryDao
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
        AccountPerformanceEntity::class,
        WatchListEntity::class,
        WatchListItemEntity::class,
        CsvImportMappingEntity::class,
        NamedCsvMappingEntity::class,
        ChangeHistoryEntity::class,
        DefinitionEntity::class,
        SqlLibraryEntity::class,
        AiLibraryEntity::class
    ],
    version = 29,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class InvestHelpDatabase : RoomDatabase() {
    abstract fun accountDao(): InvestmentAccountDao
    abstract fun itemDao(): InvestmentItemDao
    abstract fun transactionDao(): InvestmentTransactionDao
    abstract fun accountPerformanceDao(): AccountPerformanceDao
    abstract fun watchListDao(): WatchListDao
    abstract fun csvImportMappingDao(): CsvImportMappingDao
    abstract fun changeHistoryDao(): ChangeHistoryDao
    abstract fun definitionDao(): DefinitionDao
    abstract fun sqlLibraryDao(): SqlLibraryDao
    abstract fun aiLibraryDao(): AiLibraryDao
}
