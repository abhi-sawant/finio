package com.slowatcoding.finio.ui

import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.navigation.routeForLaunch
import com.slowatcoding.finio.ui.navigation.routeForPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteForPathTest {
    @Test
    fun mapsEveryWebPathToItsRoute() {
        assertEquals(Routes.Dashboard, routeForPath("/"))
        assertEquals(Routes.Budgets, routeForPath("/budgets"))
        assertEquals(Routes.Budgets, routeForPath("/budgets/"))
        assertEquals(Routes.Accounts, routeForPath("/accounts"))
        assertEquals(Routes.Recurring, routeForPath("/recurring"))
        assertEquals(Routes.SettingsCategory("backup"), routeForPath("/settings/backup"))
        assertEquals(Routes.EditTransaction("t1"), routeForPath("/edit-transaction/t1"))
        assertEquals(Routes.EditLoan("l1"), routeForPath("/edit-loan/l1"))
        assertEquals(Routes.LoanSchedule("l1"), routeForPath("/loan-schedule/l1"))
        assertEquals(Routes.CategoryRules(), routeForPath("/category-rules"))
        assertEquals(Routes.YearInReview, routeForPath("/year-in-review"))
    }

    @Test
    fun unknownPathsFallBackToTheDashboard() {
        assertEquals(Routes.Dashboard, routeForPath("/nope"))
        assertEquals(Routes.Dashboard, routeForPath("/budgets/extra/segments"))
    }

    @Test
    fun emailLessAuthRoutesRedirectLikeTheWeb() {
        assertEquals(Routes.Register, routeForPath("/verify-otp"))
        assertEquals(Routes.ForgotPassword, routeForPath("/reset-password"))
    }

    @Test
    fun addTransactionCarriesTheShortcutDraft() {
        assertEquals(Routes.AddTransaction(), routeForPath("/add-transaction"))
        assertNull(Routes.AddTransaction().draft())
        val income = routeForPath("/add-transaction?type=income") as Routes.AddTransaction
        assertEquals(TransactionType.Income, income.draft()!!.type)
    }

    /**
     * A cold-start deep link arrives as the intent's raw data string. The shortcut / notification
     * form (`path=%2Fmanage-categories`) and a hand-typed plain one must land on the same screen,
     * and an encoded Add Transaction link keeps its query.
     */
    @Test
    fun encodedAndPlainDeepLinksLandOnTheSameRoute() {
        val view = "android.intent.action.VIEW"
        fun land(uri: String) = com.slowatcoding.finio.platform.share.parseLaunch(view, uri)
            ?.let { routeForLaunch(it) }
        assertEquals(Routes.ManageCategories, land("finio://open?path=%2Fmanage-categories"))
        assertEquals(Routes.ManageCategories, land("finio://open?path=/manage-categories"))
        assertEquals(Routes.SettingsCategory("backup"), land("finio://open?path=%2Fsettings%2Fbackup"))
        val expense = land("finio://open?path=%2Fadd-transaction%3Ftype%3Dexpense")
        assertEquals(expense, land("finio://open?path=/add-transaction?type=expense"))
        assertEquals(TransactionType.Expense.wire, (expense as Routes.AddTransaction).type)
        assertNull(land("finio://elsewhere?path=%2Fbudgets"))
    }
}
