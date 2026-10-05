package com.slowatcoding.finio.ui.navigation

import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.share.SharedTransactionDraft
import com.slowatcoding.finio.core.share.parseSharePayload
import com.slowatcoding.finio.platform.share.DeepLinks
import kotlinx.serialization.Serializable

/**
 * Every destination, as a type-safe Navigation-Compose route — one per web route in App.tsx.
 * Always refer to them qualified (`Routes.Budgets`): several names (Settings, Accounts…) collide
 * with core model types.
 *
 * Arguments are plain strings/nullables so they survive the saved-state bundle; enums travel as
 * their web wire name (e.g. `"expense"`). The web's `location.state` payloads (VerifyOtp's email,
 * CategoryRules' prefill) are route arguments here.
 */
@Serializable
sealed interface FinioRoute

object Routes {
    // ---- Layout routes: tab bar (+ coin FAB where the web shows it) --------------------------

    /** `/` */
    @Serializable data object Dashboard : FinioRoute

    /** `/accounts` */
    @Serializable data object Accounts : FinioRoute

    /** `/transactions` */
    @Serializable data object Transactions : FinioRoute

    /** `/analytics` */
    @Serializable data object Analytics : FinioRoute

    /** `/tools` */
    @Serializable data object Tools : FinioRoute

    /** `/settings` — reached from the Dashboard header on mobile, not a tab. */
    @Serializable data object Settings : FinioRoute

    /** `/settings/:category` — account | backup | profile | security | notifications | organise. */
    @Serializable data class SettingsCategory(val category: String) : FinioRoute

    // ---- Full-screen routes -----------------------------------------------------------------

    /**
     * `/add-transaction` and `/share-target`. All-null is the plain FAB entry; otherwise the
     * fields are a share-sheet / shortcut draft (`type` is a wire name: expense|income|transfer)
     * that seeds a blank form — see [draft].
     */
    @Serializable
    data class AddTransaction(
        val type: String? = null,
        val amount: String? = null,
        val note: String? = null,
    ) : FinioRoute {
        /** The OS-provided draft, or null for a plain add. */
        fun draft(): SharedTransactionDraft? {
            if (type == null && amount == null && note == null) return null
            val parsedType = TransactionType.entries.find { it.wire == type } ?: TransactionType.Expense
            return SharedTransactionDraft(parsedType, amount ?: "", note ?: "")
        }

        companion object {
            fun from(draft: SharedTransactionDraft) = AddTransaction(draft.type.wire, draft.amount, draft.note)
        }
    }

    /** `/edit-transaction/:id` — guarded: a stale id goes back to Transactions. */
    @Serializable data class EditTransaction(val id: String) : FinioRoute

    /** `/add-account` */
    @Serializable data object AddAccount : FinioRoute

    /** `/edit-account/:id` — guarded: a stale id goes back to Accounts. */
    @Serializable data class EditAccount(val id: String) : FinioRoute

    @Serializable data object ManageCategories : FinioRoute
    @Serializable data object ManageLabels : FinioRoute
    @Serializable data object Budgets : FinioRoute
    @Serializable data object Recurring : FinioRoute
    @Serializable data object Goals : FinioRoute
    @Serializable data object Debts : FinioRoute
    @Serializable data object Loans : FinioRoute
    @Serializable data object AddLoan : FinioRoute

    /** `/edit-loan/:id` — guarded: a stale id goes back to Loans. */
    @Serializable data class EditLoan(val id: String) : FinioRoute

    /** `/loan-schedule/:id` — guarded: a stale id goes back to Loans. */
    @Serializable data class LoanSchedule(val id: String) : FinioRoute

    @Serializable data object ImportCsv : FinioRoute

    /**
     * `/category-rules`. [pattern]/[scope] are Merchants' "Create a rule" prefill (web
     * `location.state`); [scope] is a RuleScope wire name: any|expense|income.
     */
    @Serializable
    data class CategoryRules(val pattern: String? = null, val scope: String? = null) : FinioRoute

    @Serializable data object Merchants : FinioRoute
    @Serializable data object YearInReview : FinioRoute

    // ---- Cloud account + legal --------------------------------------------------------------

    @Serializable data object Login : FinioRoute
    @Serializable data object Register : FinioRoute

    /** `/verify-otp` with the web's `state.email`. */
    @Serializable data class VerifyOtp(val email: String) : FinioRoute

    @Serializable data object ForgotPassword : FinioRoute

    /** `/reset-password` with the web's `state.email` (the screen bounces to ForgotPassword without one). */
    @Serializable data class ResetPassword(val email: String? = null) : FinioRoute

    @Serializable data object Privacy : FinioRoute
    @Serializable data object Terms : FinioRoute

    /** Debug builds only: the Mudra component gallery (long-press the Tools title). */
    @Serializable data object DebugGallery : FinioRoute
}

/**
 * The route for an in-app web path (`/budgets`, `/edit-loan/abc`, `/settings/backup`,
 * `/add-transaction?type=income`…) — notification URLs, launcher shortcuts, insight actions.
 * Anything unknown maps to the Dashboard, like the web's `*` catch-all.
 */
fun routeForPath(path: String): FinioRoute {
    val q = path.indexOf('?')
    val pathname = (if (q >= 0) path.substring(0, q) else path).trimEnd('/').ifEmpty { "/" }
    val params = if (q >= 0) DeepLinks.queryParams(path.substring(q + 1)) else emptyMap()
    val parts = pathname.removePrefix("/").split('/')
    val head = parts.getOrNull(0) ?: ""
    val arg = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
    return when {
        pathname == "/" -> Routes.Dashboard
        parts.size == 1 -> when (head) {
            "accounts" -> Routes.Accounts
            "transactions" -> Routes.Transactions
            "analytics" -> Routes.Analytics
            "tools" -> Routes.Tools
            "settings" -> Routes.Settings
            "add-transaction", "share-target" ->
                if (params.isEmpty()) Routes.AddTransaction()
                else Routes.AddTransaction.from(parseSharePayload(params["title"], params["text"], params["url"], params["type"]))
            "add-account" -> Routes.AddAccount
            "manage-categories" -> Routes.ManageCategories
            "manage-labels" -> Routes.ManageLabels
            "budgets" -> Routes.Budgets
            "recurring" -> Routes.Recurring
            "goals" -> Routes.Goals
            "debts" -> Routes.Debts
            "loans" -> Routes.Loans
            "add-loan" -> Routes.AddLoan
            "import-csv" -> Routes.ImportCsv
            "category-rules" -> Routes.CategoryRules()
            "merchants" -> Routes.Merchants
            "year-in-review" -> Routes.YearInReview
            "login" -> Routes.Login
            // No email to verify — the web redirects to /register.
            "register", "verify-otp" -> Routes.Register
            // No email to reset — the web redirects to /forgot-password.
            "forgot-password", "reset-password" -> Routes.ForgotPassword
            "privacy" -> Routes.Privacy
            "terms" -> Routes.Terms
            else -> Routes.Dashboard
        }
        parts.size == 2 && arg != null -> when (head) {
            "settings" -> Routes.SettingsCategory(arg)
            "edit-transaction" -> Routes.EditTransaction(arg)
            "edit-account" -> Routes.EditAccount(arg)
            "edit-loan" -> Routes.EditLoan(arg)
            "loan-schedule" -> Routes.LoanSchedule(arg)
            else -> Routes.Dashboard
        }
        else -> Routes.Dashboard
    }
}
