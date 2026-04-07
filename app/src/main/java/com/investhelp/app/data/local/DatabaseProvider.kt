package com.investhelp.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseProvider @Inject constructor(
    private val context: Context
) {
    private var _database: InvestHelpDatabase? = null

    val database: InvestHelpDatabase
        get() = _database ?: throw IllegalStateException("Database not opened. Authenticate first.")

    val isOpen: Boolean
        get() = _database != null

    fun open(passphrase: ByteArray) {
        if (_database != null) return
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(passphrase)
        _database = Room.databaseBuilder(
            context,
            InvestHelpDatabase::class.java,
            "invest_help.db"
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    fun close() {
        _database?.close()
        _database = null
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE investment_items ADD COLUMN ticker TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS positions (
                        ticker TEXT NOT NULL PRIMARY KEY,
                        quantity REAL NOT NULL,
                        cost REAL NOT NULL,
                        dayGainLoss REAL NOT NULL,
                        totalGainLoss REAL NOT NULL,
                        value REAL NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE positions ADD COLUMN accountId INTEGER DEFAULT NULL REFERENCES investment_accounts(id) ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_positions_accountId ON positions(accountId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate positions table with composite PK (ticker + accountId) and CASCADE delete
                db.execSQL("DROP TABLE IF EXISTS positions")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS positions (
                        ticker TEXT NOT NULL,
                        accountId INTEGER NOT NULL,
                        quantity REAL NOT NULL,
                        cost REAL NOT NULL,
                        dayGainLoss REAL NOT NULL,
                        totalGainLoss REAL NOT NULL,
                        value REAL NOT NULL,
                        PRIMARY KEY(ticker, accountId),
                        FOREIGN KEY(accountId) REFERENCES investment_accounts(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_positions_accountId ON positions(accountId)")
            }
        }
    }
}
