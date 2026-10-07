package com.slowatcoding.finio.platform

import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.platform.share.DeepLinks
import com.slowatcoding.finio.platform.share.LaunchTarget
import com.slowatcoding.finio.platform.share.parseLaunch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchTargetTest {
    private val send = "android.intent.action.SEND"
    private val view = "android.intent.action.VIEW"
    private val main = "android.intent.action.MAIN"

    @Test fun plainLauncherStartHasNoTarget() {
        assertNull(parseLaunch(main, null))
    }

    @Test fun sharedBankSmsBecomesAnExpenseDraft() {
        val t = parseLaunch(send, null, "text/plain", null, "Rs. 1,250.50 debited from a/c XX1234 at SWIGGY") as LaunchTarget.AddTransaction
        assertEquals(TransactionType.Expense, t.draft.type)
        assertEquals("1250.5", t.draft.amount)
        assertEquals(LaunchTarget.Source.Share, t.source)
    }

    @Test fun sharedCreditIsIncomeAndSubjectIsTheTitle() {
        val t = parseLaunch(send, null, "text/plain", "Salary", "INR 50000 credited") as LaunchTarget.AddTransaction
        assertEquals(TransactionType.Income, t.draft.type)
        assertEquals("50000", t.draft.amount)
        assertEquals("Salary INR 50000 credited", t.draft.note)
    }

    @Test fun sharedLinkBecomesItsHostname() {
        val t = parseLaunch(send, null, "text/plain", null, "https://www.swiggy.com/order/123") as LaunchTarget.AddTransaction
        assertEquals("swiggy.com", t.draft.note)
    }

    @Test fun emptyOrNonTextShareIsIgnored() {
        assertNull(parseLaunch(send, null, "text/plain", null, "  "))
        assertNull(parseLaunch(send, null, "image/png", null, "Rs 10"))
    }

    @Test fun shortcutDeepLinksMirrorThePwaManifest() {
        val expense = parseLaunch(view, "finio://open?path=%2Fadd-transaction%3Ftype%3Dexpense") as LaunchTarget.AddTransaction
        assertEquals(TransactionType.Expense, expense.draft.type)
        assertEquals("", expense.draft.amount)
        assertEquals(LaunchTarget.Source.DeepLink, expense.source)

        val income = parseLaunch(view, "finio://open?path=%2Fadd-transaction%3Ftype%3Dincome") as LaunchTarget.AddTransaction
        assertEquals(TransactionType.Income, income.draft.type)

        assertEquals(LaunchTarget.Route("/transactions"), parseLaunch(view, "finio://open?path=%2Ftransactions"))
        assertEquals(LaunchTarget.Route("/budgets"), parseLaunch(view, "finio://open?path=%2Fbudgets"))
    }

    @Test fun shareTargetPathCarriesTitleTextUrl() {
        val link = DeepLinks.forPath("/share-target?text=" + java.net.URLEncoder.encode("Paid ₹99 to Netflix", "UTF-8"))
        val t = parseLaunch(view, link) as LaunchTarget.AddTransaction
        assertEquals("99", t.draft.amount)
        assertEquals("Paid ₹99 to Netflix", t.draft.note)
    }

    @Test fun forPathRoundTripsNotificationUrls() {
        for (path in listOf("/budgets", "/recurring", "/accounts?highlight=abc", "/")) {
            assertEquals(LaunchTarget.Route(path), DeepLinks.parse(DeepLinks.forPath(path)))
        }
    }

    @Test fun rejectsForeignOrUnsafeLinks() {
        assertNull(parseLaunch(view, "https://open?path=%2Fbudgets"))
        assertNull(parseLaunch(view, "finio://evil?path=%2Fbudgets"))
        assertNull(parseLaunch(view, "finio://open"))
        assertNull(parseLaunch(view, "finio://open?path=https%3A%2F%2Fevil.com"))
        assertNull(parseLaunch(view, "finio://open?path=%2F%2Fevil.com"))
        assertNull(parseLaunch(view, "finio://open?path=%2Fa%5Cb"))
        assertNull(parseLaunch(view, null))
    }

    @Test fun queryParamsFirstWinsAndDecodes() {
        val p = DeepLinks.queryParams("a=1&a=2&b=x%20y&c=p+q&flag")
        assertEquals("1", p["a"])
        assertEquals("x y", p["b"])
        assertEquals("p q", p["c"])
        assertTrue(p.containsKey("flag"))
    }
}
