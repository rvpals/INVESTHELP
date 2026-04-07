# Invest Help - Requirements

## Overview
Android app to track personal investments.

## Storage & Security
- SQLite for local storage
- Database encryption with password (SQLCipher)
- Biometric authentication support

## Data Objects

### Investment Account
- Name
- Description
- Initial value
- Current value (computed from sum of position values)

### Investment Item
- Name
- Ticker symbol (optional)
- Type (ETF, Stock, Bond, MutualFund, Crypto, Other)
- Current price

### Position
- Ticker (part of composite PK)
- Account (part of composite PK) - same ticker can exist on multiple accounts
- Quantity
- Cost basis
- Current value (from live price refresh)
- Day gain/loss
- Total gain/loss

### Investment Transaction
- Date
- Time of transaction
- Action (Buy / Sell)
- Associated Account
- Number of shares
- Price per share
- Associated Investment Item

## Features

### Position Tracking
- Add/edit/delete positions per account
- Same ticker allowed on different accounts (composite key: ticker + account)
- Refresh all positions with live market prices
- Account values auto-update from position values

### Investment Item Statistics View
- Opens when clicking on an Investment Item
- Shows statistics:
  - Average buy/sell price
  - Max buy/sell price
  - Min buy/sell price
- Filterable by date range

### App Branding
- Custom app icon (invest_help_icon.png)
- Splash screen with app icon on startup (AndroidX SplashScreen API)
