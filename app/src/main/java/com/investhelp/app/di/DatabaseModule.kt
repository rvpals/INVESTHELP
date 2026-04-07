package com.investhelp.app.di

import android.content.Context
import com.investhelp.app.data.local.DatabaseProvider
import com.investhelp.app.data.local.dao.BankTransferDao
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.dao.InvestmentTransactionDao
import com.investhelp.app.data.local.dao.PositionDao
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
    fun providePositionDao(dbProvider: DatabaseProvider): PositionDao {
        return dbProvider.database.positionDao()
    }

    @Provides
    fun provideBankTransferDao(dbProvider: DatabaseProvider): BankTransferDao {
        return dbProvider.database.bankTransferDao()
    }
}
