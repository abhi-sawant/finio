package com.slowatcoding.finio.ui

import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.ui.navigation.Routes
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
}
