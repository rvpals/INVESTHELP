# Invest Help - Test Checklist

## Position Composite Key
- [ ] Add same ticker (e.g. AAPL) to two different accounts - should succeed
- [ ] Edit a position - should update correct ticker+account combo
- [ ] Delete a position - should only delete the specific ticker+account
- [ ] Delete an account - positions under that account should cascade delete

## Account Value from Positions
- [ ] Add positions to an account, verify account value = sum of position values
- [ ] Refresh positions (live prices) - account values on dashboard should update
- [ ] Account with no positions should show $0.00 value

## App Icon & Splash
- [ ] App icon appears correctly on home screen / app drawer
- [ ] Adaptive icon renders properly on API 26+ (round, squircle, etc.)
- [ ] Splash screen shows icon on app cold start
- [ ] Splash screen transitions smoothly to auth screen

## Database Migration
- [ ] Fresh install works correctly
- [ ] Upgrade from v4 to v5 migrates without crash (positions table is recreated)
