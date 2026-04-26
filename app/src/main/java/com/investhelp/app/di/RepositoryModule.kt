package com.investhelp.app.di

import com.investhelp.app.data.repository.AccountPerformanceRepository
import com.investhelp.app.data.repository.AccountPerformanceRepositoryImpl
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.data.repository.AccountRepositoryImpl
import com.investhelp.app.data.repository.BankTransferRepository
import com.investhelp.app.data.repository.BankTransferRepositoryImpl
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.data.repository.InvestmentItemRepositoryImpl
import com.investhelp.app.data.repository.TransactionRepository
import com.investhelp.app.data.repository.TransactionRepositoryImpl
import com.investhelp.app.data.repository.WatchListRepository
import com.investhelp.app.data.repository.WatchListRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    abstract fun bindItemRepository(impl: InvestmentItemRepositoryImpl): InvestmentItemRepository

    @Binds
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    abstract fun bindBankTransferRepository(impl: BankTransferRepositoryImpl): BankTransferRepository

    @Binds
    abstract fun bindAccountPerformanceRepository(impl: AccountPerformanceRepositoryImpl): AccountPerformanceRepository

    @Binds
    abstract fun bindWatchListRepository(impl: WatchListRepositoryImpl): WatchListRepository
}
