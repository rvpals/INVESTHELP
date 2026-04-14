package com.investhelp.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseProvider @Inject constructor(
    private val context: Context
) {
    val database: InvestHelpDatabase by lazy {
        Room.databaseBuilder(
            context,
            InvestHelpDatabase::class.java,
            "invest_help.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .build()
    }

    companion object {
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS account_performance (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accountId INTEGER NOT NULL,
                        totalValue REAL NOT NULL,
                        dateTime INTEGER NOT NULL,
                        FOREIGN KEY(accountId) REFERENCES investment_accounts(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_account_performance_accountId ON account_performance(accountId)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE investment_items ADD COLUMN dayHigh REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE investment_items ADD COLUMN dayLow REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Merge investment_items + positions into a single table with composite PK
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS investment_items_new (
                        ticker TEXT NOT NULL,
                        accountId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        currentPrice REAL NOT NULL,
                        quantity REAL NOT NULL,
                        cost REAL NOT NULL,
                        dayGainLoss REAL NOT NULL,
                        totalGainLoss REAL NOT NULL,
                        value REAL NOT NULL,
                        PRIMARY KEY(ticker, accountId),
                        FOREIGN KEY(accountId) REFERENCES investment_accounts(id) ON DELETE CASCADE
                    )"""
                )
                // Populate from positions joined with old items for metadata
                db.execSQL(
                    """INSERT INTO investment_items_new
                        (ticker, accountId, name, type, currentPrice, quantity, cost, dayGainLoss, totalGainLoss, value)
                        SELECT p.ticker, p.accountId,
                            COALESCE(i.name, p.ticker),
                            COALESCE(i.type, 'Stock'),
                            COALESCE(i.currentPrice, 0.0),
                            p.quantity, p.cost, p.dayGainLoss, p.totalGainLoss, p.value
                        FROM positions p
                        LEFT JOIN investment_items i ON UPPER(p.ticker) = UPPER(i.ticker)"""
                )
                db.execSQL("DROP TABLE IF EXISTS investment_items")
                db.execSQL("DROP TABLE IF EXISTS positions")
                db.execSQL("ALTER TABLE investment_items_new RENAME TO investment_items")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_items_accountId ON investment_items(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_items_ticker ON investment_items(ticker)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS bank_transfers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        accountId INTEGER NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(accountId) REFERENCES investment_accounts(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_transfers_accountId ON bank_transfers(accountId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE investment_items ADD COLUMN numShares REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate investment_transactions with ticker instead of investmentItemId,
                // nullable time, and new totalAmount + note fields
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS investment_transactions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        time INTEGER,
                        action TEXT NOT NULL,
                        accountId INTEGER NOT NULL,
                        ticker TEXT NOT NULL DEFAULT '',
                        numberOfShares REAL NOT NULL,
                        pricePerShare REAL NOT NULL,
                        totalAmount REAL NOT NULL DEFAULT 0.0,
                        note TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(accountId) REFERENCES investment_accounts(id) ON DELETE CASCADE
                    )"""
                )
                // Copy existing data, mapping investmentItemId to ticker via items table
                db.execSQL(
                    """INSERT INTO investment_transactions_new
                        (id, date, time, action, accountId, ticker, numberOfShares, pricePerShare, totalAmount, note)
                        SELECT t.id, t.date, t.time, t.action, t.accountId,
                            COALESCE(i.ticker, ''), t.numberOfShares, t.pricePerShare,
                            (t.numberOfShares * t.pricePerShare), ''
                        FROM investment_transactions t
                        LEFT JOIN investment_items i ON t.investmentItemId = i.id"""
                )
                db.execSQL("DROP TABLE investment_transactions")
                db.execSQL("ALTER TABLE investment_transactions_new RENAME TO investment_transactions")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_transactions_accountId ON investment_transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_transactions_ticker ON investment_transactions(ticker)")
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE positions ADD COLUMN accountId INTEGER DEFAULT NULL REFERENCES investment_accounts(id) ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_positions_accountId ON positions(accountId)")
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE investment_items ADD COLUMN ticker TEXT DEFAULT NULL")
            }
        }
    }
}
