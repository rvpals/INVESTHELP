package com.investhelp.app.di

import android.content.Context
import com.investhelp.app.data.local.DatabaseProvider
import com.investhelp.app.data.local.dao.AccountPerformanceDao
import com.investhelp.app.data.local.dao.BankTransferDao
import com.investhelp.app.data.local.dao.CsvImportMappingDao
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.dao.InvestmentTransactionDao
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
    fun provideBankTransferDao(dbProvider: DatabaseProvider): BankTransferDao {
        return dbProvider.database.bankTransferDao()
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
}
