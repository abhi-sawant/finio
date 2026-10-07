package com.slowatcoding.finio.ui.navigation

import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.toRoute
import com.slowatcoding.finio.core.model.FinanceState
import com.slowatcoding.finio.core.model.RuleScope
import com.slowatcoding.finio.debugGallery
import com.slowatcoding.finio.ui.common.EditGuard
import com.slowatcoding.finio.ui.screens.accounts.AccountsScreen
import com.slowatcoding.finio.ui.screens.accounts.AddAccountScreen
import com.slowatcoding.finio.ui.screens.analytics.AnalyticsScreen
import com.slowatcoding.finio.ui.screens.auth.ForgotPasswordScreen
import com.slowatcoding.finio.ui.screens.auth.LoginScreen
import com.slowatcoding.finio.ui.screens.auth.RegisterScreen
import com.slowatcoding.finio.ui.screens.auth.ResetPasswordScreen
import com.slowatcoding.finio.ui.screens.auth.VerifyOtpScreen
import com.slowatcoding.finio.ui.screens.budgets.BudgetsScreen
import com.slowatcoding.finio.ui.screens.categories.ManageCategoriesScreen
import com.slowatcoding.finio.ui.screens.dashboard.DashboardScreen
import com.slowatcoding.finio.ui.screens.debts.DebtsScreen
import com.slowatcoding.finio.ui.screens.goals.GoalsScreen
import com.slowatcoding.finio.ui.screens.importcsv.ImportCsvScreen
import com.slowatcoding.finio.ui.screens.labels.ManageLabelsScreen
import com.slowatcoding.finio.ui.screens.legal.PrivacyScreen
import com.slowatcoding.finio.ui.screens.legal.TermsScreen
import com.slowatcoding.finio.ui.screens.loans.AddLoanScreen
import com.slowatcoding.finio.ui.screens.loans.LoanScheduleScreen
import com.slowatcoding.finio.ui.screens.loans.LoansScreen
import com.slowatcoding.finio.ui.screens.merchants.MerchantsScreen
import com.slowatcoding.finio.ui.screens.recurring.RecurringScreen
import com.slowatcoding.finio.ui.screens.rules.CategoryRulesScreen
import com.slowatcoding.finio.ui.screens.settings.SettingsCategoryScreen
import com.slowatcoding.finio.ui.screens.settings.SettingsScreen
import com.slowatcoding.finio.ui.screens.tools.ToolsScreen
import com.slowatcoding.finio.ui.screens.transactions.AddTransactionScreen
import com.slowatcoding.finio.ui.screens.transactions.TransactionsScreen
import com.slowatcoding.finio.ui.screens.yearinreview.YearInReviewScreen

/**
 * The whole navigation graph — the `<Routes>` block of App.tsx. Built ONCE per NavController (the
 * shell remembers it above the gates), so the NavHost can leave composition while the app is
 * locked and come back to the same graph and the same back stack.
 *
 * [current] reads the finance store's current snapshot; the edit guards check it once, at entry.
 */
fun buildFinioNavGraph(
    navController: NavHostController,
    nav: FinioNavigator,
    current: () -> FinanceState,
): NavGraph = navController.createGraph(startDestination = Routes.Dashboard) {
    // Layout routes (tab bar).
    composable<Routes.Dashboard> { DashboardScreen(nav) }
    composable<Routes.Accounts> { AccountsScreen(nav) }
    composable<Routes.Transactions> { TransactionsScreen(nav) }
    composable<Routes.Analytics> { AnalyticsScreen(nav) }
    composable<Routes.Tools> { ToolsScreen(nav) }
    composable<Routes.Settings> { SettingsScreen(nav) }
    composable<Routes.SettingsCategory> { SettingsCategoryScreen(nav, it.toRoute<Routes.SettingsCategory>().category) }

    // Full-screen routes.
    composable<Routes.AddTransaction> {
        AddTransactionScreen(nav, transactionId = null, draft = it.toRoute<Routes.AddTransaction>().draft())
    }
    composable<Routes.EditTransaction> { entry ->
        val id = entry.toRoute<Routes.EditTransaction>().id
        EditGuard(exists = { current().transactions.any { it.id == id } }, fallback = Routes.Transactions, nav = nav) {
            AddTransactionScreen(nav, transactionId = id, draft = null)
        }
    }
    composable<Routes.AddAccount> { AddAccountScreen(nav, accountId = null) }
    composable<Routes.EditAccount> { entry ->
        val id = entry.toRoute<Routes.EditAccount>().id
        EditGuard(exists = { current().accounts.any { it.id == id } }, fallback = Routes.Accounts, nav = nav) {
            AddAccountScreen(nav, accountId = id)
        }
    }
    composable<Routes.ManageCategories> { ManageCategoriesScreen(nav) }
    composable<Routes.ManageLabels> { ManageLabelsScreen(nav) }
    composable<Routes.Budgets> { BudgetsScreen(nav) }
    composable<Routes.Recurring> { RecurringScreen(nav) }
    composable<Routes.Goals> { GoalsScreen(nav) }
    composable<Routes.Debts> { DebtsScreen(nav) }
    composable<Routes.Loans> { LoansScreen(nav) }
    composable<Routes.AddLoan> { AddLoanScreen(nav, loanId = null) }
    composable<Routes.EditLoan> { entry ->
        val id = entry.toRoute<Routes.EditLoan>().id
        EditGuard(exists = { current().loans.any { it.id == id } }, fallback = Routes.Loans, nav = nav) {
            AddLoanScreen(nav, loanId = id)
        }
    }
    composable<Routes.LoanSchedule> { entry ->
        val id = entry.toRoute<Routes.LoanSchedule>().id
        EditGuard(exists = { current().loans.any { it.id == id } }, fallback = Routes.Loans, nav = nav) {
            LoanScheduleScreen(nav, loanId = id)
        }
    }
    composable<Routes.ImportCsv> { ImportCsvScreen(nav) }
    composable<Routes.CategoryRules> {
        val route = it.toRoute<Routes.CategoryRules>()
        CategoryRulesScreen(
            nav,
            prefillPattern = route.pattern,
            prefillScope = RuleScope.entries.find { s -> s.wire == route.scope },
        )
    }
    composable<Routes.Merchants> { MerchantsScreen(nav) }
    composable<Routes.YearInReview> { YearInReviewScreen(nav) }

    // Cloud account + legal.
    composable<Routes.Login> { LoginScreen(nav) }
    composable<Routes.Register> { RegisterScreen(nav) }
    composable<Routes.VerifyOtp> { VerifyOtpScreen(nav, it.toRoute<Routes.VerifyOtp>().email) }
    composable<Routes.ForgotPassword> { ForgotPasswordScreen(nav) }
    composable<Routes.ResetPassword> { ResetPasswordScreen(nav, it.toRoute<Routes.ResetPassword>().email) }
    composable<Routes.Privacy> { PrivacyScreen(nav) }
    composable<Routes.Terms> { TermsScreen(nav) }

    composable<Routes.DebugGallery> { debugGallery?.invoke() }
}
