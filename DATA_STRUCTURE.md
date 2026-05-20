# Data Structure — Room Database (v22)

## Tables

### investment_accounts
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| name | String | |
| description | String | |
| initialValue | Double | |

### investment_items
| Column | Type | Constraints |
|--------|------|-------------|
| ticker | String | PK |
| name | String | |
| type | InvestmentType | enum: Stock, ETF, Bond, MutualFund, Crypto, Other |
| currentPrice | Double | |
| quantity | Double | |
| cost | Double | |
| dayGainLoss | Double | |
| totalGainLoss | Double | |
| value | Double | |
| dayHigh | Double | default 0.0 |
| dayLow | Double | default 0.0 |
| logo | ByteArray? | nullable BLOB |

### investment_transactions
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| date | LocalDate | stored as epoch days |
| time | LocalTime? | nullable, stored as epoch seconds |
| action | TransactionAction | enum: Buy, Sell |
| ticker | String | indexed |
| numberOfShares | Double | |
| pricePerShare | Double | |
| totalAmount | Double | default 0.0 |
| note | String | default "" |

### account_performance
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| accountId | Long | FK → investment_accounts (CASCADE), indexed |
| totalValue | Double | |
| date | LocalDate | stored as epoch days |
| note | String | default "" |

**Unique constraint:** (accountId, date)

### change_history
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| date | LocalDate | unique, stored as epoch days |
| etfValue | Double | |
| stockValue | Double | |
| totalValue | Double | |
| dailyChangeEtf | Double | default 0.0 |
| dailyChangeStock | Double | default 0.0 |
| dailyChangeTotal | Double | default 0.0 |

### watch_lists
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| name | String | |

### watch_list_items
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| watchListId | Long | FK → watch_lists (CASCADE), indexed |
| ticker | String | |
| shares | Double | |
| priceWhenAdded | Double | |
| addedDate | LocalDate | stored as epoch days |
| reminderDateTime | LocalDateTime? | nullable |
| reminderMessage | String? | nullable |

### csv_import_mappings
| Column | Type | Constraints |
|--------|------|-------------|
| importType | String | PK |
| mappingsJson | String | JSON: column index → field name |
| dateFormatJson | String | default "", JSON: field → date format |

### csv_named_mappings
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| name | String | |
| importType | String | |
| mappingsJson | String | JSON: column index → field name |
| dateFormatJson | String | default "" |

## Relationships

```
investment_accounts ──< account_performance  (CASCADE delete)
watch_lists         ──< watch_list_items     (CASCADE delete)
```

Items and transactions are not tied to accounts. Ticker is the shared key between investment_items and investment_transactions (no FK constraint).

## Enums

- **InvestmentType:** Stock, ETF, Bond, MutualFund, Crypto, Other
- **TransactionAction:** Buy, Sell
