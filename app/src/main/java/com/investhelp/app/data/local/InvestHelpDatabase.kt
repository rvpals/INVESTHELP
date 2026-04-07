package com.investhelp.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.investhelp.app.data.local.converter.Converters
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.local.dao.InvestmentTransactionDao
import com.investhelp.app.data.local.dao.PositionDao
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.data.local.entity.PositionEntity

@Database(
    entities = [
        InvestmentAccountEntity::class,
        InvestmentItemEntity::class,
        InvestmentTransactionEntity::class,
        PositionEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class InvestHelpDatabase : RoomDatabase() {
    abstract fun accountDao(): InvestmentAccountDao
    abstract fun itemDao(): InvestmentItemDao
    abstract fun transactionDao(): InvestmentTransactionDao
    abstract fun positionDao(): PositionDao
}
