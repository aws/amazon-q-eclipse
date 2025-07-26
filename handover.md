# Amazon Q Eclipse - Pricing Tiers Implementation Handover

## Current Status

**Step 4 (UI Implementation)** - Functionally complete, needs final polish and testing.

### Completed Components
- ✅ **SubscriptionDetails.java** - Flattened model with backward compatibility methods
- ✅ **SubscriptionDetailsDialog.java** - Complete SWT dialog with platform-specific fonts, progress bars, upgrade button
- ✅ **AmazonQLspClientImpl.java** - Notification handler with ObjectMapper deserialization, fallback test data
- ✅ **SubscriptionShowIntegrationTest.java** - Comprehensive integration testing covering complete flow
- ✅ **Checkstyle compliance** - Fixed 7 trailing whitespace violations

### Key Implementation Details
- Dialog displays subscription tier, usage statistics, billing cycle information
- Progress bar shows usage against limits with visual indicators
- Upgrade button opens browser to AWS pricing page (hidden for Pro tier users)
- Handles both real LSP data and fallback test data for error cases
- Platform-specific font sizing for Windows/Mac compatibility

## Authentication Configuration (Critical)

### Lokesh's Authentication Notes

**For test accounts only** - Change scopes in two places:

1. **amazon-q-eclipse repository** (only for test accounts):
   - File: [QConstants.java](https://github.com/aws/amazon-q-eclipse/blob/5bb44869c951500c49805791c067e3a914e88/plugin/src/software/aws/toolkits/eclipse/amazonq/util/QConstants.java)
   - Change: `codewhisperer` → `codewhisperer_internal`
   - **Important**: Only needed for test accounts, not regular accounts

2. **language-servers repository** (required for all testing):
   - File: [constants.ts](https://github.com/aws/language-servers/pull/1923/files#diff-4db506c91cd59cd0cb524e4e480bf2281b0cc029c67ba1fa4b6ec5b31a4455b0)
   - Change: `codewhisperer` → `codewhisperer_internal`

### Test Accounts
- **Login URL**: https://d-9067c744e4.awsapps.com/start/#/
- **Users**: 
  - `kiro-limited`
  - `kiro-include-pro` 
  - `kiro-included-pro-plus`
- **Password**: https://paste.amazon.com/show/kennvene/1753177009

### Authentication Issue Warning
**Problem**: Using `codewhisperer_internal` scopes with standard AWS SSO login causes browser connection errors after clicking "Allow".

**Solution**: 
- **Regular development**: Keep standard `codewhisperer` scopes, use standard AWS SSO
- **Test account testing**: Change to `codewhisperer_internal` scopes, use test account login URL
- **Always revert** QConstants.java changes after testing

## Reference Links

### Protocol & Implementation References
- [language-server-runtimes PR #620](https://github.com/aws/language-server-runtimes/pull/620) - Protocol extensions for subscription details
- [Visual Studio implementation (PR #2595)](https://github.com/aws/aws-toolkit-visual-studio-staging/pull/2595) - Reference implementation
- [JetBrains implementation (PR #5918)](https://github.com/aws/aws-toolkit-jetbrains/pull/5918) - Reference implementation
- [language-servers PR #1923](https://github.com/aws/language-servers/pull/1923/files#diff-4db506c91cd59cd0cb524e4e480bf2281b0cc029c67ba1fa4b6ec5b31a4455b0) - Auth endpoint configuration

### Documentation & Design
- [IDE Plugins: Pricing Tiers Support](https://quip-amazon.com/6cR7AR6dJ4lS/IDE-Plugins-Pricing-Tiers-Support) - Requirements document
- [Eclipse Q Toolkit dev environment setup](https://quip-amazon.com/6AMUAkkWdAj2/Eclipse-Q-Toolkit-dev-environment-setup) - Development setup
- Figma designs available in `../../Projects/kiro-pricing/` directory

## Development Notes

### Critical Build Workflow
**Always run `mvn clean` before test compilation** - Tycho has aggressive caching that causes intermittent compilation failures.

```bash
cd plugin
mvn clean compile test-compile -Dcheckstyle.skip=true -q
mvn test -Dtest=YourTestClass -Dcheckstyle.skip=true -q
```

### Checkstyle Compliance
Run before committing:
```bash
mvn checkstyle:check -Dcheckstyle.includeTestSourceDirectory=true -q
```

Common violations: trailing spaces, unused imports, line length

### Test Patterns
- Follow `AccountDetailsActionTest.java` for simple action testing
- Use `ActivatorStaticMockExtension` for all tests
- Mock `Platform` and `FrameworkUtil` static methods for Eclipse API calls
- Use `atLeastOnce()` for verification when methods may be called multiple times

## Next Steps

1. **Final testing** with real subscription data from language server
2. **Telemetry integration** for dialog interactions and upgrade button clicks
3. **Error handling polish** in SubscriptionDetailsDialog
4. **Cross-platform testing** (Windows/Mac font sizing verification)
5. **Revert any test-only authentication changes** before final commit

## Implementation Commits

- `8c0a9686` - Step 1: Initialization protocol capabilities
- `415a6e5a` - Step 2: Account Details menu item
- `ee1e1bc5` - Step 3: Protocol support for subscription show command
- Step 4: In progress (UI implementation and upgrade flow)
