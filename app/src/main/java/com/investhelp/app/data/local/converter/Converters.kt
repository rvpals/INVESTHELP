package com.investhelp.app.data.local.converter

import androidx.room.TypeConverter
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.model.TransactionAction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): Int? = time?.toSecondOfDay()

    @TypeConverter
    fun toLocalTime(secondOfDay: Int?): LocalTime? = secondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) }

    @TypeConverter
    fun fromInvestmentType(type: InvestmentType): String = type.name

    @TypeConverter
    fun toInvestmentType(value: String): InvestmentType = InvestmentType.valueOf(value)

    @TypeConverter
    fun fromTransactionAction(action: TransactionAction): String = action.name

    @TypeConverter
    fun toTransactionAction(value: String): TransactionAction = TransactionAction.valueOf(value)

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime): Long = dateTime.toEpochSecond(ZoneOffset.UTC)

    @TypeConverter
    fun toLocalDateTime(epochSecond: Long): LocalDateTime = LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC)
}
