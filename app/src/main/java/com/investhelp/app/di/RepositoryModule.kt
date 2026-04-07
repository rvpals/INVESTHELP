package com.investhelp.app.di

import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.data.repository.AccountRepositoryImpl
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.data.repository.InvestmentItemRepositoryImpl
import com.investhelp.app.data.repository.TransactionRepository
import com.investhelp.app.data.repository.TransactionRepositoryImpl
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
}
