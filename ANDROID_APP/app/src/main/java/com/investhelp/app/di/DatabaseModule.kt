package com.investhelp.app.di

import android.content.Context
import com.investhelp.app.data.local.DatabaseProvider
import com.investhelp.app.data.local.dao.AccountPerformanceDao
import com.investhelp.app.data.local.dao.ChangeHistoryDao
import com.investhelp.app.data.local.dao.CorrelationCacheDao
import com.investhelp.app.data.local.dao.CsvImportMappingDao
import com.investhelp.app.data.local.dao.DefinitionDao
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.dao.InvestmentTransactionDao
import com.investhelp.app.data.local.dao.AiLibraryDao
import com.investhelp.app.data.local.dao.SqlLibraryDao
import com.investhelp.app.data.local.dao.VolatilityCacheDao
import com.investhelp.app.data.local.dao.WatchListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider {
        return DatabaseProvider(context)
    }

    @Provides
    fun provideAccountDao(dbProvider: DatabaseProvider): InvestmentAccountDao {
        return dbProvider.database.accountDao()
    }

    @Provides
    fun provideItemDao(dbProvider: DatabaseProvider): InvestmentItemDao {
        return dbProvider.database.itemDao()
    }

    @Provides
    fun provideTransactionDao(dbProvider: DatabaseProvider): InvestmentTransactionDao {
        return dbProvider.database.transactionDao()
    }


    @Provides
    fun provideAccountPerformanceDao(dbProvider: DatabaseProvider): AccountPerformanceDao {
        return dbProvider.database.accountPerformanceDao()
    }

    @Provides
    fun provideWatchListDao(dbProvider: DatabaseProvider): WatchListDao {
        return dbProvider.database.watchListDao()
    }

    @Provides
    fun provideCsvImportMappingDao(dbProvider: DatabaseProvider): CsvImportMappingDao {
        return dbProvider.database.csvImportMappingDao()
    }

    @Provides
    fun provideChangeHistoryDao(dbProvider: DatabaseProvider): ChangeHistoryDao {
        return dbProvider.database.changeHistoryDao()
    }

    @Provides
    fun provideDefinitionDao(dbProvider: DatabaseProvider): DefinitionDao {
        return dbProvider.database.definitionDao()
    }

    @Provides
    fun provideSqlLibraryDao(dbProvider: DatabaseProvider): SqlLibraryDao {
        return dbProvider.database.sqlLibraryDao()
    }

    @Provides
    fun provideAiLibraryDao(dbProvider: DatabaseProvider): AiLibraryDao {
        return dbProvider.database.aiLibraryDao()
    }

    @Provides
    fun provideVolatilityCacheDao(dbProvider: DatabaseProvider): VolatilityCacheDao {
        return dbProvider.database.volatilityCacheDao()
    }

    @Provides
    fun provideCorrelationCacheDao(dbProvider: DatabaseProvider): CorrelationCacheDao {
        return dbProvider.database.correlationCacheDao()
    }
}
