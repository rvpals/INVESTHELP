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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29)
            .build()
    }

    companion object {
        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_library (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        promptText TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO ai_library (name, description, promptText) VALUES (
                        'Forensic Ticker Deep-Dive',
                        'Use this prompt when you want to look past a company''s marketing PR and look strictly at capital flows, potential red flags, and structural revenue.',
                        'Act as a senior equity research analyst. Provide a "Deep Dive" forensic analysis for the ticker [TICKER] based on its most recent quarterly results and past 30 days of market news. Maintain a objective, highly analytical, and skeptical tone.

Structure your response exactly using these sections:
1. Narrative vs. Reality: Contrast the headline PR narrative with the actual underlying order flow, margin compression, or growth drivers.
2. The Balance Sheet & Cash Flow: Highlight revenue vs. cash burn. Are they funding operations through organic profit, or is there a risk of share dilution/debt issuance?
3. Ownership & Positioning: Detail institutional ownership vs. retail concentration, short interest, and recent insider trading activity.
4. Bear, Bull, and Realistic Scenarios: Provide 3 clear near-term scenarios for the stock.
5. Key Catalyst: Identify the single most critical upcoming date or event (earnings, regulatory ruling, product launch) that isn''t fully priced in yet.'
                    )
                """)
                db.execSQL("""
                    INSERT INTO ai_library (name, description, promptText) VALUES (
                        '10-K/10-Q Earnings Summarizer',
                        'Instead of reading through a 50-page SEC filing, you can paste this prompt right after an earnings release to extract management''s true forward guidance and hidden risks.',
                        'Analyze the most recent earnings report and regulatory filings for [TICKER]. Do not give me generic corporate descriptions. Extract and summarize the following in concise bullet points:

- Key Financial Metrics: (Revenue, EPS, EBITDA, and Free Cash Flow) vs. Wall Street expectations.
- Management Guidance: Explicit forward-looking statements or changes to full-year outlooks.
- Hidden Headwinds: Any mentioned supply chain issues, margin pressures, legal risks, or macro factors that could act as a drag on the stock.
- The "Hook": What was the single biggest surprise or takeaway from the earnings call?'
                    )
                """)
                db.execSQL("""
                    INSERT INTO ai_library (name, description, promptText) VALUES (
                        'ETF "Under the Hood" Deconstruction',
                        'When evaluating an ETF, you want to know if it''s truly diversified or just carried by a few massive tech stocks, and how it handles market volatility.',
                        'Act as an institutional portfolio manager. Analyze the ETF ticker [TICKER]. Provide a structured breakdown covering:

1. Concentration Risk: What percentage of the fund is held by the top 5 and top 10 holdings? Is the fund top-heavy?
2. Sector & Macro Exposure: What specific macroeconomic themes (e.g., rising interest rates, commodity cycles, tech adoption) is this ETF highly sensitive to?
3. Expense Ratio & Liquidity: Evaluate its expense ratio against its core category peers and assess its average daily volume for slippage risks.
4. Peer Comparison: Compare this ETF briefly to its top two direct competitors in terms of performance drag and structural strategy.'
                    )
                """)
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sql_library (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        sql TEXT NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS investment_positions (
                        ticker TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        currentPrice REAL NOT NULL,
                        quantity REAL NOT NULL,
                        dayGainLoss REAL NOT NULL,
                        value REAL NOT NULL,
                        dayHigh REAL NOT NULL DEFAULT 0.0,
                        dayLow REAL NOT NULL DEFAULT 0.0,
                        logo BLOB
                    )
                """)
                db.execSQL("""
                    INSERT INTO investment_positions (ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow, logo)
                    SELECT ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow, logo
                    FROM investment_items
                """)
                db.execSQL("DROP TABLE investment_items")
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_investment_transactions_date_action_ticker_totalAmount ON investment_transactions(date, action, ticker, totalAmount)")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS definitions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL
                    )"""
                )
                db.execSQL("INSERT INTO definitions (name, description) VALUES ('Trailing P/E', 'Price-to-Earnings ratio based on the last 12 months of actual earnings. Calculated as current stock price divided by earnings per share (EPS) over the past 12 months. A higher trailing P/E suggests investors are paying more for each dollar of recent earnings, often indicating growth expectations.')")
                db.execSQL("INSERT INTO definitions (name, description) VALUES ('Forward P/E', 'Price-to-Earnings ratio based on projected future earnings. Calculated as current stock price divided by estimated earnings per share for the next 12 months. Lower than trailing P/E suggests analysts expect earnings growth; higher suggests expected earnings decline.')")
                db.execSQL("INSERT INTO definitions (name, description) VALUES ('EPS', 'Earnings Per Share. Net income available to common shareholders divided by the total number of outstanding shares. Measures how much profit is allocated to each share of stock. Higher EPS indicates greater profitability. Diluted EPS also accounts for convertible securities and stock options.')")
                db.execSQL("INSERT INTO definitions (name, description) VALUES ('Market Cap', 'Market Capitalization. Total market value of a company''s outstanding shares. Calculated as current stock price multiplied by total shares outstanding. Used to classify companies: Mega-cap (>$200B), Large-cap ($10B-$200B), Mid-cap ($2B-$10B), Small-cap ($300M-$2B), Micro-cap (<$300M).')")
                db.execSQL("INSERT INTO definitions (name, description) VALUES ('Dividend Yield', 'Annual dividend payment expressed as a percentage of the stock''s current price. Calculated as annual dividends per share divided by price per share. A 3% yield means you receive $3 in dividends for every $100 invested. Higher yields provide income but may signal limited growth reinvestment.')")
                db.execSQL("INSERT INTO definitions (name, description) VALUES ('Revenue/Share', 'Revenue Per Share. Total revenue (sales) divided by the number of outstanding shares. Indicates how much top-line revenue each share generates before any expenses are deducted. Useful for comparing companies with negative earnings where EPS is not meaningful.')")
                db.execSQL("INSERT INTO definitions (name, description) VALUES ('Profit Margins', 'Percentage of revenue that remains as profit after expenses. Net profit margin = net income divided by total revenue. For example, a 20% margin means $0.20 of every dollar of revenue becomes profit. Higher margins indicate better cost control and pricing power relative to peers in the same industry.')")
                db.execSQL("INSERT INTO definitions (name, description) VALUES ('Return on Equity', 'ROE. Measures how efficiently a company generates profit from shareholders'' equity. Calculated as net income divided by shareholders'' equity, expressed as a percentage. A 15% ROE means the company generates $0.15 of profit for every $1 of equity. Higher ROE indicates management is using invested capital effectively.')")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE investment_accounts ADD COLUMN lastUpdatedOn INTEGER")
                db.execSQL("ALTER TABLE investment_accounts ADD COLUMN lastValue REAL")
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS csv_named_mappings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        importType TEXT NOT NULL,
                        mappingsJson TEXT NOT NULL,
                        dateFormatJson TEXT NOT NULL DEFAULT ''
                    )"""
                )
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE change_history ADD COLUMN dailyChangeEtf REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE change_history ADD COLUMN dailyChangeStock REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE change_history ADD COLUMN dailyChangeTotal REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS investment_transactions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        time INTEGER,
                        action TEXT NOT NULL,
                        ticker TEXT NOT NULL,
                        numberOfShares REAL NOT NULL,
                        pricePerShare REAL NOT NULL,
                        totalAmount REAL NOT NULL DEFAULT 0.0,
                        note TEXT NOT NULL DEFAULT ''
                    )"""
                )
                db.execSQL(
                    """INSERT INTO investment_transactions_new
                        (id, date, time, action, ticker, numberOfShares, pricePerShare, totalAmount, note)
                        SELECT id, date, time, action, ticker, numberOfShares, pricePerShare, totalAmount, note
                        FROM investment_transactions"""
                )
                db.execSQL("DROP TABLE investment_transactions")
                db.execSQL("ALTER TABLE investment_transactions_new RENAME TO investment_transactions")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_transactions_ticker ON investment_transactions(ticker)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE investment_items ADD COLUMN logo BLOB")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE watch_list_items ADD COLUMN reminderDateTime INTEGER")
                db.execSQL("ALTER TABLE watch_list_items ADD COLUMN reminderMessage TEXT")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS change_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        etfValue REAL NOT NULL,
                        stockValue REAL NOT NULL,
                        totalValue REAL NOT NULL
                    )"""
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_change_history_date ON change_history(date)")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS bank_transfers")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS account_performance_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accountId INTEGER NOT NULL,
                        totalValue REAL NOT NULL,
                        date INTEGER NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(accountId) REFERENCES investment_accounts(id) ON DELETE CASCADE
                    )"""
                )
                // Convert epoch seconds (dateTime) to epoch days (date), keeping latest per accountId+day
                db.execSQL(
                    """INSERT OR REPLACE INTO account_performance_new (id, accountId, totalValue, date, note)
                        SELECT id, accountId, totalValue, CAST(dateTime / 86400 AS INTEGER), note
                        FROM account_performance"""
                )
                db.execSQL("DROP TABLE account_performance")
                db.execSQL("ALTER TABLE account_performance_new RENAME TO account_performance")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_account_performance_accountId ON account_performance(accountId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_account_performance_accountId_date ON account_performance(accountId, date)")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate investment_items with ticker-only PK, no accountId
                // Merge duplicate tickers by summing quantity/cost/value/gainLoss
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS investment_items_new (
                        ticker TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        currentPrice REAL NOT NULL,
                        quantity REAL NOT NULL,
                        cost REAL NOT NULL,
                        dayGainLoss REAL NOT NULL,
                        totalGainLoss REAL NOT NULL,
                        value REAL NOT NULL,
                        dayHigh REAL NOT NULL DEFAULT 0.0,
                        dayLow REAL NOT NULL DEFAULT 0.0
                    )"""
                )
                db.execSQL(
                    """INSERT INTO investment_items_new
                        (ticker, name, type, currentPrice, quantity, cost,
                         dayGainLoss, totalGainLoss, value, dayHigh, dayLow)
                        SELECT ticker,
                            name,
                            type,
                            currentPrice,
                            SUM(quantity),
                            SUM(cost),
                            SUM(dayGainLoss),
                            SUM(totalGainLoss),
                            SUM(value),
                            MAX(dayHigh),
                            MAX(dayLow)
                        FROM investment_items
                        GROUP BY ticker"""
                )
                db.execSQL("DROP TABLE investment_items")
                db.execSQL("ALTER TABLE investment_items_new RENAME TO investment_items")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS csv_import_mappings (
                        importType TEXT NOT NULL PRIMARY KEY,
                        mappingsJson TEXT NOT NULL,
                        dateFormatJson TEXT NOT NULL DEFAULT ''
                    )"""
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS watch_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS watch_list_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        watchListId INTEGER NOT NULL,
                        ticker TEXT NOT NULL,
                        shares REAL NOT NULL,
                        priceWhenAdded REAL NOT NULL,
                        addedDate INTEGER NOT NULL,
                        FOREIGN KEY(watchListId) REFERENCES watch_lists(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_watch_list_items_watchListId ON watch_list_items(watchListId)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE account_performance ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

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
