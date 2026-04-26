package com.investhelp.app.data.repository

import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.model.AccountWithValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AccountRepository {
    fun getAllAccounts(): Flow<List<InvestmentAccountEntity>>
    fun getAccountById(id: Long): Flow<InvestmentAccountEntity?>
    fun getAllAccountsWithValues(): Flow<List<AccountWithValue>>
    suspend fun insertAccount(account: InvestmentAccountEntity): Long
    suspend fun updateAccount(account: InvestmentAccountEntity)
    suspend fun deleteAccount(account: InvestmentAccountEntity)
    suspend fun computeTotalPortfolioValue(): Double
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: InvestmentAccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<InvestmentAccountEntity>> =
        accountDao.getAllAccounts()

    override fun getAccountById(id: Long): Flow<InvestmentAccountEntity?> =
        accountDao.getAccountById(id)

    override fun getAllAccountsWithValues(): Flow<List<AccountWithValue>> =
        accountDao.getAllAccounts().map { accounts ->
            accounts.map { account ->
                AccountWithValue(
                    account = account,
                    currentValue = account.initialValue
                )
            }
        }

    override suspend fun insertAccount(account: InvestmentAccountEntity): Long =
        accountDao.insertAccount(account)

    override suspend fun updateAccount(account: InvestmentAccountEntity) =
        accountDao.updateAccount(account)

    override suspend fun deleteAccount(account: InvestmentAccountEntity) =
        accountDao.deleteAccount(account)

    override suspend fun computeTotalPortfolioValue(): Double =
        accountDao.computeTotalPortfolioValue()
}
