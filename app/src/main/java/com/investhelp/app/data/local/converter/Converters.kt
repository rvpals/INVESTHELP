package com.investhelp.app.data.local.converter

import androidx.room.TypeConverter
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.model.TransactionAction
import java.time.LocalDate
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun fromLocalTime(time: LocalTime): Int = time.toSecondOfDay()

    @TypeConverter
    fun toLocalTime(secondOfDay: Int): LocalTime = LocalTime.ofSecondOfDay(secondOfDay.toLong())

    @TypeConverter
    fun fromInvestmentType(type: InvestmentType): String = type.name

    @TypeConverter
    fun toInvestmentType(value: String): InvestmentType = InvestmentType.valueOf(value)

    @TypeConverter
    fun fromTransactionAction(action: TransactionAction): String = action.name

    @TypeConverter
    fun toTransactionAction(value: String): TransactionAction = TransactionAction.valueOf(value)
}
